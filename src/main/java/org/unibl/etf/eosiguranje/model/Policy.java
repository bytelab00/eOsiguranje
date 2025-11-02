package org.unibl.etf.eosiguranje.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // LIFE, TRAVEL, PROPERTY

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;
}
