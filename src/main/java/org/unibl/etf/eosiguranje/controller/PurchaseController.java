package org.unibl.etf.eosiguranje.controller;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unibl.etf.eosiguranje.model.Policy;
import org.unibl.etf.eosiguranje.model.Transaction;
import org.unibl.etf.eosiguranje.service.PolicyService;
import org.unibl.etf.eosiguranje.service.TransactionService;
import org.unibl.etf.eosiguranje.service.UserService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;
// TODO: Stripe in frontend.
@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PolicyService policyService;
    private final TransactionService transactionService;
    private final UserService userService;

    // Step 1: Create PaymentIntent
    @PostMapping("/intent")
    public ResponseEntity<?> createPaymentIntent(@RequestParam Long policyId,
                                                 @RequestHeader("Authorization") String authHeader) throws Exception {
        String username = userService.getUsernameFromToken(authHeader.substring(7));
        Long userId = userService.findByUsername(username).get().getId();

        Policy policy = policyService.findById(policyId).orElseThrow(() -> new RuntimeException("Policy not found"));

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(policy.getPrice().multiply(BigDecimal.valueOf(100)).longValue())
                .setCurrency("usd")
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        transactionService.saveTransaction(userId, policyId, BigDecimal.valueOf(policy.getPrice().doubleValue()), intent.getId());

        return ResponseEntity.ok(Map.of(
                "clientSecret", intent.getClientSecret()
        ));

    }

    // Step 2: Confirmed payment → generate PDF and return receipt

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, String> payload,
                                            @RequestHeader("Authorization") String authHeader) throws IOException {
        try {
            String paymentIntentId = payload.get("paymentIntentId");
            if (paymentIntentId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "paymentIntentId is required"));
            }

            // Extract the payment intent ID from the client secret
            paymentIntentId = paymentIntentId.split("_secret_")[0];

            String username = userService.getUsernameFromToken(authHeader.substring(7));
            Long userId = userService.findByUsername(username).get().getId();

            Transaction tx = transactionService.findByPaymentIntentId(paymentIntentId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            // Update transaction status to completed
            tx = transactionService.updateTransactionStatus(tx, "completed");

            Policy policy = policyService.findById(tx.getPolicyId())
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            byte[] pdfBytes = generatePdfReceipt(tx, policy, username);
            String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);

            return ResponseEntity.ok(Map.of(
                    "message", "payment_success",
                    "pdfBase64", pdfBase64
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private byte[] generatePdfReceipt(Transaction tx, Policy policy, String username) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.setLeading(20f);
                content.newLineAtOffset(50, 700);

                content.showText("Insurance Policy Purchase Receipt");
                content.newLine();
                content.newLine();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.showText("Username: " + username);
                content.newLine();
                content.showText("Policy: " + policy.getName());
                content.newLine();
                content.showText("Amount Paid: $" + policy.getPrice());
                content.newLine();
                content.showText("Transaction ID: " + tx.getId());
                content.newLine();
                content.showText("Thank you for your purchase!");

                content.endText();
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                document.save(baos);
                return baos.toByteArray();
            }
        }
    }
}