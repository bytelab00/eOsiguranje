package org.unibl.etf.eosiguranje.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String provider; // Stripe, PayPal

    @Column(nullable = false)
    private String providerId; // id from payment gateway

    @Column(nullable = false)
    private String status; // SUCCESS, FAILED, PENDING

    private LocalDateTime createdAt = LocalDateTime.now();
}
