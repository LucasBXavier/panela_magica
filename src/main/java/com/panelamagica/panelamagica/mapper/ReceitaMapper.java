package com.panelamagica.panelamagica.mapper;

import com.panelamagica.panelamagica.domain.entites.Ingrediente;
import com.panelamagica.panelamagica.domain.entites.ReceitaIngrediente;
import com.panelamagica.panelamagica.domain.entites.Receitas;
import com.panelamagica.panelamagica.domain.enums.Categoria;
import com.panelamagica.panelamagica.domain.enums.UnidadeMedida;
import com.panelamagica.panelamagica.dto.receitas.*;
import com.panelamagica.panelamagica.exception.BusinessRuleException;
import com.panelamagica.panelamagica.repository.IngredienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReceitaMapper {

    private final IngredienteRepository ingredienteRepository;

    public Receitas toEntity(ReceitaRequestDTO dto) {
        Receitas receita = new Receitas();
        receita.setNome(dto.getNome());
        receita.setDescricao(dto.getDescricao());
        receita.setModoPreparo(dto.getModoPreparo());
        receita.setTempoPreparo(dto.getTempoPreparo());
        receita.setRendimento(dto.getRendimento());
        receita.setCategoria(parseEnum(Categoria.class, dto.getCategoria(), "categoria"));

        dto.getIngredientes().forEach(ingredienteDTO ->
                receita.addIngrediente(toReceitaIngrediente(ingredienteDTO)));

        return receita;
    }

    private ReceitaIngrediente toReceitaIngrediente(ReceitaIngredienteRequestDTO dto) {
        Ingrediente ingrediente = ingredienteRepository.findByNomeIgnoreCase(dto.getNomeIngrediente())
                .orElseGet(() -> {
                    Ingrediente novoIngrediente = new Ingrediente();
                    novoIngrediente.setNome(dto.getNomeIngrediente());
                    return ingredienteRepository.save(novoIngrediente);
                });

        ReceitaIngrediente receitaIngrediente = new ReceitaIngrediente();
        receitaIngrediente.setIngrediente(ingrediente);
        receitaIngrediente.setQuantidade(dto.getQuantidade());
        receitaIngrediente.setUnidadeMedida(
                parseEnum(UnidadeMedida.class, dto.getUnidadeMedida(), "ingredientes.unidadeMedida"));

        return receitaIngrediente;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> tipo, String valor, String campo) {
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(
                    campo + " inválido: '" + valor + "'. Valores aceitos: " + Arrays.toString(tipo.getEnumConstants()));
        }
    }

    public ReceitaResponseDTO toResponseDTO(Receitas entity) {
        ReceitaResponseDTO dto = new ReceitaResponseDTO();
        dto.setId(String.valueOf(entity.getId()));
        dto.setNome(entity.getNome());
        dto.setDescricao(entity.getDescricao());
        dto.setModoPreparo(entity.getModoPreparo());
        dto.setTempoPreparo(entity.getTempoPreparo());
        dto.setRendimento(entity.getRendimento());
        dto.setCategoria(entity.getCategoria().name());
        dto.setDataCriacao(entity.getCreatedAt());
        if (entity.getImagem() != null) {
            dto.setImagemUrl("/api/v1/receitas/imagem/" + entity.getId());
        }

        List<ReceitaIngredienteResponseDTO> ingredientes = entity.getIngredientes().stream()
                .map(this::toReceitaIngredienteResponseDTO)
                .toList();
        dto.setIngredientes(ingredientes);

        return dto;
    }

    private ReceitaIngredienteResponseDTO toReceitaIngredienteResponseDTO(ReceitaIngrediente entity) {
        ReceitaIngredienteResponseDTO dto = new ReceitaIngredienteResponseDTO();
        dto.setNomeIngrediente(entity.getIngrediente().getNome());
        dto.setQuantidade(entity.getQuantidade());
        dto.setUnidadeMedida(entity.getUnidadeMedida().name());
        return dto;
    }

    public void updateEntityFromDTO(ReceitasUpdateDTO dto, Receitas receita) {
        if (dto.getNome() != null) receita.setNome(naoVazio(dto.getNome(), "nome"));
        if (dto.getDescricao() != null) receita.setDescricao(naoVazio(dto.getDescricao(), "descricao"));
        if (dto.getModoPreparo() != null) receita.setModoPreparo(naoVazio(dto.getModoPreparo(), "modoPreparo"));
        if (dto.getTempoPreparo() != null) receita.setTempoPreparo(naoVazio(dto.getTempoPreparo(), "tempoPreparo"));
        if (dto.getRendimento() != null) receita.setRendimento(naoVazio(dto.getRendimento(), "rendimento"));
        if (dto.getCategoria() != null) {
            receita.setCategoria(parseEnum(Categoria.class, dto.getCategoria(), "categoria"));
        }

        if (dto.getIngredientes() != null) {
            if (dto.getIngredientes().isEmpty()) {
                throw new BusinessRuleException("A receita deve ter ao menos um ingrediente");
            }
            receita.getIngredientes().clear();
            dto.getIngredientes().forEach(i -> receita.addIngrediente(toReceitaIngrediente(i)));
        }
    }

    private String naoVazio(String valor, String campo) {
        if (valor.isBlank()) {
            throw new BusinessRuleException(campo + " não pode ser vazio");
        }
        return valor;
    }
}
