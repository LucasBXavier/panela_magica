package com.panelamagica.panelamagica.domain.entites;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Refresh token opaco. Só o hash (SHA-256) é armazenado; o valor em claro existe apenas na resposta ao cliente. */
@Getter
@Setter
@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_tokens_usuario", columnList = "usuario_id"))
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Preenchido quando o token é rotacionado ou revogado (logout). */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}
