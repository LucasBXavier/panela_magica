package com.panelamagica.panelamagica.dto.receitas;

import com.panelamagica.panelamagica.domain.enums.UnidadeMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceitaIngredienteRequestDTO {

    @NotBlank
    @Size(max = 255)
    private String nomeIngrediente;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal quantidade;

    @NotNull
    private UnidadeMedida unidadeMedida;
}
