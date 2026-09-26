package com.panelamagica.panelamagica.security;

import com.panelamagica.panelamagica.domain.entites.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    @Test
    void gerarTokenMontaClaims() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        Jwt jwt = new Jwt("tok", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "HS256"), Map.of("sub", "x"));
        when(encoder.encode(any())).thenReturn(jwt);
        TokenService service = new TokenService(encoder, 30, "emissor");
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());

        assertThat(service.gerarToken(u)).isEqualTo("tok");

        ArgumentCaptor<JwtEncoderParameters> cap = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(cap.capture());
        var claims = cap.getValue().getClaims();
        assertThat(claims.getSubject()).isEqualTo(u.getId().toString());
        assertThat(claims.getClaimAsString("iss")).isEqualTo("emissor");
        assertThat((String) claims.getClaim("scope")).isEqualTo("USER");
        assertThat(claims.getExpiresAt()).isAfter(claims.getIssuedAt());
        assertThat(service.getExpiresInSeconds()).isEqualTo(1800);
    }
}
