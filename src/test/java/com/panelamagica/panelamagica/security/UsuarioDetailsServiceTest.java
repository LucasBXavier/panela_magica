package com.panelamagica.panelamagica.security;

import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioDetailsServiceTest {

    private final UsuarioRepository repo = mock(UsuarioRepository.class);
    private final UsuarioDetailsService service = new UsuarioDetailsService(repo);

    @Test
    void carregaUsuarioExistente() {
        Usuario u = new Usuario();
        u.setEmail("a@b.com");
        u.setSenha("hash");
        when(repo.findByEmail("a@b.com")).thenReturn(Optional.of(u));

        UserDetails d = service.loadUserByUsername("a@b.com");

        assertThat(d.getUsername()).isEqualTo("a@b.com");
        assertThat(d.getPassword()).isEqualTo("hash");
        assertThat(d.getAuthorities()).extracting(Object::toString).containsExactly("USER");
    }

    @Test
    void usuarioInexistenteLancaExcecao() {
        when(repo.findByEmail("x@y.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("x@y.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
