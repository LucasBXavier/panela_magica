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

1. O usuário se cadastra em `POST /api/v1/usuarios/cadastrar`.
2. Faz login em `POST /api/v1/usuarios/login` e recebe um token JWT.
3. Envia o token no header `Authorization: Bearer <token>` nas rotas protegidas (criar, atualizar e deletar receitas, enviar e deletar imagem, e listar as próprias receitas). A consulta de receitas e de imagens (`GET`) é pública.

O token expira em 60 minutos (`jwt.expiration-minutes`).

A senha deve ter no mínimo 8 caracteres, com letra maiúscula, minúscula, número e um caractere especial (`@ $ ! % * ? &`).

## Atualizando um banco existente

O projeto usa `spring.jpa.hibernate.ddl-auto=update`, que cria colunas novas mas **não altera as existentes**. Se a tabela `receitas` foi criada antes do suporte a imagens, ajuste a coluna `imagem` manualmente (a imagem passou a ser opcional e guardada como `bytea`):

```sql
ALTER TABLE receitas DROP COLUMN imagem;
ALTER TABLE receitas ADD COLUMN imagem bytea;
```

Atenção: `DROP COLUMN` apaga as imagens já gravadas. Em um banco novo nada disso é necessário.

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
