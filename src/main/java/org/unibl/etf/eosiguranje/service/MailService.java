
package org.unibl.etf.eosiguranje.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }


    public void sendReceipt(String to, byte[] pdfBytes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Your Insurance Receipt");
            helper.setText("Thank you for your purchase. Please find your insurance receipt attached.");
            helper.addAttachment("receipt.pdf", () -> new java.io.ByteArrayInputStream(pdfBytes));
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    public void sendTestEmail(String to) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setFrom("no-reply@insurance.local");
        msg.setSubject("Test - Mailtrap");
        msg.setText("Mailtrap configuration working.");
        mailSender.send(msg);
    }

    public void sendEmail(String to, String message) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setFrom("no-reply@insurance.local");
        msg.setSubject("2FA Code");
        msg.setText(message);
        mailSender.send(msg);
    }
}