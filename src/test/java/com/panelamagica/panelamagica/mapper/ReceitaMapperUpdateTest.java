package com.panelamagica.panelamagica.mapper;

import com.panelamagica.panelamagica.domain.entites.Receitas;
import com.panelamagica.panelamagica.domain.enums.Categoria;
import com.panelamagica.panelamagica.dto.receitas.ReceitasUpdateDTO;
import com.panelamagica.panelamagica.repository.IngredienteRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReceitaMapperUpdateTest {

    private final ReceitaMapper mapper = new ReceitaMapper(mock(IngredienteRepository.class));

    private Receitas base() {
        Receitas r = new Receitas();
        r.setNome("n");
        r.setDescricao("d");
        r.setModoPreparo("m");
        r.setTempoPreparo("t");
        r.setRendimento("r");
        r.setCategoria(Categoria.DOCE);
        return r;
    }

    @Test
    void dtoVazioNaoAlteraNada() {
        Receitas r = base();
        mapper.updateEntityFromDTO(new ReceitasUpdateDTO(), r);
        assertThat(r.getNome()).isEqualTo("n");
        assertThat(r.getDescricao()).isEqualTo("d");
        assertThat(r.getModoPreparo()).isEqualTo("m");
        assertThat(r.getTempoPreparo()).isEqualTo("t");
        assertThat(r.getRendimento()).isEqualTo("r");
        assertThat(r.getCategoria()).isEqualTo(Categoria.DOCE);
    }

    @Test
    void dtoCompletoAlteraTodosOsCampos() {
        Receitas r = base();
        ReceitasUpdateDTO dto = new ReceitasUpdateDTO();
        dto.setNome("n2");
        dto.setDescricao("d2");
        dto.setModoPreparo("m2");
        dto.setTempoPreparo("t2");
        dto.setRendimento("r2");
        dto.setCategoria(Categoria.SALGADO);

        mapper.updateEntityFromDTO(dto, r);

        assertThat(r.getNome()).isEqualTo("n2");
        assertThat(r.getDescricao()).isEqualTo("d2");
        assertThat(r.getModoPreparo()).isEqualTo("m2");
        assertThat(r.getTempoPreparo()).isEqualTo("t2");
        assertThat(r.getRendimento()).isEqualTo("r2");
        assertThat(r.getCategoria()).isEqualTo(Categoria.SALGADO);
    }
}
