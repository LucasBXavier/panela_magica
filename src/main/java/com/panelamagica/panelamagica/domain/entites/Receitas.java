package com.panelamagica.panelamagica.domain.entites;

import com.panelamagica.panelamagica.domain.enums.Categoria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Table(name = "receitas")
@Entity
public class Receitas extends BaseEntity {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, name = "modo_preparo", columnDefinition = "TEXT")
    private String modoPreparo;

    @Column(nullable = false, name = "tempo_preparo")
    private String tempoPreparo;

    @Column(nullable = false)
    private String rendimento;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "imagem_id")
    private ReceitaImagem imagem;

    @OneToMany(mappedBy = "receita", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReceitaIngrediente> ingredientes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void addIngrediente(ReceitaIngrediente receitaIngrediente) {
        receitaIngrediente.setReceita(this);
        this.ingredientes.add(receitaIngrediente);
    }

    public void removeIngrediente(ReceitaIngrediente receitaIngrediente) {
        this.ingredientes.remove(receitaIngrediente);
        receitaIngrediente.setReceita(null);
    }
}
