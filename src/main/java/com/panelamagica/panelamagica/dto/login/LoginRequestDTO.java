package com.panelamagica.panelamagica.dto.login;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotBlank(message = "O email é obrigatório")
    @Email(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Email inválido")
    @NotNull
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @NotNull(message = "Senha não pode ser nula")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message =
                    "Senha deve conter pelo menos 8 caracteres, incluindo uma letra maiúscula, uma minúscula, um número e um caractere especial")
    private String senha;
}
