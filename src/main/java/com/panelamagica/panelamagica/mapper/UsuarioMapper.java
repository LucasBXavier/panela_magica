package com.panelamagica.panelamagica.mapper;

import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.dto.user.UsuarioRequestDTO;
import com.panelamagica.panelamagica.dto.user.UsuarioResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsuarioMapper {

    private final PasswordEncoder passwordEncoder;

    public Usuario toEntity(UsuarioRequestDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        return usuario;
    }

    public UsuarioResponseDTO toResponseDTO(Usuario entity) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(String.valueOf(entity.getId()));
        dto.setNome(entity.getNome());
        dto.setEmail(entity.getEmail());
        dto.setDataCriacao(entity.getCreatedAt());
        return dto;
    }
}
