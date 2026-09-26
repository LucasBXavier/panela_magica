package com.panelamagica.panelamagica.domain;

import com.panelamagica.panelamagica.domain.entites.*;
import com.panelamagica.panelamagica.domain.enums.Categoria;
import com.panelamagica.panelamagica.domain.enums.UnidadeMedida;
import com.panelamagica.panelamagica.dto.SuccessResponseDTO;
import com.panelamagica.panelamagica.exception.BusinessRuleException;
import com.panelamagica.panelamagica.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DominioTest {

    @Test
    void categoriaFrom() {
        assertThat(Categoria.from(null)).isNull();
        assertThat(Categoria.from(" doce ")).isEqualTo(Categoria.DOCE);
        assertThatThrownBy(() -> Categoria.from("xyz")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void unidadeMedidaFrom() {
        UnidadeMedida u = UnidadeMedida.values()[0];
        assertThat(UnidadeMedida.from(null)).isNull();
        assertThat(UnidadeMedida.from(" " + u.name().toLowerCase() + " ")).isEqualTo(u);
        assertThatThrownBy(() -> UnidadeMedida.from("xyz")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void baseEntityEquals() {
        Ingrediente a = new Ingrediente();
        Ingrediente b = new Ingrediente();
        assertThat(a.equals(a)).isTrue();
        assertThat(a.equals(b)).isFalse();
        assertThat(a.equals("x")).isFalse();
        UUID id = UUID.randomUUID();
        a.setId(id);
        b.setId(id);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        Usuario outroTipo = new Usuario();
        outroTipo.setId(id);
        assertThat(a.equals(outroTipo)).isFalse();
    }

    @Test
    void receitasIngredientesEOnCreate() {
        Receitas r = new Receitas();
        ReceitaIngrediente ri = new ReceitaIngrediente();
        r.addIngrediente(ri);
        assertThat(r.getIngredientes()).containsExactly(ri);
        assertThat(ri.getReceita()).isSameAs(r);
        r.removeIngrediente(ri);
        assertThat(r.getIngredientes()).isEmpty();
        assertThat(ri.getReceita()).isNull();
        r.onCreate();
        assertThat(r.getCreatedAt()).isNotNull();
    }

    @Test
    void usuarioToStringSemSenha() {
        Usuario u = new Usuario();
        u.setEmail("a@b.com");
        u.setSenha("segredo");
        u.onCreate();
        assertThat(u.getCreatedAt()).isNotNull();
        assertThat(u.toString()).doesNotContain("segredo");
    }

    @Test
    void dtosEExcecao() {
        SuccessResponseDTO<String> s = new SuccessResponseDTO<>(200, "ok", "d");
        assertThat(s.getTimestamp()).isNotNull();
        assertThat(new SuccessResponseDTO<>(201, "ok").getData()).isNull();
        assertThat(new TooManyRequestsException("m", 5).getRetryAfterSeconds()).isEqualTo(5);
    }
}
