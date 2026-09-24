package com.panelamagica.panelamagica.domain.entites;

import com.panelamagica.panelamagica.domain.enums.Categoria;
import com.panelamagica.panelamagica.domain.enums.UnidadeMedida;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Table(name = "receitas")
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Receitas {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, name = "modo_preparo")
    private String modoPreparo;

    @Column(nullable = false, name = "tempo_preparo")
    private String tempoPreparo;

    @Column(nullable = false)
    private String rendimento;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UnidadeMedida unidadeMedida;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
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
