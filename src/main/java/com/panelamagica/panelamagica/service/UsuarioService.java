package com.panelamagica.panelamagica.service;

import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.dto.login.LoginRequestDTO;
import com.panelamagica.panelamagica.dto.login.LoginResponseDTO;
import com.panelamagica.panelamagica.dto.login.RefreshTokenRequestDTO;
import com.panelamagica.panelamagica.dto.user.UsuarioRequestDTO;
import com.panelamagica.panelamagica.dto.user.UsuarioResponseDTO;
import com.panelamagica.panelamagica.exception.BusinessRuleException;
import com.panelamagica.panelamagica.exception.UnauthorizedException;
import com.panelamagica.panelamagica.mapper.UsuarioMapper;
import com.panelamagica.panelamagica.repository.UsuarioRepository;
import com.panelamagica.panelamagica.security.LoginRateLimiter;
import com.panelamagica.panelamagica.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;

    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
            throw new BusinessRuleException("Já existe um usuário cadastrado com este e-mail");
        }

        Usuario usuario = usuarioMapper.toEntity(dto);
        usuarioRepository.save(usuario);
        return usuarioMapper.toResponseDTO(usuario);
    }

    /**
     * @param ip endereço do cliente, usado apenas para limitar tentativas malsucedidas.
     */
    public LoginResponseDTO login(LoginRequestDTO dto, String ip) {
        String email = dto.getEmail().trim().toLowerCase();
        loginRateLimiter.verificar(email, ip);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, dto.getSenha()));
        } catch (AuthenticationException e) {
            loginRateLimiter.registrarFalha(email, ip);
            throw e;
        }
        loginRateLimiter.registrarSucesso(email);

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado"));

        return montarResposta(usuario, refreshTokenService.emitir(usuario));
    }

    /** Troca um refresh token válido por um novo par (access + refresh); o token usado deixa de valer. */
    public LoginResponseDTO refresh(RefreshTokenRequestDTO dto) {
        var rotacao = refreshTokenService.rotacionar(dto.getRefreshToken());
        return montarResposta(rotacao.usuario(), rotacao.novoRefreshToken());
    }

    /** Revoga o refresh token informado (idempotente). O access token já emitido vale até expirar. */
    public void logout(RefreshTokenRequestDTO dto) {
        refreshTokenService.revogar(dto.getRefreshToken());
    }

    private LoginResponseDTO montarResposta(Usuario usuario, String refreshToken) {
        return LoginResponseDTO.builder()
                .token(tokenService.gerarToken(usuario))
                .type("Bearer")
                .expiresIn(tokenService.getExpiresInSeconds())
                .refreshToken(refreshToken)
                .refreshExpiresIn(refreshTokenService.getExpiresInSeconds())
                .usuario(usuarioMapper.toResponseDTO(usuario))
                .build();
    }
}
