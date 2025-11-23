package org.unibl.etf.eosiguranje.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.unibl.etf.eosiguranje.repository.SecurityEventRepository;
import org.unibl.etf.eosiguranje.service.SecurityEventService;

import org.unibl.etf.eosiguranje.dto.*;
import org.unibl.etf.eosiguranje.model.User;
import org.unibl.etf.eosiguranje.model.User2FA;
import org.unibl.etf.eosiguranje.repository.User2FARepository;
import org.unibl.etf.eosiguranje.service.MailService;
import org.unibl.etf.eosiguranje.service.SecurityEventService;
import org.unibl.etf.eosiguranje.service.UserService;
import org.unibl.etf.eosiguranje.security.JwtUtil;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    private final UserService userService;
    private final User2FARepository user2FARepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final JwtUtil jwtUtil;
    private final SecurityEventService securityEventService;
    private final SecurityEventRepository securityEventRepository;
    private AuthenticationManager authenticationManager;

    // --------------------------------------------------------------------
    // REGISTER
    // --------------------------------------------------------------------
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req, HttpServletRequest request) {
        String ipAddress = getClientIP(request);

        if (userService.findByUsername(req.getUsername()).isPresent()) {
            securityEventService.logEvent("REGISTRATION_FAIL",
                    String.format("Username already taken: %s, IP: %s", req.getUsername(), ipAddress), 20);
            return ResponseEntity.badRequest().body(Map.of("error", "username_taken"));
        }
        if (userService.findByEmail(req.getEmail()).isPresent()) {
            securityEventService.logEvent("REGISTRATION_FAIL",
                    String.format("Email already taken: %s, IP: %s", req.getEmail(), ipAddress), 20);
            return ResponseEntity.badRequest().body(Map.of("error", "email_taken"));
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role("CLIENT")
                .enabled(true)
                .build();

        userService.save(user);

        // Log successful registration
        securityEventService.logRegistration(user, ipAddress);

        return ResponseEntity.ok(Map.of("message", "user_registered"));
    }

    // --------------------------------------------------------------------
    // LOGIN STEP 1 (password check + send 2FA)
    // --------------------------------------------------------------------
    @PostMapping("/login")
    public ResponseEntity<?> loginStep1(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String ipAddress = getClientIP(request);

        var userOpt = userService.findByUsername(loginRequest.getUsername());
        if (userOpt.isEmpty()) {
            // Log failed login - user not found
            securityEventService.logFailedLogin(loginRequest.getUsername(), "USER_NOT_FOUND", ipAddress);
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }

        var user = userOpt.get();
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            // Log failed login - wrong password
            securityEventService.logFailedLogin(loginRequest.getUsername(), "INVALID_PASSWORD", ipAddress);

            // Check for suspicious activity (multiple failed attempts)
            checkForSuspiciousLoginActivity(loginRequest.getUsername());

            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }

        // Generate 2FA
        String code = String.valueOf((int) (Math.random() * 900_000) + 100_000);

        User2FA user2FA = User2FA.builder()
                .user(user)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
        user2FA = user2FARepository.save(user2FA);

        // Log 2FA code sent
        securityEventService.logEvent(user, "2FA_SENT",
                String.format("2FA code sent to user from IP: %s", ipAddress), 0);

        // mailService.sendEmail(user.getEmail(), "Your 2FA code: " + code);
        System.out.println("2FA code: " + code);

        return ResponseEntity.ok(Map.of(
                "message", "2fa_sent",
                "user2FAId", user2FA.getId()
        ));
    }

    // --------------------------------------------------------------------
    // LOGIN STEP 2 (verify 2FA + issue ACCESS + REFRESH tokens)
    // --------------------------------------------------------------------
    @PostMapping("/login/verify")
    @Transactional
    public ResponseEntity<?> verify2FA(@RequestBody TwoFaRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIP(httpRequest);

        try {
            LocalDateTime now = LocalDateTime.now();

            // 1) Check 2FA ID exists, not used, and not expired
            var twoFaOpt = user2FARepository
                    .findByIdAndUsedFalseAndExpiresAtAfter(request.getUser2FAId(), now);

            if (twoFaOpt.isEmpty()) {
                // Check if it exists but expired
                var expiredOpt = user2FARepository.findById(request.getUser2FAId());
                if (expiredOpt.isPresent()) {
                    User user = expiredOpt.get().getUser();
                    securityEventService.logExpired2FA(user != null ? user.getUsername() : "UNKNOWN");
                } else {
                    securityEventService.logEvent("2FA_FAIL",
                            String.format("Invalid 2FA ID: %d, IP: %s", request.getUser2FAId(), ipAddress), 60);
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "invalid_or_expired_2fa"));
            }

            var twoFa = twoFaOpt.get();
            String submitted = request.getCode() == null ? "" : request.getCode().trim();

            // 2) Verify code
            if (!twoFa.getCode().equals(submitted)) {
                User user = twoFa.getUser();
                securityEventService.logFailed2FA(
                        user != null ? user.getUsername() : "UNKNOWN", ipAddress);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "invalid_or_expired_2fa"));
            }

            // 3) Mark as used
            twoFa.setUsed(true);
            user2FARepository.save(twoFa);

            // 4) Get user from validated 2FA session
            User user = twoFa.getUser();
            if (user == null) {
                securityEventService.logEvent("2FA_FAIL",
                        "User not found in 2FA session", 70);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "user_not_found"));
            }

            // 5) Generate tokens
            String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            // Log successful login
            securityEventService.logSuccessfulLogin(user, ipAddress);

            return ResponseEntity.ok(new AuthResponse(
                    accessToken,
                    refreshToken,
                    user.getUsername(),
                    user.getRole()
            ));

        } catch (Exception e) {
            securityEventService.logEvent("2FA_VERIFICATION_ERROR",
                    String.format("Exception during 2FA verification: %s, IP: %s",
                            e.getMessage(), ipAddress), 70);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "2fa_verification_failed"));
        }
    }

    // --------------------------------------------------------------------
    // REFRESH TOKEN
    // --------------------------------------------------------------------
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        try {
            String refreshToken = request.getRefreshToken();

            if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
                securityEventService.logEvent("TOKEN_REFRESH_FAIL",
                        "Invalid refresh token provided", 50);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid refresh token");
            }

            String username = jwtUtil.extractUsername(refreshToken);
            var userOpt = userService.findByUsername(username);

            if (userOpt.isEmpty()) {
                securityEventService.logEvent("TOKEN_REFRESH_FAIL",
                        String.format("User not found for username: %s", username), 60);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("User not found");
            }

            User user = userOpt.get();

            String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            // Log token refresh
            securityEventService.logTokenRefresh(user);

            return ResponseEntity.ok(new AuthResponse(
                    newAccessToken,
                    newRefreshToken,
                    user.getUsername(),
                    user.getRole()
            ));
        } catch (Exception e) {
            securityEventService.logEvent("TOKEN_REFRESH_ERROR",
                    String.format("Exception during token refresh: %s", e.getMessage()), 60);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token refresh failed");
        }
    }

    // --------------------------------------------------------------------
    // HELPER METHODS
    // --------------------------------------------------------------------

    /**
     * Extract client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    /**
     * Check for suspicious login activity (multiple failed attempts)
     */
    private void checkForSuspiciousLoginActivity(String username) {
        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(15);
            Long failedAttempts = securityEventRepository.countFailedLoginAttempts(username, since);

            if (failedAttempts != null && failedAttempts >= 5) {
                securityEventService.logSuspiciousActivity(username, "MULTIPLE_FAILED_LOGINS",
                        String.format("%d failed login attempts in last 15 minutes", failedAttempts));
            }
        } catch (Exception e) {
            // Don't fail the request if suspicious activity check fails
            System.err.println("Error checking suspicious activity: " + e.getMessage());
        }
    }
}