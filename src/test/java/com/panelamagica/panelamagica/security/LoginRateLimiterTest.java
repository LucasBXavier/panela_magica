package com.panelamagica.panelamagica.security;

import com.panelamagica.panelamagica.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {

    /** Relógio controlável para avançar o tempo sem esperar. */
    static class RelogioMutavel extends Clock {
        Instant agora = Instant.parse("2026-01-01T00:00:00Z");

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
        void avancar(Duration d) { agora = agora.plus(d); }
    }

    private RelogioMutavel relogio;
    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        relogio = new RelogioMutavel();
        limiter = new LoginRateLimiter(3, 5, Duration.ofMinutes(15), relogio);
    }

    @Test
    void bloqueiaOEmailAposOLimiteDeFalhas() {
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> limiter.verificar("a@x.com", "1.1.1.1"));
            limiter.registrarFalha("a@x.com", "1.1.1.1");
        }
        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> limiter.verificar("a@x.com", "9.9.9.9"));
        assertTrue(ex.getRetryAfterSeconds() > 0 && ex.getRetryAfterSeconds() <= 900);
    }

    @Test
    void emailBloqueadoNaoAfetaOutroEmailNemOutroIp() {
        for (int i = 0; i < 3; i++) {
            limiter.registrarFalha("a@x.com", "1.1.1.1");
        }
        assertDoesNotThrow(() -> limiter.verificar("b@x.com", "2.2.2.2"));
    }

    @Test
    void emailNaoDiferenciaMaiusculasEEspacos() {
        for (int i = 0; i < 3; i++) {
            limiter.registrarFalha("  A@X.com ", "1.1.1.1");
        }
        assertThrows(TooManyRequestsException.class, () -> limiter.verificar("a@x.com", "3.3.3.3"));
    }

    @Test
    void bloqueiaOIpQuandoVariosEmailsFalhamDeUmMesmoLugar() {
        for (int i = 0; i < 5; i++) {
            limiter.registrarFalha("user" + i + "@x.com", "7.7.7.7");
        }
        assertThrows(TooManyRequestsException.class, () -> limiter.verificar("novo@x.com", "7.7.7.7"));
        assertDoesNotThrow(() -> limiter.verificar("novo@x.com", "8.8.8.8"));
    }

    @Test
    void liberaAposAJanelaExpirar() {
        for (int i = 0; i < 3; i++) {
            limiter.registrarFalha("a@x.com", "1.1.1.1");
        }
        assertThrows(TooManyRequestsException.class, () -> limiter.verificar("a@x.com", "1.1.1.1"));
        relogio.avancar(Duration.ofMinutes(15));
        assertDoesNotThrow(() -> limiter.verificar("a@x.com", "1.1.1.1"));
    }

    @Test
    void novaFalhaAposJanelaExpiradaReiniciaAContagem() {
        for (int i = 0; i < 3; i++) {
            limiter.registrarFalha("a@x.com", "1.1.1.1");
        }
        relogio.avancar(Duration.ofMinutes(16));
        limiter.registrarFalha("a@x.com", "1.1.1.1");
        assertDoesNotThrow(() -> limiter.verificar("a@x.com", "1.1.1.1"));
    }

    @Test
    void sucessoZeraOContadorDoEmail() {
        limiter.registrarFalha("a@x.com", "1.1.1.1");
        limiter.registrarFalha("a@x.com", "1.1.1.1");
        limiter.registrarSucesso("a@x.com");
        limiter.registrarFalha("a@x.com", "1.1.1.1");
        limiter.registrarFalha("a@x.com", "1.1.1.1");
        assertDoesNotThrow(() -> limiter.verificar("a@x.com", "9.9.9.9"));
    }
}
