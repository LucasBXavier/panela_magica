package com.panelamagica.panelamagica.dto.receitas;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceitaResponseDTO {

    private String id;
    private String nome;
    private String descricao;
    private List<ReceitaIngredienteResponseDTO> ingredientes;
    private String modoPreparo;
    private String tempoPreparo;
    private String rendimento;
    private String categoria;
    private String unidadeMedida;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private OffsetDateTime dataCriacao;

}
