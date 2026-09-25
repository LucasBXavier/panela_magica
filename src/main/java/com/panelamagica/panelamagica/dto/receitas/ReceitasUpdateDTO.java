package com.panelamagica.panelamagica.dto.receitas;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceitasUpdateDTO {

    private String nome;
    private String descricao;

    @Valid
    private List<ReceitaIngredienteRequestDTO> ingredientes;

    private String modoPreparo;
    private String tempoPreparo;
    private String rendimento;
    private String categoria;
}
