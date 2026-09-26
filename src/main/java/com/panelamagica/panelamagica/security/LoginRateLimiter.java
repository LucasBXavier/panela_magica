package com.panelamagica.panelamagica.security;

import com.panelamagica.panelamagica.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas de login malsucedidas (força bruta) por e-mail e por IP, em janela fixa.
 * <p>
 * Estado em memória: vale para uma única instância da aplicação e é zerado no restart. Com várias
 * instâncias, mover o contador para um armazenamento compartilhado (ex.: Redis) ou usar um gateway.
 * Atrás de proxy reverso, configure {@code server.forward-headers-strategy} para que o IP seja o do cliente.
 */
@Component
public class LoginRateLimiter {

    private static final int LIMITE_ENTRADAS_PARA_LIMPEZA = 10_000;

    private record Contador(int falhas, Instant inicioJanela) {
    }

    private final Map<String, Contador> contadores = new ConcurrentHashMap<>();
    private final int maxPorEmail;
    private final int maxPorIp;
    private final Duration janela;
    private final Clock clock;

    @Autowired
    public LoginRateLimiter(@Value("${auth.login.max-attempts:5}") int maxPorEmail,
                            @Value("${auth.login.max-attempts-per-ip:20}") int maxPorIp,
                            @Value("${auth.login.window-minutes:15}") long janelaMinutos) {
        this(maxPorEmail, maxPorIp, Duration.ofMinutes(janelaMinutos), Clock.systemUTC());
    }

    LoginRateLimiter(int maxPorEmail, int maxPorIp, Duration janela, Clock clock) {
        this.maxPorEmail = maxPorEmail;
        this.maxPorIp = maxPorIp;
        this.janela = janela;
        this.clock = clock;
    }

    /** Lança {@link TooManyRequestsException} se o e-mail ou o IP já atingiram o limite na janela atual. */
    public void verificar(String email, String ip) {
        long esperaEmail = segundosParaLiberar(chaveEmail(email), maxPorEmail);
        long esperaIp = segundosParaLiberar(chaveIp(ip), maxPorIp);
        long espera = Math.max(esperaEmail, esperaIp);
        if (espera > 0) {
            throw new TooManyRequestsException(
                    "Muitas tentativas de login. Tente novamente em alguns minutos.", espera);
        }
    }

    public void registrarFalha(String email, String ip) {
        Instant agora = clock.instant();
        if (contadores.size() > LIMITE_ENTRADAS_PARA_LIMPEZA) {
            contadores.values().removeIf(c -> expirou(c, agora));
        }
        incrementar(chaveEmail(email), agora);
        incrementar(chaveIp(ip), agora);
    }

    /** Login bem-sucedido zera apenas o contador do e-mail; o do IP segue até a janela expirar. */
    public void registrarSucesso(String email) {
        contadores.remove(chaveEmail(email));
    }

    private void incrementar(String chave, Instant agora) {
        contadores.merge(chave, new Contador(1, agora), (atual, novo) ->
                expirou(atual, agora) ? novo : new Contador(atual.falhas() + 1, atual.inicioJanela()));
    }

    private long segundosParaLiberar(String chave, int limite) {
        Contador c = contadores.get(chave);
        Instant agora = clock.instant();
        if (c == null || expirou(c, agora) || c.falhas() < limite) {
            return 0;
        }
        return Math.max(1, Duration.between(agora, c.inicioJanela().plus(janela)).toSeconds());
    }

    private boolean expirou(Contador c, Instant agora) {
        return !agora.isBefore(c.inicioJanela().plus(janela));
    }

    private String chaveEmail(String email) {
        return "email:" + (email == null ? "" : email.trim().toLowerCase());
    }

    private String chaveIp(String ip) {
        return "ip:" + (ip == null ? "" : ip);
    }
}
