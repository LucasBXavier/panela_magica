package com.panelamagica.panelamagica.dto.receitas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceitaIngredienteResponseDTO {

    private String nomeIngrediente;
    private BigDecimal quantidade;
    private String unidadeMedida;
}
