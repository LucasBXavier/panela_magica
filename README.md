# 🍲 Panela Mágica

API REST para cadastro e gerenciamento de receitas culinárias, com autenticação de usuários via token JWT.

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
   mvn spring-boot:run
   ```

A API fica disponível em `http://localhost:8080`.
O Swagger UI fica em `http://localhost:8080/panela-magica/swagger-ui.html`.

## Como funciona a autenticação

1. O usuário se cadastra em `POST /api/v1/usuarios/cadastrar`.
2. Faz login em `POST /api/v1/usuarios/login` e recebe um token JWT.
3. Envia o token no header `Authorization: Bearer <token>` nas rotas protegidas (criar e deletar receitas). A consulta de receitas (`GET`) é pública.

O token expira em 60 minutos (`jwt.expiration-minutes`).

## Documentação da API

Veja [API_DOCUMENTATION.md](API_DOCUMENTATION.md) para todas as rotas, corpos de requisição e respostas.

## Status

| Funcionalidade | Situação |
| --- | --- |
| Cadastro de usuário | ✅ Funcionando |
| Login com JWT | ✅ Funcionando |
| Criar receita | ✅ Funcionando |
| Listar / buscar receita por ID | 🚧 Em desenvolvimento (ainda não retorna os dados da receita) |
| Deletar receita | 🚧 Em desenvolvimento (sem verificação de dono ou de existência) |
| Atualizar receita | ⏳ Planejado |

## Licença

Distribuído sob a licença MIT. Veja o arquivo [LICENSE](LICENSE).
