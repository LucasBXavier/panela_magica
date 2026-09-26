package com.panelamagica.panelamagica;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de ponta a ponta com PostgreSQL real (Testcontainers). Também substituem o antigo {@code contextLoads},
 * que dependia de um banco e de variáveis de ambiente locais.
 * <p>
 * Requerem Docker; sem Docker os testes são <b>ignorados</b> (não falham). No GitHub Actions o Docker existe.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "jwt.secret=segredo-de-teste-de-integracao-com-mais-de-32-caracteres",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class ApiFlowIntegrationTest {

    private static final String SENHA = "Senha@1234";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired EntityManagerFactory entityManagerFactory;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------- helpers ----------

    record Sessao(String email, String token, String refreshToken) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String body) {
        return b.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private Sessao novoUsuario() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@teste.com";
        mvc.perform(json(post("/api/v1/usuarios"),
                        "{\"nome\":\"Teste\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA)))
                .andExpect(status().isCreated());
        return login(email);
    }

    private Sessao login(String email) throws Exception {
        String body = mvc.perform(json(post("/api/v1/auth/login"),
                        "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Sessao(email, JsonPath.read(body, "$.data.token"), JsonPath.read(body, "$.data.refreshToken"));
    }

    private String receitaJson(String nome, String... ingredientes) {
        StringBuilder itens = new StringBuilder();
        for (String ing : ingredientes) {
            if (itens.length() > 0) itens.append(',');
            itens.append("{\"nomeIngrediente\":\"%s\",\"quantidade\":1.5,\"unidadeMedida\":\"grama\"}".formatted(ing));
        }
        return "{\"nome\":\"%s\",\"descricao\":\"d\",\"modoPreparo\":\"m\",\"tempoPreparo\":\"1h\",\"rendimento\":\"8\",\"categoria\":\"doce\",\"ingredientes\":[%s]}"
                .formatted(nome, itens);
    }

    private String criarReceita(Sessao s, String nome, String... ingredientes) throws Exception {
        MvcResult r = mvc.perform(json(post("/api/v1/receitas"), receitaJson(nome, ingredientes)).header("Authorization", s.bearer()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.data.id");
    }

    private MockMultipartHttpServletRequestBuilder uploadImagem(String receitaId, Sessao s) {
        return multipart(HttpMethod.PUT, "/api/v1/receitas/{id}/imagem", receitaId)
                .file(new MockMultipartFile("file", "f.png", "image/png", PNG))
                .header("Authorization", s.bearer());
    }

    // ---------- testes ----------

    @Test
    void contextoSobeComOEsquemaCriadoNoPostgres() {
        assertNotNull(context);
        Integer tabelas = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name in "
                        + "('usuarios','receitas','ingredientes','receita_ingrediente','receita_imagem','refresh_tokens')",
                Integer.class);
        assertEquals(6, tabelas);
    }

    @Test
    void fluxoCompletoDeReceitaEImagem() throws Exception {
        Sessao maria = novoUsuario();

        mvc.perform(get("/api/v1/usuarios/me/receitas").header("Authorization", maria.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));

        String id = criarReceita(maria, "Bolo", "Farinha", "Sal");

        // leitura pública, sem token
        mvc.perform(get("/api/v1/receitas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nome").value("Bolo"))
                .andExpect(jsonPath("$.data.categoria").value("DOCE"))
                .andExpect(jsonPath("$.data.ingredientes.length()").value(2))
                .andExpect(jsonPath("$.data.imagemUrl").doesNotExist());

        // nome duplicado do mesmo usuário
        mvc.perform(json(post("/api/v1/receitas"), receitaJson("Bolo", "Sal")).header("Authorization", maria.bearer()))
                .andExpect(status().isBadRequest());

        // imagem: envio, leitura pública, url na receita, remoção
        mvc.perform(uploadImagem(id, maria)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/receitas/{id}/imagem", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));
        mvc.perform(get("/api/v1/receitas/{id}", id))
                .andExpect(jsonPath("$.data.imagemUrl").value("/api/v1/receitas/" + id + "/imagem"));
        mvc.perform(delete("/api/v1/receitas/{id}/imagem", id).header("Authorization", maria.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/receitas/{id}/imagem", id)).andExpect(status().isNotFound());

        // atualização parcial e substituição de ingredientes
        mvc.perform(json(patch("/api/v1/receitas/{id}", id),
                        "{\"rendimento\":\"16\",\"ingredientes\":[{\"nomeIngrediente\":\"Cenoura\",\"quantidade\":3,\"unidadeMedida\":\"unidade\"}]}")
                        .header("Authorization", maria.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rendimento").value("16"))
                .andExpect(jsonPath("$.data.nome").value("Bolo"))
                .andExpect(jsonPath("$.data.ingredientes.length()").value(1))
                .andExpect(jsonPath("$.data.ingredientes[0].nomeIngrediente").value("Cenoura"));

        mvc.perform(get("/api/v1/usuarios/me/receitas").header("Authorization", maria.bearer()))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void ingredienteEhReaproveitadoSemDiferenciarMaiusculas() throws Exception {
        Sessao s = novoUsuario();
        String nome = "Ing" + UUID.randomUUID().toString().substring(0, 8);
        criarReceita(s, "R1", nome);
        criarReceita(s, "R2", nome.toUpperCase());

        Integer quantos = jdbc.queryForObject("select count(*) from ingredientes where lower(nome) = lower(?)", Integer.class, nome);
        assertEquals(1, quantos);
    }

    @Test
    void outroUsuarioNaoAlteraNemApagaReceitaAlheia() throws Exception {
        Sessao dono = novoUsuario();
        Sessao intruso = novoUsuario();
        String id = criarReceita(dono, "Da Maria", "Sal");

        mvc.perform(json(patch("/api/v1/receitas/{id}", id), "{\"nome\":\"Roubada\"}").header("Authorization", intruso.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/receitas/{id}", id).header("Authorization", intruso.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(uploadImagem(id, intruso)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/receitas/{id}/imagem", id).header("Authorization", intruso.bearer()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/receitas/{id}", id)).andExpect(jsonPath("$.data.nome").value("Da Maria"));
        mvc.perform(delete("/api/v1/receitas/{id}", UUID.randomUUID()).header("Authorization", intruso.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluirReceitaApagaImagemEIngredientesAssociados() throws Exception {
        Sessao s = novoUsuario();
        String id = criarReceita(s, "Para apagar", "Sal");
        mvc.perform(uploadImagem(id, s)).andExpect(status().isOk());
        UUID imagemId = jdbc.queryForObject("select imagem_id from receitas where id = ?", UUID.class, UUID.fromString(id));
        assertNotNull(imagemId);

        mvc.perform(delete("/api/v1/receitas/{id}", id).header("Authorization", s.bearer())).andExpect(status().isNoContent());

        assertEquals(0, jdbc.queryForObject("select count(*) from receita_imagem where id = ?", Integer.class, imagemId));
        assertEquals(0, jdbc.queryForObject("select count(*) from receita_ingrediente where receita_id = ?", Integer.class, UUID.fromString(id)));
        mvc.perform(get("/api/v1/receitas/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void listagemNaoTemN1() throws Exception {
        Sessao s = novoUsuario();
        for (int i = 0; i < 3; i++) {
            criarReceita(s, "Receita N1 " + i, "A" + i, "B" + i, "C" + i);
        }
        var stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        stats.clear();
        mvc.perform(get("/api/v1/receitas")).andExpect(status().isOk());
        assertTrue(stats.getPrepareStatementCount() <= 2,
                "listagem executou " + stats.getPrepareStatementCount() + " statements (esperado ≤ 2)");

        stats.clear();
        mvc.perform(get("/api/v1/usuarios/me/receitas").header("Authorization", s.bearer())).andExpect(status().isOk());
        // 1 para o usuário (getAuthenticatedUser) + 1 para as receitas com ingredientes
        assertTrue(stats.getPrepareStatementCount() <= 3,
                "minhas receitas executou " + stats.getPrepareStatementCount() + " statements (esperado ≤ 3)");
    }

    @Test
    void cadastroComEmailDuplicadoEhRecusado() throws Exception {
        Sessao s = novoUsuario();

        mvc.perform(json(post("/api/v1/usuarios"),
                        "{\"nome\":\"Outro\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(s.email().toUpperCase(), SENHA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshTokenRotacionaEDetectaReutilizacao() throws Exception {
        Sessao s = novoUsuario();

        // só o hash fica no banco
        List<String> hashes = jdbc.queryForList("select token_hash from refresh_tokens", String.class);
        assertFalse(hashes.contains(s.refreshToken()));

        String resp = mvc.perform(json(post("/api/v1/auth/refresh"), "{\"refreshToken\":\"%s\"}".formatted(s.refreshToken())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String novoRefresh = JsonPath.read(resp, "$.data.refreshToken");
        String novoToken = JsonPath.read(resp, "$.data.token");
        assertNotEquals(s.refreshToken(), novoRefresh);
        mvc.perform(get("/api/v1/usuarios/me/receitas").header("Authorization", "Bearer " + novoToken)).andExpect(status().isOk());

        // o refresh antigo não vale mais e, por ser reutilização, derruba também o novo
        mvc.perform(json(post("/api/v1/auth/refresh"), "{\"refreshToken\":\"%s\"}".formatted(s.refreshToken())))
                .andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/v1/auth/refresh"), "{\"refreshToken\":\"%s\"}".formatted(novoRefresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevogaORefreshToken() throws Exception {
        Sessao s = novoUsuario();

        mvc.perform(json(post("/api/v1/auth/logout"), "{\"refreshToken\":\"%s\"}".formatted(s.refreshToken())))
                .andExpect(status().isNoContent());
        mvc.perform(json(post("/api/v1/auth/refresh"), "{\"refreshToken\":\"%s\"}".formatted(s.refreshToken())))
                .andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/v1/auth/logout"), "{\"refreshToken\":\"desconhecido\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void loginEhBloqueadoAposVariasFalhas() throws Exception {
        Sessao s = novoUsuario();
        String errado = "{\"email\":\"%s\",\"senha\":\"SenhaErrada@1\"}".formatted(s.email());

        for (int i = 0; i < 5; i++) {
            mvc.perform(json(post("/api/v1/auth/login"), errado)).andExpect(status().isUnauthorized());
        }
        mvc.perform(json(post("/api/v1/auth/login"), errado))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        // mesmo com a senha certa, enquanto durar o bloqueio
        mvc.perform(json(post("/api/v1/auth/login"), "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(s.email(), SENHA)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void tokenAntigoComSubjectEmailExigeNovoLogin() throws Exception {
        Sessao s = novoUsuario();
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("panela-magica").subject(s.email())
                .issuedAt(agora).expiresAt(agora.plusSeconds(600)).claim("scope", "USER").build();
        String antigo = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mvc.perform(get("/api/v1/usuarios/me/receitas").header("Authorization", "Bearer " + antigo))
                .andExpect(status().isUnauthorized());
    }
}
