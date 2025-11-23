package org.unibl.etf.eosiguranje.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.unibl.etf.eosiguranje.model.SecurityEvent;
import org.unibl.etf.eosiguranje.repository.SecurityEventRepository;
import org.unibl.etf.eosiguranje.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/security-events")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class SecurityEventsController {

    private final SecurityEventRepository securityEventRepository;
    private final UserService userService;

    /**
     * Get all security events (admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SecurityEvent>> getAllEvents(
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        List<SecurityEvent> events = securityEventRepository.findAll();

        // Limit results
        if (events.size() > limit) {
            events = events.subList(0, limit);
        }

        return ResponseEntity.ok(events);
    }

    /**
     * Get events by action type
     */
    @GetMapping("/by-action")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SecurityEvent>> getEventsByAction(
            @RequestParam String action) {
        List<SecurityEvent> events = securityEventRepository.findByActionOrderByCreatedAtDesc(action);
        return ResponseEntity.ok(events);
    }

    /**
     * Get high-risk events
     */
    @GetMapping("/high-risk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SecurityEvent>> getHighRiskEvents(
            @RequestParam(required = false, defaultValue = "50") Integer threshold) {
        List<SecurityEvent> events = securityEventRepository.findHighRiskEvents(threshold);
        return ResponseEntity.ok(events);
    }

    /**
     * Get recent events within specified hours
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SecurityEvent>> getRecentEvents(
            @RequestParam(required = false, defaultValue = "24") Integer hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<SecurityEvent> events = securityEventRepository.findRecentEvents(since);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events for current user
     */
    @GetMapping("/my-events")
    public ResponseEntity<List<SecurityEvent>> getMyEvents(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = userService.getUsernameFromToken(authHeader.substring(7));
            var user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<SecurityEvent> events = securityEventRepository.findByUserOrderByCreatedAtDesc(user);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get security statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStatistics(
            @RequestParam(required = false, defaultValue = "24") Integer hours) {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            List<SecurityEvent> recentEvents = securityEventRepository.findRecentEvents(since);

            long totalEvents = recentEvents.size();
            long loginFailures = recentEvents.stream()
                    .filter(e -> "LOGIN_FAIL".equals(e.getAction()))
                    .count();
            long twoFaFailures = recentEvents.stream()
                    .filter(e -> "2FA_FAIL".equals(e.getAction()))
                    .count();
            long successfulLogins = recentEvents.stream()
                    .filter(e -> "LOGIN_SUCCESS".equals(e.getAction()))
                    .count();
            long purchases = recentEvents.stream()
                    .filter(e -> "PURCHASE_SUCCESS".equals(e.getAction()))
                    .count();
            long highRiskEvents = recentEvents.stream()
                    .filter(e -> e.getRiskScore() != null && e.getRiskScore() >= 70)
                    .count();
            long suspiciousActivity = recentEvents.stream()
                    .filter(e -> "SUSPICIOUS_ACTIVITY".equals(e.getAction()))
                    .count();

            return ResponseEntity.ok(Map.of(
                    "timeframe_hours", hours,
                    "total_events", totalEvents,
                    "login_failures", loginFailures,
                    "2fa_failures", twoFaFailures,
                    "successful_logins", successfulLogins,
                    "purchases", purchases,
                    "high_risk_events", highRiskEvents,
                    "suspicious_activity", suspiciousActivity
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get event details by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SecurityEvent> getEventById(@PathVariable Long id) {
        return securityEventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}