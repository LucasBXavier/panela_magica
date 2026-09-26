# Code Review — PanelaMagica

**Data:** 2026-09-25 · **Stack:** Spring Boot 4.0.8, Java 21, JPA/Hibernate, PostgreSQL, JWT (OAuth2 Resource Server)

**Escopo:** `src/main`, `pom.xml`, `application.properties`, `docker-compose.yml`, `.gitignore` e o teste existente. Revisão estática, complementada por testes automatizados e por execução real da aplicação (ver abaixo). `SwaggerConfig`, os enums e os DTOs triviais (`ErrorResponseDTO`, `LoginRequestDTO` etc.) não foram analisados em detalhe.

**Última revalidação:** 2026-09-25.

**Validação realizada (execução real):**
- **91 testes automatizados passando** (`./mvnw verify`), com **93,4 % de cobertura de instruções e 71,0 % de branches** (JaCoCo, `target/site/jacoco`).
- **11 testes de integração** (`ApiFlowIntegrationTest`) contra **PostgreSQL 16 real** via Testcontainers: criação do esquema pelo Hibernate (as 6 tabelas), fluxo cadastro → login → receita → imagem → atualização → exclusão, 403 para não-dono, cascata de exclusão (imagem e ingredientes), ingrediente reaproveitado sem diferenciar maiúsculas, **ausência de N+1** (listagem em ≤ 2 statements), refresh token (rotação, reutilização, logout, hash no banco), rate limit (429), token antigo (sub = e-mail) → 401.
- **Aplicação executada com Tomcat real** contra um Postgres descartável: Swagger UI (200) e `/v3/api-docs` (OpenAPI 3.1 com as 8 rotas novas) com **springdoc 3.0.2**; `PUT` multipart de imagem, leitura pública da imagem, `Location`, 204, 401/404/400 padronizados, CORS (preflight + headers expostos) e 429 com `Retry-After` — todos como esperado.
- Também: `SecurityRoutesTest` (matriz de rotas públicas × privadas com o filtro de segurança real), `GlobalExceptionHandlerTest`, `ReceitaServiceTest`, `ReceitaMapperTest`, `LoginRateLimiterTest`, `JwtTest`, `RefreshTokenServiceTest` e `ArchitectureTest` (ArchUnit).

**Ainda não verificado:** o script SQL de migração das imagens antigas (README), o comportamento com várias instâncias (rate limit em memória) e o front-end consumindo o novo contrato.

---

## Itens resolvidos até agora

| Item | Problema | Como foi resolvido | Detalhes |
|---|---|---|---|
| ✅ P0-1 | Imagem `bytea` carregada em toda leitura | Imagem movida para `ReceitaImagem` (`@OneToOne LAZY`); service reescrito | [§3 P0-1](#p0-1) |
| ✅ P0-2 | N+1 na listagem | `@EntityGraph` em `findAll`, `findById` e `findAllByUsuario` | [§3 P0-2](#p0-2) |
| ✅ P0-3 | `/minhas-receitas` público, respondendo 400 | Regras do `SecurityConfig` com método explícito (hoje `GET /usuarios/me/receitas` autenticado) | [§3 P0-3](#p0-3) |
| ✅ P0-4 | springdoc 2.8.13 com Spring Boot 4 | Atualizado para 3.0.2; Swagger e OpenAPI verificados com a aplicação rodando | [§3 P0-4](#p0-4) |
| ✅ P1-2 | Autorização mapeada para 400 | `ForbiddenException` → **403** para receita de outro usuário; não autenticado → **401**; `buscarDoUsuario` único (era repetido 4×); exclusão sem queries extras | [P1-2](#p1-2) |
| ✅ P1-5 | Tratamento de erros incompleto | `GlobalExceptionHandler` reescrito (handler genérico 500 sem vazar detalhes, 401/403/409/429, tipo de parâmetro inválido, 404/405/415 do Spring preservados) e 401/403 do Security no mesmo formato de erro | [P1-5](#p1-5) |
| ✅ P1-6 | `open-in-view` ativo | `spring.jpa.open-in-view=false` | [P1-6](#p1-6) |
| ✅ P1-7 | Segurança de autenticação | Rate limit no login (429 + `Retry-After`), validação de `iss`, `sub` = id do usuário, refresh token com rotação e logout, regra de senha corrigida | [P1-7](#p1-7) |
| ✅ P1-8 | Ausência de testes | 91 testes (unitários, segurança das rotas, ArchUnit, integração com PostgreSQL via Testcontainers), `contextLoads` sem depender de banco local, JaCoCo (93,4 %) e CI no GitHub Actions | [P1-8](#p1-8) |
| ✅ P2-1 | `@Data` em entidades | `BaseEntity` com `id` UUID e `equals/hashCode` por `id`; `@Getter/@Setter`; `Usuario.toString()` sem senha | [P2-1](#p2-1) |
| ✅ P2-2 | Estratégia de ID inconsistente | Todas as entidades com `GenerationType.UUID` (via `BaseEntity`) | [P2-2](#p2-2) |
| ✅ P2-5 | Injeção inconsistente | `ReceitaService` com `@RequiredArgsConstructor` e campos `final` | [P2-5](#p2-5) |
| ✅ P2-6 | Validação de DTO frágil | Enums nos DTOs, `@Size(max)`, "não vazio" declarativo, itens `null` rejeitados, redundâncias removidas | [P2-6](#p2-6) |
| ✅ P2-7 | Contrato REST fora do padrão | Rotas sem verbos, `PUT /{id}/imagem`, `Location` no 201, `204` nos DELETE, datas ISO-8601, IDs `UUID`, URL da imagem centralizada (**breaking**) | [P2-7](#p2-7) |

**Regressão encontrada e corrigida:** a primeira tentativa de aceitar `#`, `-`, `_` na senha usava `[@$!%*?&#-_]`, cujo `#-_` é um intervalo que incluía dígitos e maiúsculas (aceitava `Abcdefg1` sem especial). A versão atual (`[@$!%*?&#_-]`, hífen no fim) foi verificada: `Abcdefg1` recusada; `Abcdefg1#`, `Abcdefg1-`, `Abcdefg1_` e `Abcdefg1@` aceitas.

---

## 1. Resumo executivo

A base é limpa e coerente: camadas claras (controller → service → repository), DTOs separados das entidades, exception handler global, JWT stateless, BCrypt, validação de imagem por *magic bytes* e segredos fora do repositório. Para um MVP está acima da média.

Os riscos restantes concentram-se em **(a) ausência de migrations** (`ddl-auto=update`, P1-3), **(b) integridade de dados garantida só pela aplicação** (P1-4), **(c) falta de paginação** (P1-1), **(d) acoplamento do service à camada web e mapper com efeito colateral** (P2-3/P2-4) e **(e) configuração por ambiente e observabilidade** (P2-8/P2-9). Todos os itens críticos (P0) estão resolvidos e verificados em execução, assim como segurança/autenticação, tratamento de erros, autorização por dono e testes automatizados.

### Status dos itens críticos (P0)

| # | Item | Status | Observação |
|---|---|---|---|
| P0-1 | Imagem `bytea` carregada em toda leitura | ✅ Corrigido | Imagem movida para `ReceitaImagem` (`@OneToOne LAZY`). `getImagem() != null` no mapper não carrega os bytes. Falta migrar dados antigos (ver §7). |
| P0-2 | N+1 na listagem | ✅ Corrigido | `@EntityGraph` em `findAll()`, `findById()` e `findAllByUsuario()`. Falta paginação (P1-1). |
| P0-3 | `/minhas-receitas` público / 400 | ✅ Corrigido | Regras de segurança com método explícito. `GET /minhas-receitas` exige token. |
| P0-4 | springdoc 2.8.13 com Boot 4 | ✅ Corrigido e verificado | Atualizado para 3.0.2; Swagger UI e `/v3/api-docs` respondem 200 com a aplicação rodando. |

| Prioridade | Total | Resolvidos | Parciais | Abertos |
|---|---|---|---|---|
| P0 | 4 | 4 | 0 | 0 |
| P1 | 8 | 5 (P1-2, P1-5, P1-6, P1-7, P1-8) | 0 | 3 |
| P2 | 9 | 5 (P2-1, P2-2, P2-5, P2-6, P2-7) | 0 | 4 |
| P3 | 6 | 0 | 0 | 6 |

---

## 2. Pontos positivos

- Propriedade dos recursos verificada no service em update, delete e imagem.
- `JwtConfig` falha no startup se o segredo tiver menos de 32 bytes.
- Upload valida o tipo real pelo conteúdo, não pelo `Content-Type` do cliente; limite de 5MB.
- E-mail normalizado (`trim().toLowerCase()`) de forma consistente.
- Sessão `STATELESS`, CSRF desabilitado coerentemente com JWT em header, CORS configurável.
- `@Transactional(readOnly = true)` nas leituras; `orphanRemoval` e helpers `addIngrediente/removeIngrediente` mantêm as duas pontas da associação.
- `.env` ignorado pelo git.

---

## 3. P0 — Itens críticos (resolvidos)

Registro do que foi encontrado e feito, para rastreabilidade.

<a id="p0-1"></a>
### P0-1. Imagem em `bytea` carregada em toda leitura ✅
**Problema:** `byte[]` em coluna básica não é lazy; `findAll()`, `minhasReceitas()` e `findById()` traziam até 5MB por receita só para o mapper checar `getImagem() != null`. A listagem pública virava vetor de degradação.
**Verificado em execução:** upload (`PUT` multipart em Tomcat real), leitura pública, remoção e cascata na exclusão da receita, com PostgreSQL real.
**Correção aplicada:**
- Nova entidade `ReceitaImagem` (`dados`, `contentType`), ligada por `Receitas.imagem` (`@OneToOne(LAZY, cascade = ALL, orphanRemoval = true)`, FK `imagem_id`).
- `ReceitaService`: upload cria/reaproveita o `ReceitaImagem`; leitura devolve `dados`/`contentType`; exclusão faz `setImagem(null)` (o `orphanRemoval` remove a linha).
- Removidos `ImagemRepository` e `ReceitaImagem.usuario` (o dono é o da receita).
**Melhorias futuras:** object storage (S3/MinIO) guardando só a chave; `Cache-Control`/`ETag` no endpoint de imagem; o endpoint de imagem hoje carrega a receita com o entity graph de ingredientes (joins desnecessários) — criar query própria só para a imagem.

<a id="p0-2"></a>
### P0-2. N+1 na listagem ✅
**Problema:** para cada receita, `ingredientes` (lazy) e, para cada item, `ingrediente` (lazy): 1 + N + N·M queries.
**Correção aplicada:** `@EntityGraph(attributePaths = {"ingredientes", "ingredientes.ingrediente"})` em `ReceitasRepository.findAll()`, `findById()` e `findAllByUsuario()`.
**Verificado em execução:** teste de integração mede os statements do Hibernate: listagem pública em ≤ 2 (sem N+1) mesmo com várias receitas e ingredientes.
**Atenção:** com fetch join de coleção e paginação, o Hibernate pagina em memória (`HHH90003004`). Ao implementar P1-1, usar `default_batch_fetch_size` ou consulta em duas etapas (ids paginados + fetch).

<a id="p0-3"></a>
### P0-3. `GET /minhas-receitas` público, respondendo 400 ✅
**Problema:** `permitAll` para `GET /api/v1/receitas/**` cobria `/minhas-receitas`; sem token o request chegava ao service e retornava 400.
**Correção aplicada (`SecurityConfig`):**
```java
.requestMatchers(HttpMethod.POST, "/api/v1/usuarios", "/api/v1/auth/login").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/receitas", "/api/v1/receitas/{id}", "/api/v1/receitas/{id}/imagem").permitAll()
// demais rotas (inclui GET /api/v1/usuarios/me/receitas): anyRequest().authenticated()
```
Após a reorganização de rotas do P2-7, `/minhas-receitas` virou `GET /api/v1/usuarios/me/receitas`, que não casa com nenhum padrão público. Antes de a regra ser reescrita, uma versão intermediária usava padrões sem método e em ordem errada (a primeira regra que casa vence), o que deixava `DELETE/PATCH /{id}` sem exigência de token.
**Verificado:** `SecurityRoutesTest` cobre a matriz completa. Detalhe descoberto: a rota antiga `GET /receitas/minhas-receitas` casa com o padrão público `/receitas/{id}` e responde **400** ("Valor inválido para o parâmetro 'id'"), não 401 — inofensivo (não devolve dado), mas é o comportamento esperado para clientes que ainda usem a rota antiga.
**Atenção:** ao adicionar rotas em `/receitas/**`, atualizar esse teste.

<a id="p0-4"></a>
### P0-4. springdoc incompatível com Spring Boot 4 ✅
**Problema:** `springdoc-openapi-starter-webmvc-ui:2.8.13` é da linha 2.x (Boot 3.x).
**Correção aplicada:** atualizado para `3.0.2`. **Verificado em execução:** `/panela-magica/swagger-ui.html` → 200 e `/panela-magica/v3/api-docs` → OpenAPI 3.1 com todas as rotas.

---

## 4. Achados abertos

### P1 — Alto

<a id="p1-1"></a>**P1-1. Sem paginação** — `ReceitasController.java` (`getReceitas`, `getMinhasReceitas`). Retornam tudo. Usar `Pageable` com DTO resumido (sem `modoPreparo`/ingredientes) e filtros por `categoria` e `nome`. Ver atenção do P0-2.

<a id="p1-2"></a>**P1-2. Autorização mapeada para 400** — ✅ **Resolvido** — `ReceitaService`, `GlobalExceptionHandler`.
- **403** `ForbiddenException` para receita de outro usuário em atualizar, excluir, enviar imagem e excluir imagem (antes `BusinessRuleException` → 400). Optou-se por **403 e não 404**: as receitas são públicas para leitura, então revelar que existem não expõe nada, e o 403 é mais claro para o cliente.
- **401** `UnauthorizedException` quando não há usuário autenticado, o `sub` do token não é um UUID (token antigo) ou o usuário não existe mais.
- Novo método privado `buscarDoUsuario(id, usuario)`: carrega a receita (404 se não existe) e valida o dono (403). Substitui o bloco de checagem que se repetia 4×.
- `deletarReceita` passou de 3 queries (`existsById`, `existsByIdAndUsuarioId`, `deleteById`) para carregar a receita e removê-la (`delete(receita)`); o método morto `existsByIdAndUsuarioId` saiu do repository.
- Verificado: `ReceitaServiceTest` (dono × não-dono × inexistente para cada operação, e nada é gravado/apagado quando negado), `ApiFlowIntegrationTest` (403 com PostgreSQL real e a receita continua intacta) e `SecurityRoutesTest`.
- **Contrato:** mudança de 400 → 403 (documentada em `API_DOCUMENTATION.md`). O front que tratava "receita de outro usuário" como 400 precisa ser ajustado.

<a id="p1-3"></a>**P1-3. `ddl-auto=update` sem migrations** — `application.properties`. Não é reproduzível nem seguro fora do dev (não remove/renomeia colunas; não migra dados — como no P0-1). Adotar **Flyway** (`V1__baseline.sql`), `ddl-auto=validate`.

<a id="p1-4"></a>**P1-4. Integridade só na aplicação**
- Unicidade `(usuario_id, nome)` da receita é *check-then-act* — race condition e sensível a maiúsculas/minúsculas.
- `ingredientes.nome` tem `unique` case-sensitive, mas a busca é `IgnoreCase`: requisições concorrentes com "Sal"/"sal" → `DataIntegrityViolationException` → 500.
- Sem índices em `receitas.usuario_id` e `receita_ingrediente.receita_id`.
*Correção:* índices únicos em minúsculas (`UNIQUE (usuario_id, lower(nome))`, `UNIQUE (lower(nome))`), normalização com `trim()` e tratamento de `DataIntegrityViolationException` → 409.

<a id="p1-5"></a>**P1-5. Tratamento de erros incompleto** — ✅ **Resolvido** — `GlobalExceptionHandler`, `SecurityConfig`.

**Revisão da versão anterior do handler** (alterada pelo time) — defeitos encontrados:
1. `@ExceptionHandler(Exception.class)` estava sobre o método auxiliar `build(HttpStatus, String)`. O Spring não consegue resolver esses parâmetros, então o "handler genérico" nunca funcionava; além disso, o método serve de helper e não de handler.
2. Os handlers de `MethodArgumentTypeMismatchException`, `DataIntegrityViolationException`, `AccessDeniedException` e `HttpRequestMethodNotSupportedException` recebiam `HttpStatus...` em vez da exceção; o Spring também não resolve esse parâmetro, então nenhum deles funcionava (o comportamento caía no padrão do Spring).
3. `AccessDeniedException` importada era a de `java.nio.file` (sistema de arquivos), e não a do Spring Security.
4. `IllegalArgumentException` continuava mapeada para 400, mascarando bugs internos como erro do cliente.

**Correção aplicada** (handler reescrito, com `@Slf4j`):
- 400 `BusinessRuleException`, validação de campos, JSON malformado/enum inválido (mensagem específica só quando a causa é uma `BusinessRuleException` nossa; senão texto genérico, sem vazar detalhes do parser), parâmetro ausente e **valor inválido de parâmetro** (ex.: UUID no path).
- 401 `UnauthorizedException`, `BadCredentialsException` ("E-mail ou senha inválidos") e demais `AuthenticationException` ("Não autenticado", sem detalhar o motivo).
- 403 `AccessDeniedException` **do Spring Security**.
- 404 `ResourceNotFoundException`. 409 `DataIntegrityViolationException` (sem detalhes do banco; detalhe só no log). 413 upload grande. **429** `TooManyRequestsException` com header `Retry-After`.
- `IllegalArgumentException` **deixou de ser 400**: cai no handler genérico (500 com log), evitando mascarar bugs.
- Handler genérico `Exception`: exceções do Spring MVC que implementam `ErrorResponse` (rota inexistente 404, 405 com `Allow`, 415, 406...) mantêm status e headers originais com mensagem em português; qualquer outra vira **500 "Erro interno do servidor"** sem detalhes (o stack trace vai para o log).
- `SecurityConfig`: o 401 (antes JSON escrito à mão sem `timestamp`) e o novo 403 do filtro de segurança agora seguem o formato do `ErrorResponseDTO` (`message`, `status`, `timestamp`). `Location` e `Retry-After` foram expostos no CORS.
- Verificado por 13 testes (`GlobalExceptionHandlerTest`): status, mensagens, `Retry-After`, `Allow`, ausência de vazamento de detalhes internos.
**Decisão deliberada:** o formato RFC 7807 (`ProblemDetail`) **não foi adotado**, porque mudaria o corpo de todos os erros (`message`/`status`/`timestamp`) do qual o front depende; pode ser feito junto com a eventual troca do envelope de sucesso (P2-7).

<a id="p1-6"></a>**P1-6. `open-in-view` ativo (padrão)** — ✅ **Resolvido:** `spring.jpa.open-in-view=false` adicionado. Revisão estática: os acessos lazy (`receita.getUsuario()`, `getImagem()`, ingredientes no mapper) ocorrem dentro de métodos `@Transactional` do `ReceitaService`, e `UsuarioService.login` só lê campos simples. **Validar em execução** (um `LazyInitializationException` só apareceria em runtime); novos acessos lazy fora do service voltarão a falhar.

<a id="p1-7"></a>**P1-7. Segurança de autenticação** — ✅ **Resolvido**
- **Rate limit no login** (`LoginRateLimiter`): janela fixa de 15 min; bloqueia após 5 falhas por e-mail ou 20 por IP (`auth.login.max-attempts`, `auth.login.max-attempts-per-ip`, `auth.login.window-minutes`) com **429 + `Retry-After`**. Login bem-sucedido zera o contador do e-mail. Falha de qualquer `AuthenticationException` conta (inclusive e-mail inexistente, sem revelar se a conta existe). Coberto por 7 testes com relógio controlável.
  - *Limitações:* contadores **em memória** (uma instância; zerados no restart) — com várias instâncias usar Redis/gateway; atrás de proxy reverso configurar `server.forward-headers-strategy` para o IP ser o do cliente (senão todos compartilham o IP do proxy e o limite por IP fica agressivo); o bloqueio por e-mail permite que um terceiro trave temporariamente uma conta conhecida (trade-off clássico, limitado a 15 min).
- **Validação de `iss`**: `JwtDecoder` usa `JwtValidators.createDefaultWithIssuer(jwt.issuer)` (assinatura, expiração e emissor). Verificado: token de outro emissor, assinado com outro segredo ou expirado é rejeitado.
- **`sub` = id do usuário** (UUID) em vez do e-mail; o e-mail não vai mais no token. `ReceitaService` busca o usuário por id. **Impacto de deploy:** tokens emitidos antes (sub = e-mail) passam a responder 401 "Token inválido: faça login novamente".
- **Refresh token e revogação** (`RefreshToken`, `RefreshTokenService`, `POST /auth/refresh`, `POST /auth/logout`): valor aleatório de 256 bits; só o **hash SHA-256** fica no banco (tabela `refresh_tokens`, índice por usuário); validade `jwt.refresh-expiration-days` (7); **rotação** a cada uso; **detecção de reutilização** (token já usado → revoga todas as sessões do usuário, gravando a revogação mesmo com o 401 via `noRollbackFor`); lock pessimista na busca para evitar rotação concorrente; limpeza oportunista de tokens expirados na emissão. Coberto por 7 testes com repositório em memória.
  - *Limitações:* o **access token** já emitido continua válido até expirar (60 min) após o logout — para reduzir a janela, baixar `jwt.expiration-minutes` (ex.: 15) agora que existe refresh; não há lista de revogação de access tokens.
- **Senha:** `@Size(max = 72)` (BCrypt trunca acima disso); regex do cadastro `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#_-])[A-Za-z\d@$!%*?&#_-]{8,72}$` (recusa `Abcdefg1`; aceita `#`, `-`, `_`, `@`); `LoginRequestDTO` só exige `@NotBlank` e `@Size(max = 72)`. Uma versão anterior, com `#-_`, era um intervalo e enfraquecia a regra ("Regressão" no topo).
- **Melhorias futuras (não bloqueantes):** política por comprimento (12+) e lista de senhas vazadas; aceitar acentos/espaço exigiria alterar a classe de caracteres da regex; rate limit distribuído.

<a id="p1-8"></a>**P1-8. Ausência de testes** — ✅ **Resolvido.** De 1 teste (`contextLoads`, que falhava sem banco) para **91 testes** e cobertura de **93,4 % das instruções / 71,0 % dos branches**:

| Camada | Classe de teste | Testes | O que cobre |
|---|---|---|---|
| Unitário | `ReceitaServiceTest` | 27 | dono × não-dono × inexistente, duplicidade, upload (JPEG/PNG/WEBP, conteúdo falso, vazio), imagem, autenticação |
| Unitário | `ReceitaMapperTest` | 6 | mapeamento, ingrediente reaproveitado/criado, URL da imagem, atualização parcial |
| Unitário | `GlobalExceptionHandlerTest` | 13 | status, mensagens e headers de cada erro, sem vazamento de detalhes |
| Unitário | `LoginRateLimiterTest` | 7 | bloqueio por e-mail/IP, janela, relógio controlável |
| Unitário | `JwtTest` | 5 | `sub`, `iss`, assinatura, expiração, segredo curto |
| Unitário | `RefreshTokenServiceTest` | 7 | rotação, reutilização, expiração, logout |
| Segurança | `SecurityRoutesTest` | 8 | matriz pública × privada com o filtro real, 401 padronizado, CORS, tokens inválidos, `PUT` multipart, `Location`/204 |
| Arquitetura | `ArchitectureTest` (ArchUnit) | 6 (+1 ignorada) | controller não acessa repository/entidade, dto/domain/repository sem dependências indevidas. A regra "service não usa `org.springframework.http`" está com `@ArchIgnore` até o P2-4 |
| Integração | `ApiFlowIntegrationTest` | 11 | PostgreSQL 16 real (Testcontainers): esquema, fluxos completos, 403, cascata, N+1, refresh, rate limit. **Substitui o `contextLoads`** e não depende mais de `.env` nem de banco local |

- **CI:** `.github/workflows/ci.yml` (Java 21, `./mvnw -B verify`, relatórios de teste e cobertura como artefato). Os runners têm Docker, então a integração roda de verdade; **sem Docker local, o `ApiFlowIntegrationTest` é ignorado** (`@Testcontainers(disabledWithoutDocker = true)`), não falha.
- **JaCoCo** no `verify` (relatório em `target/site/jacoco`), sem limite mínimo imposto.
- Dependências de teste adicionadas: `testcontainers-junit-jupiter`, `testcontainers-postgresql` (gerenciadas pelo Boot) e `archunit-junit5` 1.4.1.
- **Não adotados:** Spotless/Checkstyle (reformataria o código todo; decisão de equipe) e um limite mínimo de cobertura no JaCoCo (definir depois de estabilizar).

### P2 — Médio

<a id="p2-1"></a>**P2-1. `@Data` em entidades** — ✅ **Resolvido.**
**Problema:** `@Data` gerava `equals/hashCode/toString` com associações lazy (`usuario`, `ingrediente`) e campos mutáveis; `Usuario.toString()` incluía o hash da senha; `equals` por campos quebrava `List.remove` em `removeIngrediente`.
**Correção aplicada:**
- Nova `BaseEntity` (`@MappedSuperclass`): `id` UUID gerado e `equals/hashCode` por `id`, seguros para proxies Hibernate (`Hibernate.getClass`, `getId()`); entidade ainda não persistida só é igual a si mesma.
- `Usuario`, `Receitas`, `ReceitaIngrediente`, `Ingrediente` e `ReceitaImagem` estendem `BaseEntity` com `@Getter/@Setter` (sem `@Data`, sem `@AllArgsConstructor`; nenhum código usava esses construtores). O esquema do banco não muda (mesmas colunas).
- `Usuario.toString()` explícito, sem senha; removidas as anotações `@EqualsAndHashCode.Exclude`/`@ToString.Exclude` sem efeito.
- Verificado em teste: iguais quando o `id` coincide, distintos entre tipos, `removeIngrediente` funciona e `toString` de `Usuario` não contém a senha.
**Atenção:** o `hashCode` é constante por classe (padrão recomendado para entidades com id gerado); em `Set` muito grandes de entidades da mesma classe isso degrada, o que não ocorre nos usos atuais (`List`).

<a id="p2-2"></a>**P2-2. Estratégia de ID inconsistente** — ✅ **Resolvido:** `Usuario` usava `@GeneratedValue(generator = "UUID")` sem gerador declarado; agora todas as entidades herdam `GenerationType.UUID` de `BaseEntity`.

<a id="p2-3"></a>**P2-3. Efeito colateral no mapper** — `ReceitaMapper.toReceitaIngrediente` faz `ingredienteRepository.save(...)`. Mapper deve ser puro; mover "find or create" para service, deduplicar ingredientes da mesma receita e resolver todos em uma query (`findByNomeIn`).

<a id="p2-4"></a>**P2-4. Service acoplado a HTTP e a `SecurityContextHolder`** — `getImagemReceita` retorna `ResponseEntity<byte[]>` (camada web no service) e `getAuthenticatedUser()` busca o usuário por e-mail em toda chamada. Retornar `ImagemDTO(dados, contentType)`; usar `@AuthenticationPrincipal Jwt` no controller e passar o `userId` ao service.

<a id="p2-5"></a>**P2-5. Injeção inconsistente** — ✅ **Resolvido:** `ReceitaService` agora usa `@RequiredArgsConstructor` com campos `final`.

<a id="p2-6"></a>**P2-6. Validação de DTO frágil** — ✅ **Resolvido.**
- `@NotNull @NotBlank` redundantes removidos (`@NotBlank` já cobre `null`) em `ReceitaRequestDTO`, `ReceitaIngredienteRequestDTO`, `UsuarioRequestDTO` e `LoginRequestDTO`.
- `@Size(max)` adicionado: `nome`/`tempoPreparo`/`rendimento`/`nomeIngrediente` 255 (limite do `varchar(255)`), `descricao` 5.000, `modoPreparo` 20.000, e-mail e nome de usuário 255. Texto maior agora retorna 400 em vez de 500.
- `categoria` e `unidadeMedida` são enums direto nos DTOs. `Categoria.from` e `UnidadeMedida.from` (`@JsonCreator`) mantêm o comportamento anterior: sem diferenciar maiúsculas/minúsculas, ignoram espaços nas pontas e usam `Locale.ROOT`. Valor inválido → 400 com a lista de valores aceitos (via handler de `HttpMessageNotReadableException`, ver P1-5). `parseEnum` foi removido do mapper.
- `ReceitasUpdateDTO`: "não vazio" passou a ser declarativo (`@Pattern(".*\\S.*")` + `@Size`) e `ingredientes` com `@Size(min = 1)`; o método `naoVazio` e a checagem manual de lista vazia saíram do mapper. Campos ausentes (`null`) continuam sem alteração.
- `List<@NotNull @Valid ReceitaIngredienteRequestDTO>` em criação e atualização: item `null` → 400 em vez de NPE/500.
- Verificado em teste: enum em minúsculas/com espaços, item `null`, `nome` em branco, lista vazia na atualização, limite de 255 e DTO de atualização vazio (válido).
**Observação:** o mapper JPA de `quantidade` (`BigDecimal` sem `@Column(precision, scale)`) usa o padrão do Hibernate (`numeric(38,2)`): frações além de 2 casas seriam arredondadas no banco. Definir a escala desejada (por exemplo `@Digits`/`@Column(scale = 3)`) é uma decisão de produto.

<a id="p2-7"></a>**P2-7. Contrato REST fora do padrão** — ✅ **Resolvido (breaking change).**

| Antes | Agora |
|---|---|
| `POST /api/v1/usuarios/cadastrar` | `POST /api/v1/usuarios` |
| `POST /api/v1/usuarios/login` | `POST /api/v1/auth/login` (novo `AuthController`) |
| `POST /api/v1/receitas/criar` | `POST /api/v1/receitas` (201 + header `Location`) |
| `GET /api/v1/receitas/minhas-receitas` | `GET /api/v1/usuarios/me/receitas` |
| `POST /api/v1/receitas/imagem` (+ `receitaId`) | `PUT /api/v1/receitas/{id}/imagem` |
| `GET /api/v1/receitas/imagem/{id}` | `GET /api/v1/receitas/{id}/imagem` |
| `DELETE /api/v1/receitas/imagem/{id}` | `DELETE /api/v1/receitas/{id}/imagem` |

- `DELETE` de receita e de imagem respondem `204 No Content`; criar receita devolve `Location`.
- `dataCriacao` (receita e usuário): removido o `@JsonFormat("dd/MM/yyyy HH:mm:ss")`; agora ISO-8601 com fuso (`2026-09-25T22:07:21-03:00`, verificado em teste).
- `id` como `UUID` nos DTOs de resposta (o JSON continua sendo texto).
- URL da imagem centralizada em `ReceitaMapper.imagemUrl(UUID)`; `ReceitaService` e o mapper usam a mesma função.
- `SecurityConfig` atualizado para as novas rotas; `API_DOCUMENTATION.md` e `README.md` atualizados (com tabela "antes → agora").
**Decisões deliberadas (não alteradas):**
- O envelope `SuccessResponseDTO` foi **mantido** (trocar por resposta direta reescreve todos os payloads e é decisão conjunta com o front). O `timestamp` do envelope e o de `ErrorResponseDTO` continuam em `dd/MM/yyyy HH:mm:ss`; se o front quiser uniformidade, migrar para ISO-8601 junto com o envelope.
- `POST /api/v1/usuarios` não devolve `Location`, pois ainda não existe `GET /usuarios/{id}` nem `/usuarios/me`.
- Não foram mantidas rotas antigas como *deprecated*: o front precisa migrar de uma vez (ver tabela). Se houver clientes em produção, considerar manter aliases temporários.
**Atenção:** `PUT` com `multipart/form-data` depende de o Spring/Tomcat processar multipart em métodos não-`POST` (suportado no Spring 6+; validar no fluxo manual, §7).

<a id="p2-8"></a>**P2-8. Configuração e perfis** — um único `application.properties`: `logging.level.com.panelamagica=DEBUG` por padrão, Swagger sempre habilitado e liberado no `SecurityConfig`, CORS fixo em `localhost`. Criar perfis `dev`/`prod`, desabilitar springdoc em prod e fornecer `.env.example`. O `.gitignore` tem `.env.` (provável typo de `.env.*`).

<a id="p2-9"></a>**P2-9. Observabilidade** — sem Actuator, sem logs nas operações de negócio, sem correlation-id, sem log de eventos de autenticação.

### P3 — Baixo

- **Nomenclatura:** pacote `entites` → `entities`; `Receitas` → `Receita`; `ReceitasController/Repository/UpdateDTO` vs `ReceitaRequestDTO`; `groupId` `com.PanelaMagica`.
- **`pom.xml`:** blocos vazios `<licenses>`, `<developers>`, `<scm>`, `<url/>`.
- **Java 21:** DTOs como `record`; considerar `spring.threads.virtual.enabled=true`.
- **`login`** faz duas queries de usuário e usa `IllegalStateException` (→ 500).
- **Código morto:** `ReceitaIngredienteRepository.findByReceitaId`; import não usado de `EntityGraph` em `IngredienteRepository`. (O `toLowerCase()` redundante de `UsuarioMapper.toResponseDTO` foi removido junto com o P2-7.)
- **Documentação:** `API_DOCUMENTATION.md` está versionado (o commit diz o contrário) e duplica o Swagger; definir uma fonte única.
- **Imagens:** além do *magic number*, re-encodar com `ImageIO` (remove metadados/poliglotas) e limitar dimensões.

---

## 5. Riscos de negócio a decidir

1. **Leitura pública de receitas** é decisão de produto. Se surgirem receitas privadas, precisa de campo `visibilidade` + regra na query, não só no `SecurityConfig`.
2. **Token inválido/expirado em rota pública:** o resource server responde 401 mesmo em `permitAll` se o cliente enviar `Authorization`. O front deve omitir o header nessas rotas ou tratar o 401.
3. **Ingredientes globais** (`Ingrediente` sem dono): qualquer usuário cria entradas visíveis a todos; sem moderação/normalização (singular/plural, acentos) o catálogo degrada.
4. **`tempoPreparo`/`rendimento` como texto livre** impede filtro e ordenação; avaliar `tempoPreparoMinutos:int` + `porcoes:int`.

---

## 6. Estratégia de testes

A estratégia recomendada originalmente (unitário, segurança, integração com Testcontainers, arquitetura, CI e cobertura) **foi implementada**; o detalhe está em [P1-8](#p1-8). Para evoluir: testes de contrato (OpenAPI) contra o front, testes de carga na listagem pública e, quando o P2-4 for feito, ativar a regra ArchUnit `servicosNaoDependemDaCamadaWeb`.

---|---|---|
| Unitário | JUnit 5 + Mockito | `ReceitaService` (dono, duplicidade, imagem), `ReceitaMapper`, `detectarTipoImagem` |
| Slice web | `@WebMvcTest` + `spring-security-test` | 401 sem token, 403 para não-dono, **matriz de rotas públicas × privadas do `SecurityConfig`**, validação de DTO, formato de erro |
| Integração | `@SpringBootTest` + **Testcontainers Postgres** | fluxo cadastrar → login → criar → upload/leitura/exclusão de imagem → atualizar → deletar; constraints; contagem de queries (detectar N+1) |
| Arquitetura | ArchUnit | camadas (controller não acessa repository; service não importa `org.springframework.http`) |

**Já existem** (sem banco): `GlobalExceptionHandlerTest`, `LoginRateLimiterTest`, `JwtTest` e `RefreshTokenServiceTest`.

Adicionar CI (GitHub Actions: `mvn verify`), JaCoCo e Spotless/Checkstyle. O `contextLoads` deve usar Testcontainers para não depender do ambiente local.

---

## 7. Verificações pendentes

A maior parte do que estava pendente foi **verificado em execução** (ver o topo do documento). Resta:

1. **Migração de dados:** as imagens gravadas na coluna antiga `receitas.imagem` não são migradas pelo `ddl-auto=update`. O `README.md` traz um script SQL (**não testado**) para copiar os dados para `receita_imagem` e preencher `receitas.imagem_id`; testar em uma cópia do banco antes (idealmente via Flyway, P1-3).
2. **Front-end:** ajustar ao novo contrato (rotas do P2-7, `refreshToken`, 403 em vez de 400 para receita alheia, `sub`/login obrigatório após o deploy, `429` com `Retry-After`).
3. **Deploy com proxy reverso / várias instâncias:** configurar `server.forward-headers-strategy` (IP correto no rate limit) e, com mais de uma instância, mover o contador de tentativas para Redis/gateway.
4. **Caso 403 do filtro de segurança** (`accessDeniedHandler`): implementado no mesmo formato de erro, mas sem rota que o exercite hoje (não há autorização por papel); testar se surgirem regras por papel.

---

## 8. Roadmap sugerido

1. **Imediato:** comunicar ao front o novo contrato (P2-7, autenticação e 403), considerar baixar `jwt.expiration-minutes` para ~15 (já existe refresh) e testar o script de migração das imagens em uma cópia do banco.
2. **Sprint 1 (dados):** P1-3 (Flyway com baseline, migração da imagem e tabela `refresh_tokens`, `ddl-auto=validate`), P1-4 (constraints únicas em minúsculas e índices, com tratamento do 409 já pronto no handler), P1-1 (paginação; ver atenção do P0-2).
3. **Sprint 2 (design):** P2-3 (mapper puro / `IngredienteService`), P2-4 (service sem `ResponseEntity`; ativar a regra ArchUnit).
4. **Sprint 3 (operação):** P2-8 (perfis dev/prod, `.env.example`, springdoc desligado em prod), P2-9 (Actuator, logs de negócio, correlation-id), P3.
