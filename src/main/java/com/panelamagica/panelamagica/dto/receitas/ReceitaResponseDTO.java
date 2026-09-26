package com.panelamagica.panelamagica.dto.receitas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceitaResponseDTO {

    private UUID id;
    private String nome;
    private String descricao;
    private List<ReceitaIngredienteResponseDTO> ingredientes;
    private String modoPreparo;
    private String tempoPreparo;
    private String rendimento;
    private String categoria;
    private String imagemUrl;

    /** ISO-8601 com fuso (ex.: 2026-09-25T21:41:28.096Z). */
    private OffsetDateTime dataCriacao;

}
