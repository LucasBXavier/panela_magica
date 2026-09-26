package com.panelamagica.panelamagica.dto.login;

import com.panelamagica.panelamagica.dto.user.UsuarioResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;
    private String type;
    private long expiresIn;
    private String refreshToken;
    private long refreshExpiresIn;
    private UsuarioResponseDTO usuario;
}
