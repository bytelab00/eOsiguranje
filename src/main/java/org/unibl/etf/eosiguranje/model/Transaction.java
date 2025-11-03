package org.unibl.etf.eosiguranje.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false)
    private String username; // Add this field


    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "provider")
    private String provider;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(nullable = false)
    private String status; // Add this field

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (provider == null) {
            provider = "stripe";
        }
        if (providerId == null) {
            providerId = 1L;
        }
        if (status == null) {
            status = "pending"; // Default status when creating a transaction
        }
    }
}