package org.unibl.etf.eosiguranje.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_2fa")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User2FA {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @Column(nullable = false)
    private String code; // 6-digit code

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private Boolean used = false;
}
