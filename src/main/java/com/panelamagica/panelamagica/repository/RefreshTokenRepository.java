package com.panelamagica.panelamagica.repository;

import com.panelamagica.panelamagica.domain.entites.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Lock pessimista: impede que duas requisições rotacionem o mesmo token ao mesmo tempo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUsuarioIdAndRevokedAtIsNull(UUID usuarioId);

    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :limite")
    int apagarExpiradosAntesDe(@Param("limite") Instant limite);
}
