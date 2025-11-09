package org.unibl.etf.eosiguranje.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.unibl.etf.eosiguranje.model.Policy;
import org.unibl.etf.eosiguranje.model.Transaction;
import org.unibl.etf.eosiguranje.model.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class PdfService {

    public byte[] generateInvoice(User user, Policy policy, Transaction amount) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 20);
                content.setLeading(22f);
                content.newLineAtOffset(50, 700);

                content.showText("Invoice");
                content.newLine();
                content.newLine();

                content.setFont(PDType1Font.HELVETICA, 12);
                content.showText("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                content.newLine();
                content.showText("User: " + user.getUsername() + " (" + user.getEmail() + ")");
                content.newLine();
                content.showText("Policy: " + policy.getName());
                content.newLine();
                content.showText("Type: " + policy.getType());
                content.newLine();
                content.showText("Amount: $" + amount);
                content.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    public byte[] generateReceipt(User user, Policy policy, Transaction tx) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setFont(PDType1Font.HELVETICA_BOLD, 20);
                content.beginText();
                content.newLineAtOffset(200, 750);
                content.showText("Insurance Receipt");
                content.endText();

                content.setFont(PDType1Font.HELVETICA, 12);
                int y = 700;
                content.beginText();
                content.newLineAtOffset(50, y);
                content.showText("User: " + user.getUsername());
                content.newLineAtOffset(0, -20);
                content.showText("Email: " + user.getEmail());
                content.newLineAtOffset(0, -20);
                content.showText("Policy: " + policy.getName());
                content.newLineAtOffset(0, -20);
                content.showText("Price: $" + policy.getPrice());
                content.newLineAtOffset(0, -20);
                content.showText("Transaction ID: " + tx.getStripePaymentIntentId());
                content.newLineAtOffset(0, -20);
                content.showText("Status: " + tx.getStatus());
                content.newLineAtOffset(0, -20);
                content.showText("Date: " + tx.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                content.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    public String generateBase64Receipt(User user, Policy policy, Transaction tx) {
        return Base64.getEncoder().encodeToString(generateReceipt(user, policy, tx));
    }
}
