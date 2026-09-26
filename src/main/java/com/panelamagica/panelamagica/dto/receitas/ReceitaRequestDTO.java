package com.panelamagica.panelamagica.dto.receitas;

import com.panelamagica.panelamagica.domain.enums.Categoria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceitaRequestDTO {

    @NotBlank
    @Size(max = 255)
    private String nome;

    @NotBlank
    @Size(max = 5000)
    private String descricao;

    @NotEmpty
    private List<@NotNull @Valid ReceitaIngredienteRequestDTO> ingredientes;

    @NotBlank
    @Size(max = 20000)
    private String modoPreparo;

    @NotBlank
    @Size(max = 255)
    private String tempoPreparo;

    @NotBlank
    @Size(max = 255)
    private String rendimento;

    @NotNull
    private Categoria categoria;
}
