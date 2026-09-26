package com.panelamagica.panelamagica.exception;

import com.panelamagica.panelamagica.domain.enums.Categoria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    static class Corpo {
        @NotBlank
        public String nome;
        public Categoria categoria;
    }

    @RestController
    static class ControllerDeTeste {
        @GetMapping("/business") void business() { throw new BusinessRuleException("regra violada"); }
        @GetMapping("/naoautorizado") void naoAutorizado() { throw new UnauthorizedException("sem sessão"); }
        @GetMapping("/credenciais") void credenciais() { throw new BadCredentialsException("x"); }
        @GetMapping("/desabilitado") void desabilitado() { throw new DisabledException("conta desabilitada"); }
        @GetMapping("/negado") void negado() { throw new AccessDeniedException("x"); }
        @GetMapping("/limite") void limite() { throw new TooManyRequestsException("calma", 42); }
        @GetMapping("/integridade") void integridade() {
            throw new DataIntegrityViolationException("erro", new RuntimeException("duplicate key value ... uk_secreto"));
        }
        @GetMapping("/bug") void bug() { throw new RuntimeException("detalhe interno secreto"); }
        @GetMapping("/iae") void iae() { throw new IllegalArgumentException("bug interno"); }
        @GetMapping("/item/{id}") void item(@PathVariable UUID id) { }
        @PostMapping("/corpo") void corpo(@Valid @RequestBody Corpo corpo) { }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void regraDeNegocioVira400() throws Exception {
        mvc.perform(get("/business")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("regra violada"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void autenticacaoVira401() throws Exception {
        mvc.perform(get("/naoautorizado")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("sem sessão"));
        mvc.perform(get("/credenciais")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
        mvc.perform(get("/desabilitado")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Não autenticado"));
    }

    @Test
    void acessoNegadoDoSpringSecurityVira403() throws Exception {
        mvc.perform(get("/negado")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado"));
    }

    @Test
    void limiteDeTentativasVira429ComRetryAfter() throws Exception {
        mvc.perform(get("/limite")).andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"));
    }

    @Test
    void violacaoDeIntegridadeVira409SemVazarDetalhes() throws Exception {
        mvc.perform(get("/integridade")).andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("uk_secreto"))));
    }

    @Test
    void erroInesperadoVira500GenericoSemVazarDetalhes() throws Exception {
        mvc.perform(get("/bug")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno do servidor"))
                .andExpect(content().string(not(containsString("secreto"))));
    }

    @Test
    void illegalArgumentNaoEhMaisTratadoComoErroDoCliente() throws Exception {
        mvc.perform(get("/iae")).andExpect(status().isInternalServerError());
    }

    @Test
    void uuidInvalidoNoPathVira400() throws Exception {
        mvc.perform(get("/item/abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para o parâmetro 'id'"));
    }

    @Test
    void metodoNaoPermitidoMantem405EHeaderAllow() throws Exception {
        mvc.perform(post("/business")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.message").value("Método não permitido"));
    }

    @Test
    void tipoDeConteudoNaoSuportadoMantem415() throws Exception {
        mvc.perform(post("/corpo").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void validacaoListaOsCampos() throws Exception {
        mvc.perform(post("/corpo").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("nome")));
    }

    @Test
    void enumInvalidoInformaValoresAceitos() throws Exception {
        mvc.perform(post("/corpo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"a\",\"categoria\":\"xyz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("DOCE")));
    }

    @Test
    void jsonMalformadoNaoVazaDetalhesDoParser() throws Exception {
        mvc.perform(post("/corpo").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido ou malformado"));
    }
}
