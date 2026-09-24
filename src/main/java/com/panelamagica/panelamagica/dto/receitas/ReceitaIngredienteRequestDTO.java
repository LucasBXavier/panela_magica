package com.panelamagica.panelamagica.dto.receitas;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceitaIngredienteRequestDTO {

    @NotNull
    @NotBlank
    private String nomeIngrediente;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal quantidade;

    @NotNull
    @NotBlank
    private String unidadeMedida;
}
