package com.panelamagica.panelamagica.mapper;

import com.panelamagica.panelamagica.domain.entites.Ingrediente;
import com.panelamagica.panelamagica.domain.entites.ReceitaImagem;
import com.panelamagica.panelamagica.domain.entites.Receitas;
import com.panelamagica.panelamagica.domain.enums.Categoria;
import com.panelamagica.panelamagica.domain.enums.UnidadeMedida;
import com.panelamagica.panelamagica.dto.receitas.ReceitaIngredienteRequestDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaRequestDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitasUpdateDTO;
import com.panelamagica.panelamagica.repository.IngredienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReceitaMapperTest {

    private IngredienteRepository ingredienteRepository;
    private ReceitaMapper mapper;

    @BeforeEach
    void setUp() {
        ingredienteRepository = mock(IngredienteRepository.class);
        when(ingredienteRepository.findByNomeIgnoreCase(any())).thenReturn(Optional.empty());
        when(ingredienteRepository.save(any(Ingrediente.class))).thenAnswer(inv -> inv.getArgument(0));
        mapper = new ReceitaMapper(ingredienteRepository);
    }

    private ReceitaIngredienteRequestDTO ingrediente(String nome, String qtd, UnidadeMedida un) {
        return new ReceitaIngredienteRequestDTO(nome, new BigDecimal(qtd), un);
    }

    private ReceitaRequestDTO request() {
        ReceitaRequestDTO dto = new ReceitaRequestDTO();
        dto.setNome("Bolo");
        dto.setDescricao("d");
        dto.setModoPreparo("m");
        dto.setTempoPreparo("1h");
        dto.setRendimento("8");
        dto.setCategoria(Categoria.DOCE);
        dto.setIngredientes(List.of(ingrediente("Farinha", "2", UnidadeMedida.XICARA), ingrediente("Sal", "1", UnidadeMedida.A_GOSTO)));
        return dto;
    }

    @Test
    void toEntityCopiaCamposEAssociaIngredientes() {
        Receitas r = mapper.toEntity(request());

        assertEquals("Bolo", r.getNome());
        assertEquals(Categoria.DOCE, r.getCategoria());
        assertEquals(2, r.getIngredientes().size());
        assertTrue(r.getIngredientes().stream().allMatch(i -> i.getReceita() == r), "lado inverso da associação");
        assertEquals(UnidadeMedida.XICARA, r.getIngredientes().get(0).getUnidadeMedida());
    }

    @Test
    void ingredienteExistenteEhReaproveitadoEOsNovosSaoCriados() {
        Ingrediente existente = new Ingrediente();
        existente.setNome("Farinha");
        when(ingredienteRepository.findByNomeIgnoreCase("Farinha")).thenReturn(Optional.of(existente));

        Receitas r = mapper.toEntity(request());

        assertSame(existente, r.getIngredientes().get(0).getIngrediente());
        verify(ingredienteRepository, times(1)).save(any(Ingrediente.class)); // só o "Sal"
    }

    @Test
    void toResponseDtoNaoTemImagemUrlSemImagem() {
        Receitas r = mapper.toEntity(request());
        r.setId(UUID.randomUUID());
        r.setCreatedAt(OffsetDateTime.now());

        var dto = mapper.toResponseDTO(r);

        assertNull(dto.getImagemUrl());
        assertEquals("DOCE", dto.getCategoria());
        assertEquals(2, dto.getIngredientes().size());
        assertEquals("XICARA", dto.getIngredientes().get(0).getUnidadeMedida());
    }

    @Test
    void toResponseDtoTemImagemUrlQuandoHaImagem() {
        Receitas r = mapper.toEntity(request());
        r.setId(UUID.randomUUID());
        r.setImagem(new ReceitaImagem());

        assertEquals("/api/v1/receitas/" + r.getId() + "/imagem", mapper.toResponseDTO(r).getImagemUrl());
        assertEquals(ReceitaMapper.imagemUrl(r.getId()), mapper.toResponseDTO(r).getImagemUrl());
    }

    @Test
    void updateIgnoraCamposNulos() {
        Receitas r = mapper.toEntity(request());
        ReceitasUpdateDTO dto = new ReceitasUpdateDTO();
        dto.setRendimento("16");

        mapper.updateEntityFromDTO(dto, r);

        assertEquals("16", r.getRendimento());
        assertEquals("Bolo", r.getNome());
        assertEquals(Categoria.DOCE, r.getCategoria());
        assertEquals(2, r.getIngredientes().size());
    }

    @Test
    void updateComIngredientesSubstituiAListaInteira() {
        Receitas r = mapper.toEntity(request());
        ReceitasUpdateDTO dto = new ReceitasUpdateDTO();
        dto.setIngredientes(List.of(ingrediente("Cenoura", "3", UnidadeMedida.UNIDADE)));
        dto.setCategoria(Categoria.SOBREMESA);

        mapper.updateEntityFromDTO(dto, r);

        assertEquals(1, r.getIngredientes().size());
        assertEquals("Cenoura", r.getIngredientes().get(0).getIngrediente().getNome());
        assertEquals(Categoria.SOBREMESA, r.getCategoria());
    }
}
