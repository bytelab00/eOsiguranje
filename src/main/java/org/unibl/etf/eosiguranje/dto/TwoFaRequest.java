package org.unibl.etf.eosiguranje.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFaRequest {
    private Long user2FAId;
    private String code;
}
