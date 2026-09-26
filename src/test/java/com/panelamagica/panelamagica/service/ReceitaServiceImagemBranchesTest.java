package com.panelamagica.panelamagica.service;

import com.panelamagica.panelamagica.domain.entites.Receitas;
import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.exception.BusinessRuleException;
import com.panelamagica.panelamagica.mapper.ReceitaMapper;
import com.panelamagica.panelamagica.repository.ReceitasRepository;
import com.panelamagica.panelamagica.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Cobre os ramos de detecção por magic number (arquivos truncados / cabeçalhos parcialmente corretos). */
class ReceitaServiceImagemBranchesTest {

    private ReceitaService service;
    private Receitas receita;

    @BeforeEach
    void setUp() {
        ReceitasRepository receitas = mock(ReceitasRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        Usuario dono = new Usuario();
        dono.setId(UUID.randomUUID());
        when(usuarios.findById(dono.getId())).thenReturn(Optional.of(dono));
        receita = new Receitas();
        receita.setId(UUID.randomUUID());
        receita.setUsuario(dono);
        when(receitas.findById(receita.getId())).thenReturn(Optional.of(receita));
        service = new ReceitaService(receitas, mock(ReceitaMapper.class), usuarios);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                dono.getId().toString(), null, AuthorityUtils.createAuthorityList("SCOPE_USER")));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void rejeita(byte... bytes) {
        var file = new MockMultipartFile("file", "f", "image/png", bytes);
        assertThrows(BusinessRuleException.class, () -> service.uploadImagemReceita(receita.getId(), file));
    }

    @Test
    void cabecalhosInvalidosOuTruncadosSaoRejeitados() {
        Stream.of(
                new byte[]{(byte) 0xFF},                                                     // curto demais
                new byte[]{(byte) 0xFF, (byte) 0xD8, 0x00},                                  // JPEG: 3º byte errado
                new byte[]{(byte) 0xFF, 0x00, (byte) 0xFF},                                  // JPEG: 2º byte errado
                new byte[]{0x00, (byte) 0xD8, (byte) 0xFF},                                  // JPEG: 1º byte errado
                new byte[]{(byte) 0x89, 'P', 'N', 'G'},                                      // PNG truncado
                new byte[]{(byte) 0x89, 'P', 'N', 'X', 0, 0, 0, 0},                          // PNG: 4º byte errado
                new byte[]{(byte) 0x89, 'P', 'X', 'G', 0, 0, 0, 0},                          // PNG: 3º byte errado
                new byte[]{(byte) 0x89, 'X', 'N', 'G', 0, 0, 0, 0},                          // PNG: 2º byte errado
                new byte[]{0x00, 'P', 'N', 'G', 0, 0, 0, 0},                                 // PNG: 1º byte errado
                new byte[]{'R', 'I', 'F', 'F'},                                              // WEBP truncado
                new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'X'},             // WEBP: 12º byte errado
                new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'X', 'P'},             // WEBP: 11º byte errado
                new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'X', 'B', 'P'},             // WEBP: 10º byte errado
                new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'X', 'E', 'B', 'P'},             // WEBP: 9º byte errado
                new byte[]{'R', 'I', 'F', 'X', 0, 0, 0, 0, 'W', 'E', 'B', 'P'},             // WEBP: 4º byte errado
                new byte[]{'R', 'I', 'X', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'},             // WEBP: 3º byte errado
                new byte[]{'R', 'X', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'},             // WEBP: 2º byte errado
                new byte[]{'X', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}              // WEBP: 1º byte errado
        ).forEach(this::rejeita);
    }
}
