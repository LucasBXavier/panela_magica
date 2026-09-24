package com.panelamagica.panelamagica.dto.receitas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceitaRequestDTO {

    @NotBlank
    @NotNull
    private String nome;

    @NotNull
    @NotBlank
    private String descricao;

    @NotNull
    @NotEmpty
    @Valid
    private List<ReceitaIngredienteRequestDTO> ingredientes;

    @NotNull
    @NotBlank
    private String modoPreparo;

    @NotNull
    @NotBlank
    private String tempoPreparo;

    @NotNull
    @NotBlank
    private String rendimento;

    @NotNull
    @NotBlank
    private String categoria;
    @NotNull
    @NotBlank
    private String unidadeMedida;
}
