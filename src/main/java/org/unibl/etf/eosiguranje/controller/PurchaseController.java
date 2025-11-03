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
import org.unibl.etf.eosiguranje.service.PolicyService;
import org.unibl.etf.eosiguranje.service.TransactionService;
import org.unibl.etf.eosiguranje.service.UserPolicyService;
import org.unibl.etf.eosiguranje.service.UserService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PolicyService policyService;
    private final TransactionService transactionService;
    private final UserService userService;
    private final UserPolicyService userPolicyService;

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
            transactionService.saveTransaction(userId, username, policyId, policy.getPrice(), session.getId());

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
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().get();

                Transaction tx = transactionService.findByPaymentIntentId(session.getId())
                        .orElseThrow(() -> new RuntimeException("Transaction not found"));

                transactionService.updateTransactionStatus(tx, "completed");

                User user = userService.findByUsername(tx.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                Policy policy = policyService.findById(tx.getPolicyId())
                        .orElseThrow(() -> new RuntimeException("Policy not found"));

                userPolicyService.createUserPolicy(user, policy, null);
            }

            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}