package com.panelamagica.panelamagica.mapper;

import com.panelamagica.panelamagica.domain.entites.Ingrediente;
import com.panelamagica.panelamagica.domain.entites.ReceitaIngrediente;
import com.panelamagica.panelamagica.domain.entites.Receitas;
import com.panelamagica.panelamagica.domain.enums.Categoria;
import com.panelamagica.panelamagica.domain.enums.UnidadeMedida;
import com.panelamagica.panelamagica.dto.receitas.ReceitaIngredienteRequestDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaIngredienteResponseDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaRequestDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaResponseDTO;
import com.panelamagica.panelamagica.repository.IngredienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        receita.setCategoria(Categoria.valueOf(dto.getCategoria()));
        receita.setUnidadeMedida(UnidadeMedida.valueOf(dto.getUnidadeMedida()));

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
        receitaIngrediente.setUnidadeMedida(UnidadeMedida.valueOf(dto.getUnidadeMedida()));

        return receitaIngrediente;
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
        dto.setUnidadeMedida(entity.getUnidadeMedida().name());
        dto.setDataCriacao(entity.getCreatedAt());

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
}
