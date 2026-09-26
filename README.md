# 🍲 Panela Mágica

API REST para cadastro e gerenciamento de receitas culinárias, com autenticação de usuários via token JWT.

## Funcionalidades

- Cadastro e login de usuários (JWT)
- Criar, listar, buscar, atualizar (parcialmente) e deletar receitas, cada uma com seus ingredientes
- Listar apenas as receitas do usuário logado
- Imagem por receita (JPEG, PNG ou WEBP, até 5MB), armazenada no próprio banco de dados

> ⚠️ **Projeto em desenvolvimento.** Funcionalidades, rotas e formatos de resposta ainda podem mudar sem aviso. Nem todos os endpoints estão finalizados (veja [Status](#status)).

## Tecnologias

- Java 21
- Spring Boot 4 (Web, Data JPA, Validation, Security)
- Autenticação stateless com JWT (Spring Security OAuth2 Resource Server, HS256)
- PostgreSQL 16 (via Docker)
- Swagger / OpenAPI (springdoc)
- Lombok e Maven

## Pré-requisitos

- JDK 21
- Maven (ou o `mvnw` do projeto)
- Docker e Docker Compose

## Como executar

1. Crie um arquivo `.env` na raiz do projeto (ele já está no `.gitignore`):

   ```env
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/panela_magica
   SPRING_DATASOURCE_USERNAME=seu_usuario
   SPRING_DATASOURCE_PASSWORD=sua_senha
   JWT_SECRET=uma-chave-secreta-com-no-minimo-32-caracteres
   ```

   Variáveis opcionais: `POSTGRES_DB` (padrão `panela_magica`) e `POSTGRES_PORT` (padrão `5433`).

2. Suba o banco de dados:

   ```bash
   docker compose up -d
   ```

3. Inicie a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

   (no Windows, use `mvnw.cmd spring-boot:run`; se tiver o Maven instalado, `mvn spring-boot:run` também funciona)

A API fica disponível em `http://localhost:8080`.
O Swagger UI fica em `http://localhost:8080/panela-magica/swagger-ui.html`.

## Como funciona a autenticação

1. O usuário se cadastra em `POST /api/v1/usuarios`.
2. Faz login em `POST /api/v1/auth/login` e recebe um token JWT (access) e um `refreshToken`.
3. Envia o token no header `Authorization: Bearer <token>` nas rotas protegidas (criar, atualizar e deletar receitas, enviar e deletar imagem, e listar as próprias receitas). A consulta de receitas e de imagens (`GET`) é pública.

O token expira em 60 minutos (`jwt.expiration-minutes`). Para renovar sem pedir a senha, use `POST /api/v1/auth/refresh` com o `refreshToken` (válido por `jwt.refresh-expiration-days`, 7 dias; cada uso o troca por um novo) e `POST /api/v1/auth/logout` para revogá-lo. O JWT só é aceito se o emissor for `jwt.issuer` e seu `sub` é o id do usuário.

O login limita tentativas malsucedidas (`auth.login.max-attempts`, `auth.login.max-attempts-per-ip`, `auth.login.window-minutes`); o excesso retorna `429` com `Retry-After`. Os contadores ficam em memória (uma instância); atrás de proxy reverso configure `server.forward-headers-strategy` para que o IP seja o do cliente.

A senha deve ter de 8 a 72 caracteres, com letra maiúscula, minúscula, número e um caractere especial (`@ $ ! % * ? & # _ -`).

## Atualizando um banco existente

O projeto usa `spring.jpa.hibernate.ddl-auto=update`, que cria tabelas e colunas novas mas **não altera nem remove as existentes**. A imagem da receita agora fica na tabela `receita_imagem` (ligada por `receitas.imagem_id`), e não mais nas colunas `receitas.imagem` / `receitas.imagem_content_type`.

Em um banco novo nada precisa ser feito. Em um banco que já tem imagens, **faça backup**, suba a aplicação uma vez (para o Hibernate criar `receita_imagem` e `receitas.imagem_id`) e então migre os dados (script não testado automaticamente — revise antes de executar):

```sql
ALTER TABLE receita_imagem ADD COLUMN receita_id_tmp uuid;

INSERT INTO receita_imagem (id, dados, content_type, receita_id_tmp)
SELECT gen_random_uuid(), imagem, imagem_content_type, id
FROM receitas
WHERE imagem IS NOT NULL;

UPDATE receitas r
SET imagem_id = ri.id
FROM receita_imagem ri
WHERE ri.receita_id_tmp = r.id;

ALTER TABLE receita_imagem DROP COLUMN receita_id_tmp;
ALTER TABLE receitas DROP COLUMN imagem, DROP COLUMN imagem_content_type;
```

## Documentação da API

Veja [API_DOCUMENTATION.md](API_DOCUMENTATION.md) para todas as rotas, corpos de requisição e respostas.

## Status

| Funcionalidade | Situação |
| --- | --- |
| Cadastro de usuário | ✅ Funcionando |
| Login com JWT | ✅ Funcionando |
| Criar receita | ✅ Funcionando (bloqueia nome duplicado por usuário) |
| Listar / buscar receita por ID | ✅ Funcionando (públicas) |
| Deletar receita | ✅ Funcionando (só o dono pode deletar) |
| Listar minhas receitas | ✅ Funcionando (requer token) |
| Imagem da receita (enviar / obter / deletar) | ✅ Funcionando (JPEG, PNG e WEBP até 5MB, salva no banco) |
| Atualizar receita (parcial) | ✅ Funcionando (só o dono pode atualizar) |

## Licença

Distribuído sob a licença MIT. Veja o arquivo [LICENSE](LICENSE).
