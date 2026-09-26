package com.panelamagica.panelamagica.security;

import com.panelamagica.panelamagica.domain.entites.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    public static final String SCOPE_PADRAO = "USER";

    private final JwtEncoder jwtEncoder;
    private final Duration expiration;
    private final String issuer;

    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${jwt.expiration-minutes}") long expirationMinutes,
                        @Value("${jwt.issuer:panela-magica}") String issuer) {
        this.jwtEncoder = jwtEncoder;
        this.expiration = Duration.ofMinutes(expirationMinutes);
        this.issuer = issuer;
    }

    /** O {@code sub} é o id (UUID) do usuário, que não muda mesmo que o e-mail seja alterado. */
    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(expiration))
                .claim("scope", SCOPE_PADRAO)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpiresInSeconds() {
        return expiration.toSeconds();
    }
}
