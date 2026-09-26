package com.panelamagica.panelamagica.domain.entites;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "ingredientes")
@Entity
public class Ingrediente extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String nome;
}
