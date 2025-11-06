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


    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(
            @RequestParam Long policyId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = userService.getUsernameFromToken(authHeader.substring(7));
            Long userId = userService.findByUsername(username).get().getId();

            Policy policy = policyService.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
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
                    .putMetadata("username", username) // Add username to metadata
                    .build();

            Session session = Session.create(params);

            // Save initial transaction with username
            //transactionService.saveTransaction(userId, username, policyId, policy.getPrice(), session.getId());

            // Save and mark transaction as completed immediately
            Transaction tx = transactionService.saveTransaction(userId, username, policyId, policy.getPrice(), session.getId());
            tx.setStatus("completed");
            transactionService.update(tx);

            // Generate PDF and email
            User user = userService.findByUsername(username).get();
            byte[] pdf = pdfService.generateReceipt(user, policy, tx);
            mailService.sendReceipt(user.getEmail(), pdf);

            return ResponseEntity.ok(Map.of(
                    "checkoutUrl", session.getUrl()
            ));

        } catch (Exception e) {
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

            if ("checkout.session.completed".equals(event.getType())) {
                System.out.println("Processing checkout.session.completed event");

                // Safely deserialize
                var deserialized = event.getDataObjectDeserializer();
                if (deserialized.getObject().isEmpty()) {
                    System.err.println("No object found in webhook event.");
                    return ResponseEntity.ok("No object found");
                }

                Session session = (Session) deserialized.getObject().get();
                System.out.println("Session ID: " + session.getId());
                System.out.println("PaymentIntent ID: " + session.getPaymentIntent());

                String paymentIntentId = session.getPaymentIntent();
                if (paymentIntentId == null) {
                    System.err.println("Session missing paymentIntent");
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
            }

            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException e) {
            System.err.println("Webhook signature verification failed: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Webhook processing failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

}