package com.panelamagica.panelamagica.security;

import com.panelamagica.panelamagica.config.JwtConfig;
import com.panelamagica.panelamagica.domain.entites.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres!";

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setEmail("maria@email.com");
        return u;
    }

    @Test
    void subjectEhOIdDoUsuarioENaoOEmail() {
        JwtConfig config = new JwtConfig(SEGREDO, "panela-magica");
        TokenService tokens = new TokenService(config.jwtEncoder(), 60, "panela-magica");
        Usuario u = usuario();

        Jwt jwt = config.jwtDecoder().decode(tokens.gerarToken(u));

        assertEquals(u.getId().toString(), jwt.getSubject());
        assertEquals("panela-magica", jwt.getClaimAsString("iss"));
        assertEquals("USER", jwt.getClaimAsString("scope"));
        assertFalse(jwt.getClaims().containsValue("maria@email.com"), "e-mail não deve ir no token");
    }

    @Test
    void decoderRejeitaTokenDeOutroEmissor() {
        JwtConfig config = new JwtConfig(SEGREDO, "panela-magica");
        JwtEncoder encoder = config.jwtEncoder();
        String tokenDeOutroEmissor = new TokenService(encoder, 60, "outro-emissor").gerarToken(usuario());

        JwtDecoder decoder = config.jwtDecoder();
        JwtValidationException ex = assertThrows(JwtValidationException.class, () -> decoder.decode(tokenDeOutroEmissor));
        assertTrue(ex.getMessage().contains("iss"), "a falha deve ser por causa do emissor: " + ex.getMessage());
    }

    @Test
    void decoderRejeitaTokenAssinadoComOutroSegredo() {
        JwtConfig atacante = new JwtConfig("outro-segredo-de-teste-com-mais-de-32-caracteres", "panela-magica");
        String forjado = new TokenService(atacante.jwtEncoder(), 60, "panela-magica").gerarToken(usuario());

        assertThrows(Exception.class, () -> new JwtConfig(SEGREDO, "panela-magica").jwtDecoder().decode(forjado));
    }

    @Test
    void decoderRejeitaTokenExpirado() {
        JwtConfig config = new JwtConfig(SEGREDO, "panela-magica");
        Instant umaHoraAtras = Instant.now().minusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("panela-magica")
                .subject(UUID.randomUUID().toString())
                .issuedAt(umaHoraAtras.minusSeconds(3600))
                .expiresAt(umaHoraAtras)
                .build();
        String expirado = config.jwtEncoder()
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThrows(JwtValidationException.class, () -> config.jwtDecoder().decode(expirado));
    }

    @Test
    void segredoCurtoEhRecusadoNoStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtConfig("curto", "panela-magica"));
    }
}
