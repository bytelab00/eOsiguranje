package org.unibl.etf.eosiguranje.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.unibl.etf.eosiguranje.model.SecurityEvent;
import org.unibl.etf.eosiguranje.model.User;
import org.unibl.etf.eosiguranje.repository.SecurityEventRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;

    @Transactional
    public void logEvent(User user, String action, String details, Integer riskScore) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                    .user(user)
                    .action(action)
                    .details(details)
                    .riskScore(riskScore)
                    .createdAt(LocalDateTime.now())
                    .build();

            securityEventRepository.save(event);
            log.info("Security event logged: {} - User: {} - Details: {}",
                    action, user != null ? user.getUsername() : "ANONYMOUS", details);
        } catch (Exception e) {
            log.error("Failed to log security event: {}", action, e);
        }
    }

    @Transactional
    public void logEvent(String action, String details, Integer riskScore) {
        logEvent(null, action, details, riskScore);
    }

    public void logSuccessfulLogin(User user, String ipAddress) {
        logEvent(user, "LOGIN_SUCCESS",
                String.format("User logged in from IP: %s", ipAddress), 0);
    }

    public void logFailedLogin(String username, String reason, String ipAddress) {
        logEvent(null, "LOGIN_FAIL",
                String.format("Failed login for username: %s, reason: %s, IP: %s",
                        username, reason, ipAddress), 50);
    }

    public void logFailed2FA(String username, String ipAddress) {
        logEvent(null, "2FA_FAIL",
                String.format("Failed 2FA verification for username: %s, IP: %s",
                        username, ipAddress), 60);
    }

    public void logExpired2FA(String username) {
        logEvent(null, "2FA_EXPIRED",
                String.format("Expired 2FA code used for username: %s", username), 40);
    }

    public void logPurchase(User user, Long policyId, String amount, String paymentIntentId) {
        logEvent(user, "PURCHASE_SUCCESS",
                String.format("Policy purchased - PolicyID: %d, Amount: %s, PaymentIntent: %s",
                        policyId, amount, paymentIntentId), 0);
    }

    public void logFailedPurchase(User user, Long policyId, String reason) {
        logEvent(user, "PURCHASE_FAIL",
                String.format("Purchase failed - PolicyID: %d, Reason: %s",
                        policyId, reason), 30);
    }

    public void logPaymentAnomaly(User user, String details) {
        logEvent(user, "PAYMENT_ANOMALY", details, 80);
    }

    public void logWebhookFailure(String details) {
        logEvent(null, "WEBHOOK_FAIL", details, 40);
    }

    public void logSuspiciousActivity(String username, String activityType, String details) {
        logEvent(null, "SUSPICIOUS_ACTIVITY",
                String.format("Type: %s, Username: %s, Details: %s",
                        activityType, username, details), 90);
    }

    public void logRegistration(User user, String ipAddress) {
        logEvent(user, "REGISTRATION",
                String.format("New user registered from IP: %s", ipAddress), 0);
    }

    public void logTokenRefresh(User user) {
        logEvent(user, "TOKEN_REFRESH", "Access token refreshed", 0);
    }
}