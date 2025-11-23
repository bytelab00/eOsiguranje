package org.unibl.etf.eosiguranje.controller;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unibl.etf.eosiguranje.model.Policy;
import org.unibl.etf.eosiguranje.model.Transaction;
import org.unibl.etf.eosiguranje.model.User;
import org.unibl.etf.eosiguranje.service.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class PurchaseController {

    private final PolicyService policyService;
    private final TransactionService transactionService;
    private final UserService userService;
    private final UserPolicyService userPolicyService;
    private final PdfService pdfService;
    private final MailService mailService;
    private final SecurityEventService securityEventService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    @GetMapping("/success")
    public ResponseEntity<?> handleSuccess(@RequestParam String session_id) {
        try {
            Session session = Session.retrieve(session_id);

            String username = session.getMetadata().get("username");
            Long policyId = Long.parseLong(session.getMetadata().get("policy_id"));
            Long userId = Long.parseLong(session.getMetadata().get("user_id"));

            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Policy policy = policyService.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            // Find and update transaction
            Transaction tx = transactionService.findByPaymentIntentId(session_id)
                    .orElseGet(() -> transactionService.saveTransaction(userId, username, policyId, policy.getPrice(), session_id));

            tx.setStatus("completed");
            transactionService.update(tx);

            // Create user policy
            userPolicyService.createUserPolicy(user, policy, null);

            // ✅ LOG SUCCESSFUL PURCHASE HERE
            securityEventService.logPurchase(user, policyId,
                    policy.getPrice().toString(), session_id);

            return ResponseEntity.ok(Map.of(
                    "message", "Payment successful",
                    "policy", policy.getName()
            ));

        } catch (Exception e) {
            securityEventService.logEvent("PAYMENT_SUCCESS_ERROR",
                    String.format("Error handling success: %s", e.getMessage()), 40);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/cancel")
    public ResponseEntity<?> handleCancel(@RequestParam(required = false) String session_id) {
        try {
            if (session_id != null && !session_id.isEmpty()) {
                Session session = Session.retrieve(session_id);

                String username = session.getMetadata().get("username");
                Long policyId = Long.parseLong(session.getMetadata().get("policy_id"));

                User user = userService.findByUsername(username).orElse(null);

                // Find and mark transaction as cancelled
                Transaction tx = transactionService.findByPaymentIntentId(session_id).orElse(null);
                if (tx != null) {
                    tx.setStatus("cancelled");
                    transactionService.update(tx);
                }

                // ✅ LOG CANCELLED PURCHASE HERE
                if (user != null) {
                    securityEventService.logFailedPurchase(user, policyId, "User cancelled payment");
                }
            }

            return ResponseEntity.ok(Map.of("message", "Payment cancelled"));

        } catch (Exception e) {
            securityEventService.logEvent("PAYMENT_CANCEL_ERROR",
                    String.format("Error handling cancel: %s", e.getMessage()), 30);
            return ResponseEntity.ok(Map.of("message", "Payment cancelled"));
        }
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(
            @RequestParam Long policyId,
            @RequestHeader("Authorization") String authHeader) {
        String username = null;
        User user = null;

        try {
            username = userService.getUsernameFromToken(authHeader.substring(7));
            user = userService.findByUsername(username).orElseThrow(() ->
                    new RuntimeException("User not found"));
            Long userId = user.getId();

            Policy policy = policyService.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            // Check for suspicious purchase patterns
            checkSuspiciousPurchaseActivity(user, policy);

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")  // ADD THIS
                    .setCancelUrl(cancelUrl + "?session_id={CHECKOUT_SESSION_ID}")    // ADD THIS
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(policy.getPrice().multiply(new BigDecimal(100)).longValue())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(policy.getName())
                                            .build())
                                    .build())
                            .setQuantity(1L)
                            .build())
                    .putMetadata("policy_id", policyId.toString())
                    .putMetadata("user_id", userId.toString())
                    .putMetadata("username", username)
                    .build();

            Session session = Session.create(params);

            // Save and mark transaction as completed immediately
            Transaction tx = transactionService.saveTransaction(
                    userId, username, policyId, policy.getPrice(), session.getId());
            tx.setStatus("completed");
            transactionService.update(tx);

            // Log purchase initiation
            securityEventService.logEvent(user, "PURCHASE_INITIATED",
                    String.format("Checkout session created - PolicyID: %d, Amount: %s, SessionID: %s",
                            policyId, policy.getPrice().toString(), session.getId()), 0);

            // Generate PDF and email
            // TODO: Uncomment Email sending (limit almost reached)
            /*
            byte[] pdf = pdfService.generateReceipt(user, policy, tx);
            mailService.sendReceipt(user.getEmail(), pdf);
            */

            // Log successful purchase
            //securityEventService.logPurchase(user, policyId,
            //        policy.getPrice().toString(), session.getId());

            return ResponseEntity.ok(Map.of(
                    "checkoutUrl", session.getUrl()
            ));

        } catch (Exception e) {
            // Log failed purchase
            if (user != null) {
                securityEventService.logFailedPurchase(user, policyId, e.getMessage());
            } else {
                securityEventService.logEvent("PURCHASE_FAIL",
                        String.format("PolicyID: %d, Username: %s, Error: %s",
                                policyId, username, e.getMessage()), 40);
            }

            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            System.out.println("Webhook received - Signature: " + sigHeader);
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            System.out.println("Event type received: " + event.getType());

            // Log webhook received
            securityEventService.logEvent("WEBHOOK_RECEIVED",
                    String.format("Stripe webhook - Event type: %s", event.getType()), 0);

            if ("checkout.session.completed".equals(event.getType())) {
                System.out.println("Processing checkout.session.completed event");

                // Safely deserialize
                var deserialized = event.getDataObjectDeserializer();
                if (deserialized.getObject().isEmpty()) {
                    System.err.println("No object found in webhook event.");
                    securityEventService.logWebhookFailure("No object found in webhook event");
                    return ResponseEntity.ok("No object found");
                }

                Session session = (Session) deserialized.getObject().get();
                System.out.println("Session ID: " + session.getId());
                System.out.println("PaymentIntent ID: " + session.getPaymentIntent());

                String paymentIntentId = session.getPaymentIntent();
                if (paymentIntentId == null) {
                    System.err.println("Session missing paymentIntent");
                    securityEventService.logWebhookFailure(
                            String.format("Session missing paymentIntent - SessionID: %s", session.getId()));
                    return ResponseEntity.ok("Missing paymentIntent");
                }

                Transaction tx = transactionService.findByPaymentIntentId(paymentIntentId)
                        .orElseThrow(() -> new RuntimeException("Transaction not found for " + paymentIntentId));

                transactionService.updateTransactionStatus(tx, "completed");
                System.out.println("Transaction marked as completed");

                User user = userService.findByUsername(tx.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));

                Policy policy = policyService.findById(tx.getPolicyId())
                        .orElseThrow(() -> new RuntimeException("Policy not found"));

                userPolicyService.createUserPolicy(user, policy, null);
                System.out.println("User policy created successfully");

                // Log webhook processed successfully
                securityEventService.logEvent(user, "WEBHOOK_PROCESSED",
                        String.format("Payment completed - PaymentIntent: %s, PolicyID: %d",
                                paymentIntentId, tx.getPolicyId()), 0);
            }

            return ResponseEntity.ok().build();

        } catch (SignatureVerificationException e) {
            System.err.println("Webhook signature verification failed: " + e.getMessage());
            securityEventService.logWebhookFailure(
                    String.format("Signature verification failed: %s", e.getMessage()));
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            System.err.println("Webhook processing failed: " + e.getMessage());
            e.printStackTrace();
            securityEventService.logWebhookFailure(
                    String.format("Processing failed: %s", e.getMessage()));
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Check for suspicious purchase activity
     */
    private void checkSuspiciousPurchaseActivity(User user, Policy policy) {
        try {
            // Check for multiple purchases in short time
            LocalDateTime since = LocalDateTime.now().minusMinutes(5);
            // You'll need to implement this in your transaction service
            // Long recentPurchases = transactionService.countRecentPurchasesByUser(user.getId(), since);

            // Example: If user tries to buy the same expensive policy multiple times rapidly
            if (policy.getPrice().compareTo(new BigDecimal("1000")) > 0) {
                securityEventService.logEvent(user, "HIGH_VALUE_PURCHASE",
                        String.format("High-value purchase attempt - PolicyID: %d, Amount: %s",
                                policy.getId(), policy.getPrice().toString()), 30);
            }

        } catch (Exception e) {
            System.err.println("Error checking suspicious purchase activity: " + e.getMessage());
        }
    }
}