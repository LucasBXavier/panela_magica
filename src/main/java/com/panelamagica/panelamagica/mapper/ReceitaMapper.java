package com.panelamagica.panelamagica.mapper;

import com.panelamagica.panelamagica.domain.entites.Ingrediente;
import com.panelamagica.panelamagica.domain.entites.ReceitaIngrediente;
import com.panelamagica.panelamagica.domain.entites.Receitas;
import com.panelamagica.panelamagica.dto.receitas.*;
import com.panelamagica.panelamagica.repository.IngredienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReceitaMapper {

    private final IngredienteRepository ingredienteRepository;

    /** Caminho público da imagem de uma receita (fonte única da URL). */
    public static String imagemUrl(UUID receitaId) {
        return "/api/v1/receitas/" + receitaId + "/imagem";
    }

    public Receitas toEntity(ReceitaRequestDTO dto) {
        Receitas receita = new Receitas();
        receita.setNome(dto.getNome());
        receita.setDescricao(dto.getDescricao());
        receita.setModoPreparo(dto.getModoPreparo());
        receita.setTempoPreparo(dto.getTempoPreparo());
        receita.setRendimento(dto.getRendimento());
        receita.setCategoria(dto.getCategoria());

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
        receitaIngrediente.setUnidadeMedida(dto.getUnidadeMedida());

        return receitaIngrediente;
    }

    public ReceitaResponseDTO toResponseDTO(Receitas entity) {
        ReceitaResponseDTO dto = new ReceitaResponseDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setDescricao(entity.getDescricao());
        dto.setModoPreparo(entity.getModoPreparo());
        dto.setTempoPreparo(entity.getTempoPreparo());
        dto.setRendimento(entity.getRendimento());
        dto.setCategoria(entity.getCategoria().name());
        dto.setDataCriacao(entity.getCreatedAt());
        if (entity.getImagem() != null) {
            dto.setImagemUrl(imagemUrl(entity.getId()));
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

    /** Atualização parcial: campos nulos são ignorados (a validação de "não vazio" é feita no DTO). */
    public void updateEntityFromDTO(ReceitasUpdateDTO dto, Receitas receita) {
        if (dto.getNome() != null) receita.setNome(dto.getNome());
        if (dto.getDescricao() != null) receita.setDescricao(dto.getDescricao());
        if (dto.getModoPreparo() != null) receita.setModoPreparo(dto.getModoPreparo());
        if (dto.getTempoPreparo() != null) receita.setTempoPreparo(dto.getTempoPreparo());
        if (dto.getRendimento() != null) receita.setRendimento(dto.getRendimento());
        if (dto.getCategoria() != null) receita.setCategoria(dto.getCategoria());

        if (dto.getIngredientes() != null) {
            receita.getIngredientes().clear();
            dto.getIngredientes().forEach(i -> receita.addIngrediente(toReceitaIngrediente(i)));
        }
    }
}
