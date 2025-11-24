package org.unibl.etf.eosiguranje.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String username;
    private String email;
    private String role;
    private Boolean enabled = true;
    // Password je opciono - ako je null, ne mijenjamo ga
    private String password;
}