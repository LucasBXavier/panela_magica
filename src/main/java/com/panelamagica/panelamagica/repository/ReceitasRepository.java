package com.panelamagica.panelamagica.repository;

import com.panelamagica.panelamagica.domain.entites.Receitas;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReceitasRepository extends JpaRepository<Receitas, UUID> {
}
