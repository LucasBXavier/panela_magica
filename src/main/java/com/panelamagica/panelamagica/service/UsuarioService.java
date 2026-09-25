package com.panelamagica.panelamagica.service;

import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.dto.login.LoginRequestDTO;
import com.panelamagica.panelamagica.dto.login.LoginResponseDTO;
import com.panelamagica.panelamagica.exception.BusinessRuleException;
import com.panelamagica.panelamagica.security.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import com.panelamagica.panelamagica.dto.user.UsuarioRequestDTO;
import com.panelamagica.panelamagica.dto.user.UsuarioResponseDTO;
import com.panelamagica.panelamagica.mapper.UsuarioMapper;
import com.panelamagica.panelamagica.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
            throw new BusinessRuleException("Já existe um usuário cadastrado com este e-mail");
        }

        Usuario usuario = usuarioMapper.toEntity(dto);
        usuarioRepository.save(usuario);
        return usuarioMapper.toResponseDTO(usuario);
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail().trim().toLowerCase(), dto.getSenha()));

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado"));

        return LoginResponseDTO.builder()
                .token(tokenService.gerarToken(authentication))
                .type("Bearer")
                .expiresIn(tokenService.getExpiresInSeconds())
                .usuario(usuarioMapper.toResponseDTO(usuario))
                .build();
    }

}
