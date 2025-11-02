package org.unibl.etf.eosiguranje.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTestEmail(String to) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setFrom("no-reply@insurance.local"); // optional
        msg.setSubject("Test - Mailtrap");
        msg.setText("Mailtrap configuration working.");
        mailSender.send(msg);
    }
}
