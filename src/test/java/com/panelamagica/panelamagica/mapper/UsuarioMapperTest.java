package com.panelamagica.panelamagica.mapper;

import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.dto.user.UsuarioRequestDTO;
import com.panelamagica.panelamagica.dto.user.UsuarioResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioMapperTest {

    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UsuarioMapper mapper = new UsuarioMapper(encoder);

    @Test
    void toEntityNormalizaEmailECodificaSenha() {
        when(encoder.encode("Senha@123")).thenReturn("hash");

        Usuario u = mapper.toEntity(new UsuarioRequestDTO("Ana", "  ANA@Mail.com ", "Senha@123"));

        assertThat(u.getNome()).isEqualTo("Ana");
        assertThat(u.getEmail()).isEqualTo("ana@mail.com");
        assertThat(u.getSenha()).isEqualTo("hash");
    }

    @Test
    void toResponseDtoCopiaCampos() {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setNome("Ana");
        u.setEmail("ana@mail.com");
        u.setCreatedAt(OffsetDateTime.now());

        UsuarioResponseDTO dto = mapper.toResponseDTO(u);

        assertThat(dto.getId()).isEqualTo(u.getId());
        assertThat(dto.getNome()).isEqualTo("Ana");
        assertThat(dto.getEmail()).isEqualTo("ana@mail.com");
        assertThat(dto.getDataCriacao()).isEqualTo(u.getCreatedAt());
    }
}
