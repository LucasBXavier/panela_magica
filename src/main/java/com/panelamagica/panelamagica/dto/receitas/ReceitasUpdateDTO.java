package com.panelamagica.panelamagica.dto.receitas;

import com.panelamagica.panelamagica.domain.enums.Categoria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Atualização parcial: campos ausentes (null) não são alterados; se enviados, não podem ser vazios.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceitasUpdateDTO {

    private static final String NAO_VAZIO = ".*\\S.*";

    @Pattern(regexp = NAO_VAZIO, message = "não pode ser vazio")
    @Size(max = 255)
    private String nome;

    @Pattern(regexp = NAO_VAZIO, message = "não pode ser vazio")
    @Size(max = 5000)
    private String descricao;

    @Size(min = 1, message = "A receita deve ter ao menos um ingrediente")
    private List<@NotNull @Valid ReceitaIngredienteRequestDTO> ingredientes;

    @Pattern(regexp = NAO_VAZIO, message = "não pode ser vazio")
    @Size(max = 20000)
    private String modoPreparo;

    @Pattern(regexp = NAO_VAZIO, message = "não pode ser vazio")
    @Size(max = 255)
    private String tempoPreparo;

    @Pattern(regexp = NAO_VAZIO, message = "não pode ser vazio")
    @Size(max = 255)
    private String rendimento;

    private Categoria categoria;
}
