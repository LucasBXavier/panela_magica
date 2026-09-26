package com.panelamagica.panelamagica.dto.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotBlank(message = "O email é obrigatório")
    @Email(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Email inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(max = 72, message = "Senha deve ter no máximo 72 caracteres")
    private String senha;
}
