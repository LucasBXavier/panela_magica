package com.panelamagica.panelamagica.dto.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotBlank(message = "O email é obrigatório")
    @Email(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Email inválido")
    @NotNull
    private String email;
    @NotNull
    @NotBlank(message = "Senha obrigatória")
    @Size(
            min = 6,
            message =
                    "Senha deve ter no mínimo 6 caracteres, incluindo letras, números e caracteres especiais")
    private String senha;
}
