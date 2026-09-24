# Documentação da API — Panela Mágica

> ⚠️ Projeto em desenvolvimento. Este documento descreve o comportamento **atual** da API; itens marcados com 🚧 ainda não estão finalizados.

- **URL base:** `http://localhost:8080`
- **Formato:** JSON (`Content-Type: application/json`)
- **Swagger UI:** `http://localhost:8080/panela-magica/swagger-ui.html`

## Sumário

- [Autenticação](#autenticação)
- [Formato das respostas](#formato-das-respostas)
- [Usuários](#usuários)
- [Receitas](#receitas)
- [Valores aceitos (enums)](#valores-aceitos-enums)
- [Códigos de erro](#códigos-de-erro)

---

## Autenticação

A API usa **JWT (Bearer token)**. Fluxo:

1. Cadastre-se em `POST /api/v1/usuarios/cadastrar`.
2. Faça login em `POST /api/v1/usuarios/login` e guarde o `token` recebido.
3. Envie o token em toda rota protegida:

```
Authorization: Bearer <token>
```

| Rota | Acesso |
| --- | --- |
| `POST /api/v1/usuarios/cadastrar` | Pública |
| `POST /api/v1/usuarios/login` | Pública |
| Swagger (`/panela-magica/swagger-ui.html`, `/v3/api-docs`) | Pública |
| `GET /api/v1/receitas` e `GET /api/v1/receitas/{id}` | Pública |
| Todas as demais (`POST`/`DELETE` em `/api/v1/receitas/**`) | **Requer token** |

O token expira em 60 minutos. Token ausente, inválido ou expirado retorna **401**:

```json
{ "message": "Token ausente, inválido ou expirado", "status": 401 }
```

O CORS está liberado para `http://localhost:3000`, `http://localhost:5173` e `http://localhost:4200` (configurável em `cors.allowed-origins`).

---

## Formato das respostas

**Sucesso** (quando a rota usa o envelope padrão):

```json
{
  "status": 200,
  "message": "Mensagem de sucesso",
  "data": { },
  "timestamp": "24/09/2026 14:30:00"
}
```

**Erro de validação ou credenciais:**

```json
{
  "message": "descrição do erro",
  "status": 400,
  "timestamp": "24/09/2026 14:30:00"
}
```

---

## Usuários

### Cadastrar usuário

`POST /api/v1/usuarios/cadastrar` — pública

**Corpo da requisição**

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `nome` | string | sim | não pode ser vazio |
| `email` | string | sim | e-mail válido; deve ser único |
| `senha` | string | sim | mínimo 8 caracteres |

```json
{
  "nome": "Maria Silva",
  "email": "maria@email.com",
  "senha": "senha1234"
}
```

**Resposta `201 Created`**

```json
{
  "status": 201,
  "message": "Usuário cadastrado com sucesso!",
  "data": null,
  "timestamp": "24/09/2026 14:30:00"
}
```

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | Campo inválido, ou e-mail já cadastrado (nesse caso o corpo é apenas o texto da mensagem: `Já existe um usuário cadastrado com este e-mail`) |

---

### Login

`POST /api/v1/usuarios/login` — pública

**Corpo da requisição**

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `email` | string | sim | e-mail válido |
| `senha` | string | sim | mínimo 6 caracteres |

```json
{
  "email": "maria@email.com",
  "senha": "senha1234"
}
```

**Resposta `200 OK`**

```json
{
  "status": 200,
  "message": "Login realizado com sucesso!",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "expiresIn": 3600,
    "usuario": {
      "id": "3f2b8c1e-...",
      "nome": "Maria Silva",
      "email": "maria@email.com",
      "dataCriacao": "24/09/2026 14:00:00"
    }
  },
  "timestamp": "24/09/2026 14:30:00"
}
```

| Campo de `data` | Descrição |
| --- | --- |
| `token` | JWT a ser enviado no header `Authorization` |
| `type` | Sempre `Bearer` |
| `expiresIn` | Validade do token, em segundos |
| `usuario` | Dados do usuário autenticado (a senha nunca é retornada) |

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | Corpo inválido (e-mail mal formatado, senha curta etc.) |
| `401` | `E-mail ou senha inválidos` |

---

## Receitas

A consulta (`GET`) é **pública**: não precisa de token. Criar e deletar **exigem** o header `Authorization: Bearer <token>`.

### Criar receita

`POST /api/v1/receitas/criar`

A receita é associada automaticamente ao usuário dono do token.

**Corpo da requisição**

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `nome` | string | sim | Nome da receita |
| `descricao` | string | sim | Descrição curta |
| `ingredientes` | array | sim | Ao menos 1 item (veja abaixo) |
| `modoPreparo` | string | sim | Passo a passo |
| `tempoPreparo` | string | sim | Ex.: `"45 minutos"` |
| `rendimento` | string | sim | Ex.: `"8 porções"` |
| `categoria` | string | sim | Um valor de [Categoria](#categoria) |
| `unidadeMedida` | string | sim | Um valor de [UnidadeMedida](#unidademedida) |

Cada item de `ingredientes`:

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `nomeIngrediente` | string | sim | não pode ser vazio |
| `quantidade` | número | sim | maior que 0 |
| `unidadeMedida` | string | sim | Um valor de [UnidadeMedida](#unidademedida) |

```json
{
  "nome": "Bolo de cenoura",
  "descricao": "Bolo fofinho com cobertura de chocolate",
  "ingredientes": [
    { "nomeIngrediente": "Cenoura", "quantidade": 3, "unidadeMedida": "UNIDADE" },
    { "nomeIngrediente": "Farinha de trigo", "quantidade": 2, "unidadeMedida": "XICARA" },
    { "nomeIngrediente": "Açúcar", "quantidade": 300, "unidadeMedida": "GRAMA" }
  ],
  "modoPreparo": "Bata a cenoura no liquidificador, misture aos secos e asse por 40 minutos.",
  "tempoPreparo": "1 hora",
  "rendimento": "12 fatias",
  "categoria": "DOCE",
  "unidadeMedida": "GRAMA"
}
```

**Resposta `200 OK`** 🚧 — hoje retorna apenas texto puro, sem o envelope padrão:

```
Receita criada com sucesso
```

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | Validação falhou; o corpo lista os campos, ex.: `nome: must not be blank` |
| `401` | Token ausente, inválido ou expirado |

---

### Listar receitas 🚧

`GET /api/v1/receitas` — pública

**Situação atual:** a rota funciona sem token, mas **ainda não retorna as receitas**. A resposta é apenas o texto:

```
Lista de receitas
```

O retorno com a lista de `ReceitaResponseDTO` (formato abaixo) está previsto.

---

### Buscar receita por ID 🚧

`GET /api/v1/receitas/{id}` — pública

| Parâmetro | Tipo | Descrição |
| --- | --- | --- |
| `id` | UUID (path) | Identificador da receita |

**Situação atual:** retorna apenas o texto `Receita com ID: {id}`, sem os dados da receita.

**Formato previsto da receita (`ReceitaResponseDTO`):**

```json
{
  "id": "9a7c2d10-...",
  "nome": "Bolo de cenoura",
  "descricao": "Bolo fofinho com cobertura de chocolate",
  "ingredientes": [
    { "nomeIngrediente": "Cenoura", "quantidade": 3, "unidadeMedida": "UNIDADE" }
  ],
  "modoPreparo": "...",
  "tempoPreparo": "1 hora",
  "rendimento": "12 fatias",
  "categoria": "DOCE",
  "unidadeMedida": "GRAMA",
  "dataCriacao": "24/09/2026 14:30:00"
}
```

---

### Deletar receita 🚧

`DELETE /api/v1/receitas/{id}`

| Parâmetro | Tipo | Descrição |
| --- | --- | --- |
| `id` | UUID (path) | Identificador da receita |

**Resposta `200 OK`:** `Receita com ID: {id} deletada com sucesso`

**Situação atual:** não verifica se a receita existe nem se pertence ao usuário autenticado. Essas validações estão previstas.

---

## Valores aceitos (enums)

### Categoria

`DOCE`, `SALGADO`, `BEBIDA`, `SOBREMESA`, `OUTROS`

### UnidadeMedida

`GRAMA`, `QUILOGRAMA`, `MILILITRO`, `LITRO`, `UNIDADE`, `XICARA`, `COLHER_SOPA`, `COLHER_CHA`, `A_GOSTO`

> Os campos `categoria` e `unidadeMedida` são enviados como texto; use exatamente os nomes acima, em maiúsculas.

---

## Códigos de erro

| Status | Significado |
| --- | --- |
| `200` | Sucesso |
| `201` | Recurso criado (cadastro de usuário) |
| `400` | Requisição inválida (validação de campos, e-mail já cadastrado) |
| `401` | Não autenticado: credenciais inválidas no login, ou token ausente/inválido/expirado |
| `500` | Erro interno do servidor |

## Exemplo rápido com curl

```bash
# 1. Cadastrar
curl -X POST http://localhost:8080/api/v1/usuarios/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maria Silva","email":"maria@email.com","senha":"senha1234"}'

# 2. Login (copie o "token" da resposta)
curl -X POST http://localhost:8080/api/v1/usuarios/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@email.com","senha":"senha1234"}'

# 3. Listar receitas (pública, não precisa de token)
curl http://localhost:8080/api/v1/receitas

# 4. Rota protegida: deletar exige o token
curl -X DELETE http://localhost:8080/api/v1/receitas/<id> \
  -H "Authorization: Bearer <token>"
```
