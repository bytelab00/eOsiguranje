package org.unibl.etf.eosiguranje.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PurchaseResponse {
  //  private String message;
  //  private String clientSecret;
    private String checkoutUrl;

}