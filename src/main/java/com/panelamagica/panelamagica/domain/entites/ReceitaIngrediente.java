package com.panelamagica.panelamagica.domain.entites;

import com.panelamagica.panelamagica.domain.enums.UnidadeMedida;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Table(name = "receita_ingrediente")
@Entity
public class ReceitaIngrediente extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receita_id", nullable = false)
    private Receitas receita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    @Column(nullable = false)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "unidade_medida")
    private UnidadeMedida unidadeMedida;
}
