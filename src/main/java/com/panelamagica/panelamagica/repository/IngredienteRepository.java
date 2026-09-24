package com.panelamagica.panelamagica.repository;

import com.panelamagica.panelamagica.domain.entites.Ingrediente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IngredienteRepository extends JpaRepository<Ingrediente, UUID> {

    Optional<Ingrediente> findByNomeIgnoreCase(String nome);
}
