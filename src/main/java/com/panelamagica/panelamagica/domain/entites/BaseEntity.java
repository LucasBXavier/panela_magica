package com.panelamagica.panelamagica.domain.entites;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.util.UUID;

/**
 * Base das entidades: id UUID gerado e igualdade por identidade (id), segura para proxies lazy.
 * Entidades ainda não persistidas (id nulo) só são iguais a si mesmas.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity other) || getId() == null) {
            return false;
        }
        return Hibernate.getClass(this) == Hibernate.getClass(other) && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
