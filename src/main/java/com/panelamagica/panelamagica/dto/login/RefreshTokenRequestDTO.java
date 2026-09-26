package com.panelamagica.panelamagica.dto.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefreshTokenRequestDTO {

    @NotBlank(message = "refreshToken é obrigatório")
    @Size(max = 200, message = "refreshToken inválido")
    private String refreshToken;
}
