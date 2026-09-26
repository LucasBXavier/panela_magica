package com.panelamagica.panelamagica.service;

import com.panelamagica.panelamagica.domain.entites.ReceitaImagem;
import com.panelamagica.panelamagica.domain.entites.Receitas;
import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.domain.enums.Categoria;
import com.panelamagica.panelamagica.domain.enums.UnidadeMedida;
import com.panelamagica.panelamagica.dto.receitas.ReceitaIngredienteRequestDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaRequestDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitasUpdateDTO;
import com.panelamagica.panelamagica.exception.BusinessRuleException;
import com.panelamagica.panelamagica.exception.ForbiddenException;
import com.panelamagica.panelamagica.exception.ResourceNotFoundException;
import com.panelamagica.panelamagica.exception.UnauthorizedException;
import com.panelamagica.panelamagica.mapper.ReceitaMapper;
import com.panelamagica.panelamagica.repository.IngredienteRepository;
import com.panelamagica.panelamagica.repository.ReceitasRepository;
import com.panelamagica.panelamagica.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ReceitaServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    private ReceitasRepository receitasRepository;
    private UsuarioRepository usuarioRepository;
    private IngredienteRepository ingredienteRepository;
    private ReceitaService service;

    private Usuario dono;
    private Usuario outro;

    @BeforeEach
    void setUp() {
        receitasRepository = mock(ReceitasRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        ingredienteRepository = mock(IngredienteRepository.class);
        when(ingredienteRepository.findByNomeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(receitasRepository.save(any(Receitas.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new ReceitaService(receitasRepository, new ReceitaMapper(ingredienteRepository), usuarioRepository);

        dono = usuario("dono@x.com");
        outro = usuario("outro@x.com");
        autenticarComo(dono);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------

    private Usuario usuario(String email) {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setNome(email);
        when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        return u;
    }

    private void autenticarComo(Usuario u) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u.getId().toString(), null, AuthorityUtils.createAuthorityList("SCOPE_USER")));
    }

    private Receitas receitaDe(Usuario u) {
        Receitas r = new Receitas();
        r.setId(UUID.randomUUID());
        r.setNome("Bolo");
        r.setDescricao("d");
        r.setModoPreparo("m");
        r.setTempoPreparo("1h");
        r.setRendimento("8");
        r.setCategoria(Categoria.DOCE);
        r.setUsuario(u);
        when(receitasRepository.findById(r.getId())).thenReturn(Optional.of(r));
        return r;
    }

    private ReceitaRequestDTO requestDto(String nome) {
        ReceitaRequestDTO dto = new ReceitaRequestDTO();
        dto.setNome(nome);
        dto.setDescricao("d");
        dto.setModoPreparo("m");
        dto.setTempoPreparo("1h");
        dto.setRendimento("8");
        dto.setCategoria(Categoria.DOCE);
        dto.setIngredientes(List.of(new ReceitaIngredienteRequestDTO("Sal", BigDecimal.ONE, UnidadeMedida.COLHER_CHA)));
        return dto;
    }

    private MockMultipartFile arquivo(byte[] bytes) {
        return new MockMultipartFile("file", "f.bin", "application/octet-stream", bytes);
    }

    // ---------- autenticação ----------

    @Test
    void semAutenticacaoLancaUnauthorized() {
        SecurityContextHolder.clearContext();
        assertThrows(UnauthorizedException.class, () -> service.minhasReceitas());
    }

    @Test
    void usuarioAnonimoLancaUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "k", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThrows(UnauthorizedException.class, () -> service.minhasReceitas());
    }

    @Test
    void tokenAntigoComSubjectEmailLancaUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dono@x.com", null, AuthorityUtils.createAuthorityList("SCOPE_USER")));
        assertThrows(UnauthorizedException.class, () -> service.minhasReceitas());
    }

    @Test
    void usuarioInexistenteLancaUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(), null, AuthorityUtils.createAuthorityList("SCOPE_USER")));
        assertThrows(UnauthorizedException.class, () -> service.minhasReceitas());
    }

    // ---------- criar ----------

    @Test
    void criarAssociaAoUsuarioLogado() {
        when(receitasRepository.existsByNomeAndUsuarioId("Bolo", dono.getId())).thenReturn(false);

        var resposta = service.criarReceita(requestDto("Bolo"));

        assertEquals("Bolo", resposta.getNome());
        verify(receitasRepository).save(argThat(r -> r.getUsuario() == dono));
    }

    @Test
    void criarComNomeDuplicadoDoMesmoUsuarioEhRecusado() {
        when(receitasRepository.existsByNomeAndUsuarioId("Bolo", dono.getId())).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.criarReceita(requestDto("Bolo")));
        verify(receitasRepository, never()).save(any());
    }

    // ---------- atualizar ----------

    @Test
    void atualizarReceitaDeOutroUsuarioLancaForbidden() {
        Receitas alheia = receitaDe(outro);

        assertThrows(ForbiddenException.class, () -> service.atualizarReceita(alheia.getId(), new ReceitasUpdateDTO()));
        verify(receitasRepository, never()).save(any());
    }

    @Test
    void atualizarReceitaInexistenteLancaNotFound() {
        when(receitasRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.atualizarReceita(UUID.randomUUID(), new ReceitasUpdateDTO()));
    }

    @Test
    void atualizarAlteraSomenteOsCamposEnviados() {
        Receitas minha = receitaDe(dono);
        ReceitasUpdateDTO dto = new ReceitasUpdateDTO();
        dto.setRendimento("16");

        var resposta = service.atualizarReceita(minha.getId(), dto);

        assertEquals("16", resposta.getRendimento());
        assertEquals("Bolo", resposta.getNome());
    }

    @Test
    void renomearParaNomeJaUsadoPorOutraReceitaDoUsuarioEhRecusado() {
        Receitas minha = receitaDe(dono);
        when(receitasRepository.existsByNomeAndUsuarioId("Pudim", dono.getId())).thenReturn(true);
        ReceitasUpdateDTO dto = new ReceitasUpdateDTO();
        dto.setNome("Pudim");

        assertThrows(BusinessRuleException.class, () -> service.atualizarReceita(minha.getId(), dto));
    }

    @Test
    void manterOMesmoNomeNaoConsultaDuplicidade() {
        Receitas minha = receitaDe(dono);
        ReceitasUpdateDTO dto = new ReceitasUpdateDTO();
        dto.setNome("Bolo");

        assertDoesNotThrow(() -> service.atualizarReceita(minha.getId(), dto));
        verify(receitasRepository, never()).existsByNomeAndUsuarioId(anyString(), any());
    }

    // ---------- deletar ----------

    @Test
    void donoDeletaAReceita() {
        Receitas minha = receitaDe(dono);

        service.deletarReceita(minha.getId());

        verify(receitasRepository).delete(minha);
    }

    @Test
    void deletarReceitaDeOutroUsuarioLancaForbiddenSemApagar() {
        Receitas alheia = receitaDe(outro);

        assertThrows(ForbiddenException.class, () -> service.deletarReceita(alheia.getId()));
        verify(receitasRepository, never()).delete(any(Receitas.class));
        verify(receitasRepository, never()).deleteById(any());
    }

    @Test
    void deletarReceitaInexistenteLancaNotFound() {
        when(receitasRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deletarReceita(UUID.randomUUID()));
    }

    // ---------- imagem ----------

    @Test
    void uploadDeOutroUsuarioLancaForbidden() {
        Receitas alheia = receitaDe(outro);

        assertThrows(ForbiddenException.class, () -> service.uploadImagemReceita(alheia.getId(), arquivo(JPEG)));
    }

    @Test
    void uploadDetectaOTipoPeloConteudo() {
        Receitas minha = receitaDe(dono);

        service.uploadImagemReceita(minha.getId(), arquivo(JPEG));
        assertEquals("image/jpeg", minha.getImagem().getContentType());

        service.uploadImagemReceita(minha.getId(), arquivo(PNG));
        assertEquals("image/png", minha.getImagem().getContentType());

        service.uploadImagemReceita(minha.getId(), arquivo(WEBP));
        assertEquals("image/webp", minha.getImagem().getContentType());
    }

    @Test
    void uploadIgnoraOContentTypeInformadoPeloCliente() {
        Receitas minha = receitaDe(dono);
        var mentiroso = new MockMultipartFile("file", "x.png", "image/png", "isto nao e uma imagem".getBytes());

        assertThrows(BusinessRuleException.class, () -> service.uploadImagemReceita(minha.getId(), mentiroso));
        assertNull(minha.getImagem());
    }

    @Test
    void uploadSubstituiAImagemReaproveitandoAEntidade() {
        Receitas minha = receitaDe(dono);
        service.uploadImagemReceita(minha.getId(), arquivo(JPEG));
        ReceitaImagem primeira = minha.getImagem();

        service.uploadImagemReceita(minha.getId(), arquivo(PNG));

        assertSame(primeira, minha.getImagem());
        assertArrayEquals(PNG, minha.getImagem().getDados());
    }

    @Test
    void uploadDeArquivoVazioEhRecusado() {
        Receitas minha = receitaDe(dono);

        assertThrows(BusinessRuleException.class, () -> service.uploadImagemReceita(minha.getId(), arquivo(new byte[0])));
        assertThrows(BusinessRuleException.class, () -> service.uploadImagemReceita(minha.getId(), null));
    }

    @Test
    void uploadRetornaAUrlDaImagem() {
        Receitas minha = receitaDe(dono);

        assertEquals("/api/v1/receitas/" + minha.getId() + "/imagem",
                service.uploadImagemReceita(minha.getId(), arquivo(JPEG)));
    }

    @Test
    void obterImagemDevolveBytesEContentType() {
        Receitas minha = receitaDe(dono);
        service.uploadImagemReceita(minha.getId(), arquivo(PNG));

        ResponseEntity<byte[]> resposta = service.getImagemReceita(minha.getId());

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("image/png", resposta.getHeaders().getContentType().toString());
        assertEquals(PNG.length, resposta.getHeaders().getContentLength());
        assertArrayEquals(PNG, resposta.getBody());
    }

    @Test
    void obterImagemSemImagemLancaNotFound() {
        Receitas minha = receitaDe(dono);

        assertThrows(ResourceNotFoundException.class, () -> service.getImagemReceita(minha.getId()));
    }

    @Test
    void deletarImagemRemoveAAssociacao() {
        Receitas minha = receitaDe(dono);
        service.uploadImagemReceita(minha.getId(), arquivo(JPEG));

        service.deletarImagemReceita(minha.getId());

        assertNull(minha.getImagem());
    }

    @Test
    void deletarImagemInexistenteLancaNotFound() {
        Receitas minha = receitaDe(dono);

        assertThrows(ResourceNotFoundException.class, () -> service.deletarImagemReceita(minha.getId()));
    }

    @Test
    void deletarImagemDeOutroUsuarioLancaForbidden() {
        Receitas alheia = receitaDe(outro);
        alheia.setImagem(new ReceitaImagem());

        assertThrows(ForbiddenException.class, () -> service.deletarImagemReceita(alheia.getId()));
        assertNotNull(alheia.getImagem());
    }

    // ---------- leitura ----------

    @Test
    void minhasReceitasConsultaPeloUsuarioLogado() {
        Receitas minha = receitaDe(dono);
        when(receitasRepository.findAllByUsuario(dono)).thenReturn(List.of(minha));

        var lista = service.minhasReceitas();

        assertEquals(1, lista.size());
        assertEquals(minha.getId(), lista.get(0).getId());
    }

    @Test
    void receitaInexistenteNaConsultaPorIdLancaNotFound() {
        when(receitasRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getReceitasById(UUID.randomUUID()));
    }
}
