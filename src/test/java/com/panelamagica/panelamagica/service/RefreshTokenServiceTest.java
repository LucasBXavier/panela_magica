package com.panelamagica.panelamagica.service;

import com.panelamagica.panelamagica.domain.entites.RefreshToken;
import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.exception.UnauthorizedException;
import com.panelamagica.panelamagica.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Repositório em memória (via Mockito) para testar rotação, reutilização e expiração sem banco. */
class RefreshTokenServiceTest {

    private final List<RefreshToken> banco = new ArrayList<>();
    private RefreshTokenRepository repository;
    private RefreshTokenService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());

        repository = mock(RefreshTokenRepository.class);
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> {
            RefreshToken t = inv.getArgument(0);
            banco.add(t);
            return t;
        });
        when(repository.findByTokenHash(anyString())).thenAnswer(inv ->
                banco.stream().filter(t -> t.getTokenHash().equals(inv.getArgument(0))).findFirst());
        when(repository.findByUsuarioIdAndRevokedAtIsNull(any(UUID.class))).thenAnswer(inv ->
                banco.stream().filter(t -> t.getRevokedAt() == null).toList());

        service = new RefreshTokenService(repository, 7);
    }

    @Test
    void emitirGuardaApenasOHash() {
        String valor = service.emitir(usuario);

        assertEquals(1, banco.size());
        assertNotEquals(valor, banco.get(0).getTokenHash());
        assertEquals(64, banco.get(0).getTokenHash().length());
        assertTrue(valor.length() >= 43);
        assertTrue(banco.get(0).getExpiresAt().isAfter(Instant.now().plus(6, ChronoUnit.DAYS)));
    }

    @Test
    void valoresEmitidosSaoDistintos() {
        assertNotEquals(service.emitir(usuario), service.emitir(usuario));
    }

    @Test
    void rotacionarTrocaOTokenEInvalidaOAntigo() {
        String original = service.emitir(usuario);

        var rotacao = service.rotacionar(original);

        assertNotEquals(original, rotacao.novoRefreshToken());
        assertEquals(usuario, rotacao.usuario());
        assertEquals(2, banco.size());
        assertNotNull(banco.get(0).getRevokedAt(), "o token usado deve ser revogado");
        assertNull(banco.get(1).getRevokedAt());
    }

    @Test
    void reutilizarTokenJaRotacionadoRevogaTodasAsSessoes() {
        String original = service.emitir(usuario);
        var rotacao = service.rotacionar(original);

        assertThrows(UnauthorizedException.class, () -> service.rotacionar(original));

        assertTrue(banco.stream().allMatch(t -> t.getRevokedAt() != null),
                "o token novo (do possível atacante ou do dono) também deve ser revogado");
        assertThrows(UnauthorizedException.class, () -> service.rotacionar(rotacao.novoRefreshToken()));
    }

    @Test
    void tokenDesconhecidoEhRecusado() {
        assertThrows(UnauthorizedException.class, () -> service.rotacionar("qualquer-coisa"));
    }

    @Test
    void tokenExpiradoEhRecusado() {
        String valor = service.emitir(usuario);
        banco.get(0).setExpiresAt(Instant.now().minusSeconds(1));

        assertThrows(UnauthorizedException.class, () -> service.rotacionar(valor));
        assertNull(banco.get(0).getRevokedAt());
    }

    @Test
    void logoutRevogaEEhIdempotente() {
        String valor = service.emitir(usuario);

        service.revogar(valor);
        Instant primeiraRevogacao = banco.get(0).getRevokedAt();
        service.revogar(valor);
        service.revogar("desconhecido");

        assertNotNull(primeiraRevogacao);
        assertEquals(primeiraRevogacao, banco.get(0).getRevokedAt());
        assertThrows(UnauthorizedException.class, () -> service.rotacionar(valor));
    }
}
