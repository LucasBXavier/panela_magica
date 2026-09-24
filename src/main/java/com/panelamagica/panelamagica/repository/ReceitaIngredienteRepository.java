package com.panelamagica.panelamagica.repository;

import com.panelamagica.panelamagica.domain.entites.ReceitaIngrediente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReceitaIngredienteRepository extends JpaRepository<ReceitaIngrediente, UUID> {

    List<ReceitaIngrediente> findByReceitaId(UUID receitaId);
}
