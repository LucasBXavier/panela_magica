package com.panelamagica.panelamagica.service;

import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.dto.login.LoginRequestDTO;
import com.panelamagica.panelamagica.dto.login.LoginResponseDTO;
import com.panelamagica.panelamagica.dto.login.RefreshTokenRequestDTO;
import com.panelamagica.panelamagica.dto.user.UsuarioRequestDTO;
import com.panelamagica.panelamagica.dto.user.UsuarioResponseDTO;
import com.panelamagica.panelamagica.exception.BusinessRuleException;
import com.panelamagica.panelamagica.exception.TooManyRequestsException;
import com.panelamagica.panelamagica.exception.UnauthorizedException;
import com.panelamagica.panelamagica.mapper.UsuarioMapper;
import com.panelamagica.panelamagica.repository.UsuarioRepository;
import com.panelamagica.panelamagica.security.LoginRateLimiter;
import com.panelamagica.panelamagica.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioMapper usuarioMapper;
    @Mock AuthenticationManager authenticationManager;
    @Mock TokenService tokenService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock LoginRateLimiter loginRateLimiter;
    @InjectMocks UsuarioService service;

    private Usuario usuario;
    private UsuarioResponseDTO usuarioDto;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("a@b.com");
        usuarioDto = new UsuarioResponseDTO();
    }

    private LoginRequestDTO login(String email) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(email);
        dto.setSenha("Senha@123");
        return dto;
    }

    @Test
    void cadastrarSalvaEDevolveDto() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Ana", "  A@B.com ", "Senha@123");
        when(usuarioRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(usuarioMapper.toEntity(dto)).thenReturn(usuario);
        when(usuarioMapper.toResponseDTO(usuario)).thenReturn(usuarioDto);

        assertThat(service.cadastrar(dto)).isSameAs(usuarioDto);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cadastrarEmailDuplicadoLancaRegraDeNegocio() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Ana", "a@b.com", "Senha@123");
        when(usuarioRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(dto)).isInstanceOf(BusinessRuleException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void loginComSucessoDevolveTokens() {
        var auth = new UsernamePasswordAuthenticationToken("a@b.com", "x");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.of(usuario));
        when(refreshTokenService.emitir(usuario)).thenReturn("refresh");
        when(refreshTokenService.getExpiresInSeconds()).thenReturn(600L);
        when(tokenService.gerarToken(usuario)).thenReturn("jwt");
        when(tokenService.getExpiresInSeconds()).thenReturn(60L);
        when(usuarioMapper.toResponseDTO(usuario)).thenReturn(usuarioDto);

        LoginResponseDTO r = service.login(login(" A@B.com "), "1.1.1.1");

        assertThat(r.getToken()).isEqualTo("jwt");
        assertThat(r.getType()).isEqualTo("Bearer");
        assertThat(r.getExpiresIn()).isEqualTo(60L);
        assertThat(r.getRefreshToken()).isEqualTo("refresh");
        assertThat(r.getRefreshExpiresIn()).isEqualTo(600L);
        assertThat(r.getUsuario()).isSameAs(usuarioDto);
        verify(loginRateLimiter).verificar("a@b.com", "1.1.1.1");
        verify(loginRateLimiter).registrarSucesso("a@b.com");
    }

    @Test
    void loginComFalhaRegistraTentativaERelanca() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

        assertThatThrownBy(() -> service.login(login("a@b.com"), "ip"))
                .isInstanceOf(BadCredentialsException.class);
        verify(loginRateLimiter).registrarFalha("a@b.com", "ip");
        verify(loginRateLimiter, never()).registrarSucesso(anyString());
    }

    @Test
    void loginBloqueadoPeloLimiterNaoAutentica() {
        doThrow(new TooManyRequestsException("muitas", 10)).when(loginRateLimiter).verificar("a@b.com", "ip");

        assertThatThrownBy(() -> service.login(login("a@b.com"), "ip"))
                .isInstanceOf(TooManyRequestsException.class);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void loginUsuarioAutenticadoInexistenteLancaUnauthorized() {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("a@b.com", "x"));
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(login("a@b.com"), "ip"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshRotacionaTokens() {
        RefreshTokenRequestDTO dto = new RefreshTokenRequestDTO();
        dto.setRefreshToken("velho");
        when(refreshTokenService.rotacionar("velho"))
                .thenReturn(new RefreshTokenService.Rotacao(usuario, "novo"));
        when(tokenService.gerarToken(usuario)).thenReturn("jwt");
        when(usuarioMapper.toResponseDTO(usuario)).thenReturn(usuarioDto);

        LoginResponseDTO r = service.refresh(dto);

        assertThat(r.getRefreshToken()).isEqualTo("novo");
        assertThat(r.getToken()).isEqualTo("jwt");
    }

    @Test
    void logoutRevogaToken() {
        RefreshTokenRequestDTO dto = new RefreshTokenRequestDTO();
        dto.setRefreshToken("t");

        service.logout(dto);

        verify(refreshTokenService).revogar("t");
    }
}
