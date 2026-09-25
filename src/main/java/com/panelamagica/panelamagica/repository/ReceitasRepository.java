package com.panelamagica.panelamagica.repository;

import com.panelamagica.panelamagica.domain.entites.Receitas;
import com.panelamagica.panelamagica.domain.entites.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReceitasRepository extends JpaRepository<Receitas, UUID> {

    boolean existsByIdAndUsuarioId(UUID id, UUID usuarioId);

    boolean existsByNomeAndUsuarioId(String nome, UUID usuarioId);

    List<Receitas> findAllByUsuario(Usuario usuario);
}
