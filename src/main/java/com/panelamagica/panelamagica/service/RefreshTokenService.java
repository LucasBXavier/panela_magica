package com.panelamagica.panelamagica.service;

import com.panelamagica.panelamagica.domain.entites.RefreshToken;
import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.exception.UnauthorizedException;
import com.panelamagica.panelamagica.repository.RefreshTokenRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Emite, rotaciona e revoga refresh tokens. A cada uso o token é trocado por um novo (rotação); se um token
 * já usado for apresentado de novo, presume-se vazamento e todas as sessões do usuário são revogadas.
 */
@Service
public class RefreshTokenService {

    public record Rotacao(Usuario usuario, String novoRefreshToken) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final Duration validade;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${jwt.refresh-expiration-days:7}") long validadeDias) {
        this.repository = repository;
        this.validade = Duration.ofDays(validadeDias);
    }

    public long getExpiresInSeconds() {
        return validade.toSeconds();
    }

    /** Devolve o valor em claro do novo refresh token (só o hash fica no banco). */
    @Transactional
    public String emitir(Usuario usuario) {
        Instant agora = Instant.now();
        repository.apagarExpiradosAntesDe(agora.minus(1, ChronoUnit.DAYS));

        byte[] aleatorio = new byte[32];
        RANDOM.nextBytes(aleatorio);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);

        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setTokenHash(hash(valor));
        token.setExpiresAt(agora.plus(validade));
        repository.save(token);
        return valor;
    }

    /**
     * Troca um refresh token válido por um novo. {@code noRollbackFor}: a revogação por reutilização precisa
     * ser gravada mesmo quando a requisição termina em 401.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Rotacao rotacionar(String valor) {
        RefreshToken token = repository.findByTokenHash(hash(valor))
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));
        Instant agora = Instant.now();

        if (token.getRevokedAt() != null) {
            revogarTodos(token.getUsuario().getId(), agora);
            throw new UnauthorizedException("Refresh token inválido");
        }
        if (!token.getExpiresAt().isAfter(agora)) {
            throw new UnauthorizedException("Refresh token expirado");
        }

        token.setRevokedAt(agora);
        Usuario usuario = token.getUsuario();
        Hibernate.initialize(usuario); // o chamador usa o usuário depois que a transação termina
        return new Rotacao(usuario, emitir(usuario));
    }

    /** Logout: revoga o token informado. Idempotente: token desconhecido ou já revogado não gera erro. */
    @Transactional
    public void revogar(String valor) {
        repository.findByTokenHash(hash(valor)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
            }
        });
    }

    private void revogarTodos(UUID usuarioId, Instant agora) {
        repository.findByUsuarioIdAndRevokedAtIsNull(usuarioId).forEach(t -> t.setRevokedAt(agora));
    }

    private String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
