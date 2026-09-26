package com.panelamagica.panelamagica.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-32-caracteres!";

    @Test
    void segredoCurtoRejeitado() {
        assertThatThrownBy(() -> new JwtConfig("curto", "iss")).isInstanceOf(IllegalStateException.class);
    }

    private String token(JwtConfig cfg, String issuer) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject("u")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        return cfg.jwtEncoder().encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void encoderEDecoderFazemRoundTrip() {
        JwtConfig cfg = new JwtConfig(SECRET, "iss");
        Jwt jwt = cfg.jwtDecoder().decode(token(cfg, "iss"));
        assertThat(jwt.getSubject()).isEqualTo("u");
    }

    @Test
    void decoderRejeitaEmissorDiferente() {
        JwtConfig cfg = new JwtConfig(SECRET, "iss");
        String t = token(cfg, "outro");
        JwtDecoder decoder = cfg.jwtDecoder();
        assertThatThrownBy(() -> decoder.decode(t)).isInstanceOf(JwtException.class);
    }
}
