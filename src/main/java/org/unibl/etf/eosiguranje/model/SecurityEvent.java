package org.unibl.etf.eosiguranje.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id")
    @JsonIgnore
    private User user; // can be null for anonymous events

    @Column(nullable = false)
    private String action; // LOGIN_FAIL, PAYMENT_ANOMALY, etc.

    @Column(length = 1000)
    private String details;

    private Integer riskScore; // optional

    private LocalDateTime createdAt = LocalDateTime.now();
}
