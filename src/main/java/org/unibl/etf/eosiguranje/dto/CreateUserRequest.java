package org.unibl.etf.eosiguranje.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String role; // CLIENT, ADMIN
    private Boolean enabled = true;
}