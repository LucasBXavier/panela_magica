package com.panelamagica.panelamagica.config;

import com.panelamagica.panelamagica.controller.AuthController;
import com.panelamagica.panelamagica.controller.ReceitasController;
import com.panelamagica.panelamagica.controller.UsuarioController;
import com.panelamagica.panelamagica.domain.entites.Usuario;
import com.panelamagica.panelamagica.dto.login.LoginResponseDTO;
import com.panelamagica.panelamagica.dto.receitas.ReceitaResponseDTO;
import com.panelamagica.panelamagica.exception.GlobalExceptionHandler;
import com.panelamagica.panelamagica.security.TokenService;
import com.panelamagica.panelamagica.service.ReceitaService;
import com.panelamagica.panelamagica.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Matriz de rotas públicas × privadas do {@link SecurityConfig}, usando os controllers reais (com os services
 * mockados) e o filtro de segurança real. Sem banco de dados.
 */
@SpringJUnitWebConfig(SecurityRoutesTest.Config.class)
@TestPropertySource(properties = {
        "jwt.secret=segredo-de-teste-com-mais-de-32-caracteres!",
        "jwt.expiration-minutes=60",
        "cors.allowed-origins=http://localhost:3000"
})
class SecurityRoutesTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, JwtConfig.class, TokenService.class, GlobalExceptionHandler.class,
            ReceitasController.class, UsuarioController.class, AuthController.class})
    static class Config {
        @Bean
        ConversionService conversionService() {
            return new DefaultConversionService();
        }
    }

    private static final String BODY_RECEITA = """
            {"nome":"Bolo","descricao":"d","modoPreparo":"m","tempoPreparo":"1h","rendimento":"8","categoria":"doce",
             "ingredientes":[{"nomeIngrediente":"Sal","quantidade":1,"unidadeMedida":"grama"}]}""";
    private static final String BODY_USUARIO = """
            {"nome":"Maria","email":"maria@email.com","senha":"Senha@1234"}""";
    private static final String BODY_LOGIN = """
            {"email":"maria@email.com","senha":"Senha@1234"}""";
    private static final String BODY_REFRESH = """
            {"refreshToken":"abc"}""";

    private final UUID id = UUID.randomUUID();

    @Autowired WebApplicationContext context;
    @Autowired TokenService tokenService;
    @Autowired JwtEncoder jwtEncoder;

    @MockitoBean ReceitaService receitaService;
    @MockitoBean UsuarioService usuarioService;
    @MockitoBean UserDetailsService userDetailsService;

    MockMvc mvc;
    String bearer;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        bearer = "Bearer " + tokenService.gerarToken(u);

        ReceitaResponseDTO dto = new ReceitaResponseDTO();
        dto.setId(id);
        when(receitaService.criarReceita(any())).thenReturn(dto);
        when(receitaService.getReceitasById(any())).thenReturn(dto);
        when(receitaService.atualizarReceita(any(), any())).thenReturn(dto);
        when(receitaService.getImagemReceita(any())).thenReturn(ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(new byte[]{1}));
        when(receitaService.uploadImagemReceita(any(), any())).thenReturn("/api/v1/receitas/" + id + "/imagem");
        when(usuarioService.login(any(), anyString())).thenReturn(LoginResponseDTO.builder().token("t").type("Bearer").build());
        when(usuarioService.refresh(any())).thenReturn(LoginResponseDTO.builder().token("t").type("Bearer").build());
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String body) {
        return b.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockMultipartHttpServletRequestBuilder imagem(HttpMethod metodo) {
        return multipart(metodo, "/api/v1/receitas/{id}/imagem", id)
                .file(new MockMultipartFile("file", "f.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0}));
    }

    // ---------- rotas públicas ----------

    @Test
    void rotasPublicasNaoExigemToken() throws Exception {
        mvc.perform(get("/api/v1/receitas")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/receitas/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/receitas/{id}/imagem", id)).andExpect(status().isOk());
        mvc.perform(json(post("/api/v1/usuarios"), BODY_USUARIO)).andExpect(status().isCreated());
        mvc.perform(json(post("/api/v1/auth/login"), BODY_LOGIN)).andExpect(status().isOk());
        mvc.perform(json(post("/api/v1/auth/refresh"), BODY_REFRESH)).andExpect(status().isOk());
        mvc.perform(json(post("/api/v1/auth/logout"), BODY_REFRESH)).andExpect(status().isNoContent());
    }

    @Test
    void swaggerEPreflightCorsNaoSaoBloqueados() throws Exception {
        // não há handler para o Swagger neste contexto: o que importa é não ser barrado pela segurança (401)
        mvc.perform(get("/panela-magica/swagger-ui.html")).andExpect(status().isNotFound());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mvc.perform(options("/api/v1/receitas")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    // ---------- rotas privadas sem token ----------

    @Test
    void rotasPrivadasSemTokenRetornam401ComCorpoPadronizado() throws Exception {
        List<RequestBuilder> privadas = List.of(
                get("/api/v1/usuarios/me/receitas"),
                json(post("/api/v1/receitas"), BODY_RECEITA),
                json(patch("/api/v1/receitas/{id}", id), "{}"),
                delete("/api/v1/receitas/{id}", id),
                imagem(HttpMethod.PUT),
                delete("/api/v1/receitas/{id}/imagem", id));

        for (RequestBuilder req : privadas) {
            mvc.perform(req)
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Token ausente, inválido ou expirado"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Test
    void permissaoPublicaValeSoParaOMetodoGet() throws Exception {
        mvc.perform(json(post("/api/v1/receitas"), BODY_RECEITA)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/receitas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void rotasAntigasEDesconhecidasNaoSaoPublicas() throws Exception {
        // a rota antiga casa com o padrão público /receitas/{id}: passa pela segurança e falha como UUID inválido
        // (400, sem expor dado algum); o que importa é que não devolve receitas
        mvc.perform(get("/api/v1/receitas/minhas-receitas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para o parâmetro 'id'"));
        mvc.perform(get("/api/v1/receitas/imagem/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/v1/usuarios/login"), BODY_LOGIN)).andExpect(status().isUnauthorized());
        mvc.perform(get("/qualquer/coisa")).andExpect(status().isUnauthorized());
    }

    // ---------- rotas privadas com token ----------

    @Test
    void rotasPrivadasComTokenValidoFuncionam() throws Exception {
        mvc.perform(get("/api/v1/usuarios/me/receitas").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(json(post("/api/v1/receitas"), BODY_RECEITA).header("Authorization", bearer))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/receitas/" + id)));
        mvc.perform(json(patch("/api/v1/receitas/{id}", id), "{\"rendimento\":\"16\"}").header("Authorization", bearer))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/receitas/{id}", id).header("Authorization", bearer)).andExpect(status().isNoContent());
        mvc.perform(imagem(HttpMethod.PUT).header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/receitas/{id}/imagem", id).header("Authorization", bearer)).andExpect(status().isNoContent());
    }

    @Test
    void enumInvalidoComTokenRetorna400ComValoresAceitos() throws Exception {
        mvc.perform(json(post("/api/v1/receitas"), BODY_RECEITA.replace("\"doce\"", "\"xyz\"")).header("Authorization", bearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("SOBREMESA")));
    }

    // ---------- tokens inválidos ----------

    @Test
    void tokensInvalidosSaoRecusadosMesmoEmRotasPublicas() throws Exception {
        Instant agora = Instant.now();
        String expirado = codificar("panela-magica", agora.minusSeconds(7200), agora.minusSeconds(3600));
        String outroEmissor = codificar("outro", agora, agora.plusSeconds(600));

        for (String token : List.of(expirado, outroEmissor, "lixo.que.nao.e.jwt", "")) {
            mvc.perform(get("/api/v1/usuarios/me/receitas").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
        // documenta o risco de negócio §5.2: token inválido derruba até rota pública
        mvc.perform(get("/api/v1/receitas").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    private String codificar(String emissor, Instant emitidoEm, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emissor).subject(UUID.randomUUID().toString())
                .issuedAt(emitidoEm).expiresAt(expiraEm).claim("scope", "USER").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
