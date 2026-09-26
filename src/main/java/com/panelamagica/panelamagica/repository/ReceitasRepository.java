package com.panelamagica.panelamagica.repository;

import com.panelamagica.panelamagica.domain.entites.Receitas;
import com.panelamagica.panelamagica.domain.entites.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceitasRepository extends JpaRepository<Receitas, UUID> {

    boolean existsByNomeAndUsuarioId(String nome, UUID usuarioId);

    @EntityGraph(attributePaths = {"ingredientes", "ingredientes.ingrediente"})
    List<Receitas> findAllByUsuario(Usuario usuario);

    @EntityGraph(attributePaths = {"ingredientes", "ingredientes.ingrediente"})
    Optional<Receitas> findById(UUID id);

    @EntityGraph(attributePaths = {"ingredientes", "ingredientes.ingrediente"})
    List<Receitas> findAll();
}
