package org.unibl.etf.eosiguranje.controller;

import org.unibl.etf.eosiguranje.service.MailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestMailController {

    private final MailService mailService;

    public TestMailController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/mail")
    public ResponseEntity<String> sendTest(@RequestParam(defaultValue = "test@example.com") String to) {
        mailService.sendTestEmail(to);
        return ResponseEntity.ok("Sent to: " + to);
    }
}
