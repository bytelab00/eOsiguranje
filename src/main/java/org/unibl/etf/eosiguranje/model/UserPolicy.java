package org.unibl.etf.eosiguranje.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @ManyToOne
    @JoinColumn(name="policy_id", nullable=false)
    private Policy policy;

    private LocalDateTime purchaseDate = LocalDateTime.now();

    private String pdfPath;
}
