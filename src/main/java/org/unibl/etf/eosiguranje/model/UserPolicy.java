package org.unibl.etf.eosiguranje.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name="policy_id", nullable=false)
    @JsonIgnore
    private Policy policy;

    private LocalDateTime purchaseDate = LocalDateTime.now();

    @Column(name="pdf_path", nullable=true)
    private String pdfPath;
}