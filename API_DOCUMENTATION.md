# Documentação da API — Panela Mágica

> ⚠️ Projeto em desenvolvimento. Este documento descreve o comportamento **atual** da API; rotas e formatos ainda podem mudar.

- **URL base:** `http://localhost:8080`
- **Formato:** JSON (`Content-Type: application/json`); o envio de imagem usa `multipart/form-data` e a leitura de imagem devolve o binário
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
| `GET /api/v1/receitas`, `GET /api/v1/receitas/{id}` e `GET /api/v1/receitas/imagem/{receitaId}` | Pública |
| Todas as demais (`POST`/`PATCH`/`DELETE` em `/api/v1/receitas/**` (inclui enviar e deletar imagem) e `GET /api/v1/receitas/minhas-receitas`) | **Requer token** |

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
| `senha` | string | sim | mínimo 8 caracteres, com ao menos uma letra maiúscula, uma minúscula, um número e um caractere especial (`@ $ ! % * ? &`); outros símbolos são rejeitados |

```json
{
  "nome": "Maria Silva",
  "email": "maria@email.com",
  "senha": "Senha@1234"
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
| `400` | Campo inválido, ou e-mail já cadastrado (`Já existe um usuário cadastrado com este e-mail`) |

---

### Login

`POST /api/v1/usuarios/login` — pública

**Corpo da requisição**

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `email` | string | sim | e-mail válido |
| `senha` | string | sim | mesmas regras da senha do cadastro (mínimo 8 caracteres, maiúscula, minúscula, número e um dos especiais `@ $ ! % * ? &`) |

```json
{
  "email": "maria@email.com",
  "senha": "Senha@1234"
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
| `400` | Corpo inválido (e-mail mal formatado, senha fora das regras acima etc.) |
| `401` | `E-mail ou senha inválidos` |

---

## Receitas

A consulta de receitas e de imagens (`GET`) é **pública**: não precisa de token. Criar, atualizar, deletar, enviar/deletar imagem e listar as próprias receitas (`minhas-receitas`) **exigem** o header `Authorization: Bearer <token>`.

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
  "categoria": "DOCE"
}
```

**Resposta `201 Created`**

```json
{
  "status": 201,
  "message": "Receita criada com sucesso",
  "data": {
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
    "imagemUrl": null,
    "dataCriacao": "24/09/2026 14:30:00"
  },
  "timestamp": "24/09/2026 14:30:00"
}
```

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | Validação falhou (o corpo lista os campos, ex.: `nome: must not be blank`); `categoria` ou `unidadeMedida` de um ingrediente inválida (a mensagem lista os valores aceitos); ou já existe uma receita com esse nome para o usuário logado (`Receita já cadastrada para o usuário logado`) |
| `401` | Token ausente, inválido ou expirado |

> Os campos `categoria` e `unidadeMedida` (dos ingredientes) aceitam maiúsculas ou minúsculas (`doce` e `DOCE` são equivalentes).

---

### Listar receitas

`GET /api/v1/receitas` — pública

**Resposta `200 OK`** — `data` é uma lista de receitas (vazia se não houver nenhuma):

```json
{
  "status": 200,
  "message": "Receitas listadas com sucesso",
  "data": [
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
      "imagemUrl": "/api/v1/receitas/imagem/9a7c2d10-...",
      "dataCriacao": "24/09/2026 14:30:00"
    }
  ],
  "timestamp": "24/09/2026 14:30:00"
}
```

> `imagemUrl` é o caminho para baixar a imagem da receita (veja [Obter imagem](#obter-imagem-da-receita)) e vem `null` enquanto nenhuma imagem foi enviada.

---

### Listar minhas receitas

`GET /api/v1/receitas/minhas-receitas` — requer token

Retorna apenas as receitas criadas pelo usuário dono do token. Mesmo formato da listagem geral, com a mensagem `Minhas receitas listadas com sucesso`.

**Erros**

| Status | Motivo |
| --- | --- |
| `401` | Token ausente, inválido ou expirado |

---

### Buscar receita por ID

`GET /api/v1/receitas/{id}` — pública

| Parâmetro | Tipo | Descrição |
| --- | --- | --- |
| `id` | UUID (path) | Identificador da receita |

**Resposta `200 OK`** — mesmo formato de uma receita da listagem, dentro de `data`, com a mensagem `Receita encontrada`.

**Erros**

| Status | Motivo |
| --- | --- |
| `404` | `Receita não encontrada` |

---

### Atualizar receita

`PATCH /api/v1/receitas/{id}` — requer token

Atualização **parcial**: só os campos enviados são alterados; os omitidos mantêm o valor atual. Só o usuário que criou a receita pode atualizá-la. A imagem não é alterada aqui: use [Enviar imagem](#enviar-imagem-da-receita).

| Parâmetro | Tipo | Descrição |
| --- | --- | --- |
| `id` | UUID (path) | Identificador da receita |

**Corpo da requisição** — todos os campos são opcionais, mas os enviados não podem ser vazios:

| Campo | Tipo | Regras |
| --- | --- | --- |
| `nome` | string | não pode ser vazio; não pode repetir o nome de outra receita do mesmo usuário |
| `descricao` | string | não pode ser vazio |
| `ingredientes` | array | se enviado, **substitui a lista inteira** (não faz merge); ao menos 1 item, com os mesmos campos e regras de [Criar receita](#criar-receita) |
| `modoPreparo` | string | não pode ser vazio |
| `tempoPreparo` | string | não pode ser vazio |
| `rendimento` | string | não pode ser vazio |
| `categoria` | string | Um valor de [Categoria](#categoria) |

```json
{
  "nome": "Bolo de cenoura com cobertura",
  "rendimento": "16 fatias",
  "ingredientes": [
    { "nomeIngrediente": "Cenoura", "quantidade": 4, "unidadeMedida": "UNIDADE" },
    { "nomeIngrediente": "Chocolate em pó", "quantidade": 5, "unidadeMedida": "COLHER_SOPA" }
  ]
}
```

**Resposta `200 OK`** — `data` traz a receita já atualizada, no mesmo formato de [Buscar receita por ID](#buscar-receita-por-id):

```json
{
  "status": 200,
  "message": "Receita atualizada com sucesso",
  "data": {
    "id": "9a7c2d10-...",
    "nome": "Bolo de cenoura com cobertura",
    "descricao": "Bolo fofinho com cobertura de chocolate",
    "ingredientes": [
      { "nomeIngrediente": "Cenoura", "quantidade": 4, "unidadeMedida": "UNIDADE" },
      { "nomeIngrediente": "Chocolate em pó", "quantidade": 5, "unidadeMedida": "COLHER_SOPA" }
    ],
    "modoPreparo": "...",
    "tempoPreparo": "1 hora",
    "rendimento": "16 fatias",
    "categoria": "DOCE",
    "imagemUrl": null,
    "dataCriacao": "24/09/2026 14:30:00"
  },
  "timestamp": "24/09/2026 15:10:00"
}
```

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | Campo enviado em branco (`nome não pode ser vazio`); `ingredientes` vazio (`A receita deve ter ao menos um ingrediente`); ingrediente inválido; `categoria` ou `unidadeMedida` inválida (a mensagem lista os valores aceitos); nome já usado por outra receita do usuário (`Receita já cadastrada para o usuário logado`); ou receita de outro usuário (`Receita não pertence ao usuário autenticado`) |
| `401` | Token ausente, inválido ou expirado |
| `404` | `Receita não encontrada` |

---

### Deletar receita

`DELETE /api/v1/receitas/{id}` — requer token

| Parâmetro | Tipo | Descrição |
| --- | --- | --- |
| `id` | UUID (path) | Identificador da receita |

Só o usuário que criou a receita pode deletá-la.

**Resposta `200 OK`**

```json
{
  "status": 200,
  "message": "Receita deletada com sucesso",
  "data": null,
  "timestamp": "24/09/2026 14:30:00"
}
```

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | `Receita não pertence ao usuário autenticado` |
| `401` | Token ausente, inválido ou expirado |
| `404` | `Receita não encontrada` |

---

### Enviar imagem da receita

`POST /api/v1/receitas/imagem` — requer token

Envia (ou substitui) a imagem de uma receita já criada. O fluxo é: criar a receita em `/criar` e, em seguida, enviar a imagem com o `id` retornado. Só o dono da receita pode enviar.

**Corpo:** `multipart/form-data`

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `file` | arquivo | sim | JPEG, PNG ou WEBP; não vazio; até **5MB** |
| `receitaId` | UUID | sim | `id` da receita |

O formato é identificado pelo conteúdo do arquivo, não pelo `Content-Type` nem pela extensão enviados.

**Resposta `200 OK`** — `data` é o caminho para obter a imagem:

```json
{
  "status": 200,
  "message": "Imagem enviada com sucesso",
  "data": "/api/v1/receitas/imagem/9a7c2d10-...",
  "timestamp": "24/09/2026 14:30:00"
}
```

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | Arquivo vazio ou ausente; formato inválido (`Formato de imagem inválido. Formatos aceitos: JPEG, PNG e WEBP`); parâmetro obrigatório ausente; ou receita de outro usuário (`Receita não pertence ao usuário autenticado`) |
| `401` | Token ausente, inválido ou expirado |
| `404` | `Receita não encontrada` |
| `413` | Imagem maior que 5MB (`Imagem muito grande. Tamanho máximo: 5MB`) |

---

### Obter imagem da receita

`GET /api/v1/receitas/imagem/{receitaId}` — pública

| Parâmetro | Tipo | Descrição |
| --- | --- | --- |
| `receitaId` | UUID (path) | Identificador da receita |

**Resposta `200 OK`** — não usa o envelope JSON: o corpo é o binário da imagem, com `Content-Type` `image/jpeg`, `image/png` ou `image/webp`. Pode ser usada direto em `<img src="...">`.

**Erros**

| Status | Motivo |
| --- | --- |
| `404` | `Receita não encontrada` ou `Receita não possui imagem` |

---

### Deletar imagem da receita

`DELETE /api/v1/receitas/imagem/{receitaId}` — requer token

Remove a imagem da receita (a receita em si permanece). Só o dono da receita pode remover.

| Parâmetro | Tipo | Descrição |
| --- | --- | --- |
| `receitaId` | UUID (path) | Identificador da receita |

**Resposta `200 OK`**

```json
{
  "status": 200,
  "message": "Imagem deletada com sucesso",
  "data": null,
  "timestamp": "24/09/2026 14:30:00"
}
```

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | `Receita não pertence ao usuário autenticado` |
| `401` | Token ausente, inválido ou expirado |
| `404` | `Receita não encontrada` ou `Receita não possui imagem para deletar` |

---

## Valores aceitos (enums)

### Categoria

`DOCE`, `SALGADO`, `BEBIDA`, `SOBREMESA`, `OUTROS`

### UnidadeMedida

`GRAMA`, `QUILOGRAMA`, `MILILITRO`, `LITRO`, `UNIDADE`, `XICARA`, `COLHER_SOPA`, `COLHER_CHA`, `A_GOSTO`

> Os campos `categoria` e `unidadeMedida` são enviados como texto. Os nomes acima são os valores canônicos; a API também aceita minúsculas.

---

## Códigos de erro

| Status | Significado |
| --- | --- |
| `200` | Sucesso |
| `201` | Recurso criado (cadastro de usuário, criação de receita) |
| `400` | Requisição inválida ou regra de negócio violada (validação de campos, e-mail já cadastrado, receita duplicada, receita de outro usuário, atualização com campo vazio, imagem vazia ou em formato inválido) |
| `401` | Não autenticado: credenciais inválidas no login, ou token ausente/inválido/expirado |
| `404` | Recurso não encontrado (receita inexistente ou sem imagem) |
| `413` | Arquivo enviado maior que o limite (5MB) |
| `500` | Erro interno do servidor |

## Exemplo rápido com curl

```bash
# 1. Cadastrar
curl -X POST http://localhost:8080/api/v1/usuarios/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maria Silva","email":"maria@email.com","senha":"Senha@1234"}'

# 2. Login (copie o "token" da resposta)
curl -X POST http://localhost:8080/api/v1/usuarios/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@email.com","senha":"Senha@1234"}'

# 3. Listar receitas (pública, não precisa de token)
curl http://localhost:8080/api/v1/receitas

# 4. Enviar imagem da receita (multipart, exige token)
curl -X POST http://localhost:8080/api/v1/receitas/imagem \
  -H "Authorization: Bearer <token>" \
  -F "receitaId=<id>" \
  -F "file=@foto.jpg"

# 5. Baixar a imagem (pública)
curl http://localhost:8080/api/v1/receitas/imagem/<id> --output foto.jpg

# 6. Atualizar parcialmente uma receita (exige token)
curl -X PATCH http://localhost:8080/api/v1/receitas/<id> \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"rendimento":"16 fatias"}'

# 7. Remover só a imagem da receita (exige token)
curl -X DELETE http://localhost:8080/api/v1/receitas/imagem/<id> \
  -H "Authorization: Bearer <token>"

# 8. Deletar a receita inteira (exige token)
curl -X DELETE http://localhost:8080/api/v1/receitas/<id> \
  -H "Authorization: Bearer <token>"
```
