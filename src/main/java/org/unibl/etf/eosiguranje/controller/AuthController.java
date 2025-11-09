package org.unibl.etf.eosiguranje.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import org.unibl.etf.eosiguranje.dto.RegisterRequest;
import org.unibl.etf.eosiguranje.dto.LoginRequest;
import org.unibl.etf.eosiguranje.dto.TwoFaRequest;
import org.unibl.etf.eosiguranje.model.User;
import org.unibl.etf.eosiguranje.model.User2FA;
import org.unibl.etf.eosiguranje.repository.User2FARepository;
import org.unibl.etf.eosiguranje.service.MailService;
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userService.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username_taken"));
        }
        if (userService.findByEmail(req.getEmail()).isPresent()) {
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
        return ResponseEntity.ok(Map.of("message", "user_registered"));
    }

    // Step 1: verify credentials & send 2FA
    @PostMapping("/login")
    public ResponseEntity<?> loginStep1(@RequestBody LoginRequest loginRequest) {
        var userOpt = userService.findByUsername(loginRequest.getUsername());
        if (userOpt.isEmpty())
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));

        var user = userOpt.get();
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }

        String code = String.valueOf((int) (Math.random() * 900_000) + 100_000);

        User2FA user2FA = User2FA.builder()
                .user(user)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
        user2FA = user2FARepository.save(user2FA);

        // TODO: uncomment sendEmail2, for 2fa, limit reached
       // mailService.sendEmail(user.getEmail(), "Your 2FA code: " + code);
        System.out.println("2FA code sent to: " + code);

        // return the id so client can call verify with it
        return ResponseEntity.ok(Map.of(
                "message", "2fa_sent",
                "user2FAId", user2FA.getId()
        ));
    }

    @PostMapping("/login/verify")
    @Transactional
    public ResponseEntity<?> loginStep2(@RequestBody TwoFaRequest request) {
        LocalDateTime now = LocalDateTime.now();
        var twoFaOpt = user2FARepository
                .findByIdAndUsedFalseAndExpiresAtAfter(request.getUser2FAId(), now);

        if (twoFaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_or_expired_2fa"));
        }

        var twoFa = twoFaOpt.get();
        String submitted = request.getCode() == null ? "" : request.getCode().trim();

        if (!twoFa.getCode().equals(submitted)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_or_expired_2fa"));
        }

        twoFa.setUsed(true);
        user2FARepository.save(twoFa);

        // ISSUE JWT using username + role
        String token = jwtUtil.generateToken(twoFa.getUser().getUsername(), twoFa.getUser().getRole());

        return ResponseEntity.ok(Map.of(
                "message", "login_success",
                "token", token
        ));
    }



}




