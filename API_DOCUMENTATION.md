# Documentação da API — Panela Mágica

> ⚠️ Projeto em desenvolvimento. Este documento descreve o comportamento **atual** da API; rotas e formatos ainda podem mudar.

- **URL base:** `http://localhost:8080`
- **Formato:** JSON (`Content-Type: application/json`); o envio de imagem usa `multipart/form-data` e a leitura de imagem devolve o binário
- **Swagger UI:** `http://localhost:8080/panela-magica/swagger-ui.html`

> **Mudança de contrato (breaking).** As rotas foram reorganizadas para o padrão REST:
>
> | Antes | Agora |
> | --- | --- |
> | `POST /api/v1/usuarios/cadastrar` | `POST /api/v1/usuarios` |
> | `POST /api/v1/usuarios/login` | `POST /api/v1/auth/login` |
> | `POST /api/v1/receitas/criar` | `POST /api/v1/receitas` (retorna `201` + header `Location`) |
> | `GET /api/v1/receitas/minhas-receitas` | `GET /api/v1/usuarios/me/receitas` |
> | `POST /api/v1/receitas/imagem` (`receitaId` no corpo) | `PUT /api/v1/receitas/{id}/imagem` |
> | `GET /api/v1/receitas/imagem/{id}` | `GET /api/v1/receitas/{id}/imagem` |
> | `DELETE /api/v1/receitas/imagem/{id}` | `DELETE /api/v1/receitas/{id}/imagem` |
>
> Também mudaram: `DELETE` de receita e de imagem respondem **`204 No Content`** (sem corpo); `dataCriacao` (receita e usuário) agora é **ISO-8601 com fuso** (ex.: `2026-09-24T14:30:00-03:00`); os `id` continuam sendo UUID em texto; tentar alterar/apagar receita (ou imagem) de **outro usuário** agora responde **`403`** (antes `400`), e usuário não autenticado no service responde `401`.
>
> **Autenticação (novidades):** o login passou a devolver também um `refreshToken`; foram adicionados `POST /api/v1/auth/refresh` e `POST /api/v1/auth/logout`; o `sub` do JWT agora é o **id (UUID) do usuário** (antes era o e-mail), então tokens emitidos antes dessa mudança respondem `401` e exigem novo login; o login tem limite de tentativas (`429`).

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

1. Cadastre-se em `POST /api/v1/usuarios`.
2. Faça login em `POST /api/v1/auth/login` e guarde o `token` (access) e o `refreshToken`.
3. Envie o token em toda rota protegida:

```
Authorization: Bearer <token>
```

| Rota | Acesso |
| --- | --- |
| `POST /api/v1/usuarios` | Pública |
| `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh` e `POST /api/v1/auth/logout` | Pública |
| Swagger (`/panela-magica/swagger-ui.html`, `/v3/api-docs`) | Pública |
| `GET /api/v1/receitas`, `GET /api/v1/receitas/{id}` e `GET /api/v1/receitas/{id}/imagem` | Pública |
| Todas as demais (`POST`/`PATCH`/`PUT`/`DELETE` em `/api/v1/receitas/**` (inclui enviar e deletar imagem) e `GET /api/v1/usuarios/me/receitas`) | **Requer token** |

O token (access) expira em 60 minutos; quando expirar, troque o `refreshToken` (válido por 7 dias) por um novo par em [Renovar token](#renovar-token), sem pedir a senha de novo. O token é assinado com HS256 e só é aceito se o emissor (`iss`) for o da aplicação. Token ausente, inválido ou expirado retorna **401**:

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

`POST /api/v1/usuarios` — pública

**Corpo da requisição**

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `nome` | string | sim | não pode ser vazio; até 255 caracteres |
| `email` | string | sim | e-mail válido; até 255 caracteres; deve ser único |
| `senha` | string | sim | de 8 a 72 caracteres, com ao menos uma letra maiúscula, uma minúscula, um número e um caractere especial (`@ $ ! % * ? & # _ -`); só letras (sem acento), números e esses símbolos são aceitos |

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

`POST /api/v1/auth/login` — pública

**Corpo da requisição**

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `email` | string | sim | e-mail válido |
| `senha` | string | sim | não pode ser vazia; até 72 caracteres (a complexidade só é exigida no cadastro) |

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
    "refreshToken": "kX3n...(43 caracteres)",
    "refreshExpiresIn": 604800,
    "usuario": {
      "id": "3f2b8c1e-...",
      "nome": "Maria Silva",
      "email": "maria@email.com",
      "dataCriacao": "2026-09-24T14:00:00-03:00"
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
| `refreshToken` | Token opaco para obter um novo par de tokens (ver [Renovar token](#renovar-token)). Guarde com segurança: quem o tiver consegue renovar a sessão |
| `refreshExpiresIn` | Validade do refresh token, em segundos |
| `usuario` | Dados do usuário autenticado (a senha nunca é retornada) |

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | Corpo inválido (e-mail mal formatado, senha vazia ou acima de 72 caracteres etc.) |
| `401` | `E-mail ou senha inválidos` |
| `429` | Muitas tentativas malsucedidas: mais de 5 para o mesmo e-mail ou 20 para o mesmo IP em 15 minutos. O header `Retry-After` informa, em segundos, quando tentar de novo. Login bem-sucedido zera o contador do e-mail |

---

### Renovar token

`POST /api/v1/auth/refresh` — pública (o refresh token é a credencial)

**Corpo da requisição**

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `refreshToken` | string | sim | valor recebido no login ou na última renovação; até 200 caracteres |

**Resposta `200 OK`** — mesmo formato do [Login](#login), com um **novo** `token` e um **novo** `refreshToken`. O refresh token enviado deixa de valer (rotação): guarde sempre o mais recente.

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | `refreshToken` ausente ou em branco |
| `401` | `Refresh token inválido` (desconhecido ou já usado) ou `Refresh token expirado` |

> **Reutilização:** apresentar um refresh token que já foi trocado é tratado como possível vazamento: **todas** as sessões do usuário são revogadas e é preciso fazer login de novo.

---

### Logout

`POST /api/v1/auth/logout` — pública

Revoga o refresh token informado. Corpo igual ao de [Renovar token](#renovar-token). Responde **`204 No Content`** sempre, inclusive para token desconhecido ou já revogado.

> O access token já emitido continua válido até expirar (até 60 minutos); descarte-o no cliente.

---

## Receitas

A consulta de receitas e de imagens (`GET`) é **pública**: não precisa de token. Criar, atualizar, deletar, enviar/deletar imagem e listar as próprias receitas (`/usuarios/me/receitas`) **exigem** o header `Authorization: Bearer <token>`.

### Criar receita

`POST /api/v1/receitas`

A receita é associada automaticamente ao usuário dono do token. A resposta traz o header `Location` com a URL da receita criada (`/api/v1/receitas/{id}`).

**Corpo da requisição**

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `nome` | string | sim | Nome da receita (até 255 caracteres) |
| `descricao` | string | sim | Descrição curta (até 5.000 caracteres) |
| `ingredientes` | array | sim | Ao menos 1 item, sem itens `null` (veja abaixo) |
| `modoPreparo` | string | sim | Passo a passo (até 20.000 caracteres) |
| `tempoPreparo` | string | sim | Ex.: `"45 minutos"` (até 255 caracteres) |
| `rendimento` | string | sim | Ex.: `"8 porções"` (até 255 caracteres) |
| `categoria` | string | sim | Um valor de [Categoria](#categoria) |

Cada item de `ingredientes`:

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `nomeIngrediente` | string | sim | não pode ser vazio; até 255 caracteres |
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

**Resposta `201 Created`** — header `Location: /api/v1/receitas/9a7c2d10-...`

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
    "dataCriacao": "2026-09-24T14:30:00-03:00"
  },
  "timestamp": "24/09/2026 14:30:00"
}
```

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | Validação falhou (o corpo lista os campos, ex.: `nome: must not be blank`, ou texto acima do limite de tamanho); JSON malformado; `categoria` ou `unidadeMedida` de um ingrediente inválida (a mensagem lista os valores aceitos); ou já existe uma receita com esse nome para o usuário logado (`Receita já cadastrada para o usuário logado`) |
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
      "imagemUrl": "/api/v1/receitas/9a7c2d10-.../imagem",
      "dataCriacao": "2026-09-24T14:30:00-03:00"
    }
  ],
  "timestamp": "24/09/2026 14:30:00"
}
```

> `imagemUrl` é o caminho para baixar a imagem da receita (veja [Obter imagem](#obter-imagem-da-receita)) e vem `null` enquanto nenhuma imagem foi enviada.

---

### Listar minhas receitas

`GET /api/v1/usuarios/me/receitas` — requer token

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
| `nome` | string | não pode ser vazio (máx. 255); não pode repetir o nome de outra receita do mesmo usuário |
| `descricao` | string | não pode ser vazio (máx. 5.000) |
| `ingredientes` | array | se enviado, **substitui a lista inteira** (não faz merge); ao menos 1 item, com os mesmos campos e regras de [Criar receita](#criar-receita) |
| `modoPreparo` | string | não pode ser vazio (máx. 20.000) |
| `tempoPreparo` | string | não pode ser vazio (máx. 255) |
| `rendimento` | string | não pode ser vazio (máx. 255) |
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
    "dataCriacao": "2026-09-24T14:30:00-03:00"
  },
  "timestamp": "24/09/2026 15:10:00"
}
```

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | Campo enviado em branco (`nome: não pode ser vazio`); `ingredientes` vazio (`ingredientes: A receita deve ter ao menos um ingrediente`); JSON malformado; ingrediente inválido; `categoria` ou `unidadeMedida` inválida (a mensagem lista os valores aceitos); ou nome já usado por outra receita do usuário (`Receita já cadastrada para o usuário logado`) |
| `401` | Token ausente, inválido ou expirado |
| `403` | Receita de outro usuário (`Receita não pertence ao usuário autenticado`) |
| `404` | `Receita não encontrada` |

---

### Deletar receita

`DELETE /api/v1/receitas/{id}` — requer token

| Parâmetro | Tipo | Descrição |
| --- | --- | --- |
| `id` | UUID (path) | Identificador da receita |

Só o usuário que criou a receita pode deletá-la.

**Resposta `204 No Content`** — sem corpo.

**Erros**

| Status | Motivo |
| --- | --- |
| `401` | Token ausente, inválido ou expirado |
| `403` | `Receita não pertence ao usuário autenticado` |
| `404` | `Receita não encontrada` |

---

### Enviar imagem da receita

`PUT /api/v1/receitas/{id}/imagem` — requer token

Envia (ou substitui) a imagem de uma receita já criada. O fluxo é: criar a receita com `POST /api/v1/receitas` e, em seguida, enviar a imagem com o `id` retornado. Só o dono da receita pode enviar.

| Parâmetro | Tipo | Descrição |
| --- | --- | --- |
| `id` | UUID (path) | Identificador da receita |

**Corpo:** `multipart/form-data`

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `file` | arquivo | sim | JPEG, PNG ou WEBP; não vazio; até **5MB** |

O formato é identificado pelo conteúdo do arquivo, não pelo `Content-Type` nem pela extensão enviados.

**Resposta `200 OK`** — `data` é o caminho para obter a imagem:

```json
{
  "status": 200,
  "message": "Imagem enviada com sucesso",
  "data": "/api/v1/receitas/9a7c2d10-.../imagem",
  "timestamp": "24/09/2026 14:30:00"
}
```

**Erros**

| Status | Motivo |
| --- | --- |
| `400` | Arquivo vazio ou ausente; formato inválido (`Formato de imagem inválido. Formatos aceitos: JPEG, PNG e WEBP`) |
| `401` | Token ausente, inválido ou expirado |
| `403` | Receita de outro usuário (`Receita não pertence ao usuário autenticado`) |
| `404` | `Receita não encontrada` |
| `413` | Imagem maior que 5MB (`Imagem muito grande. Tamanho máximo: 5MB`) |

---

### Obter imagem da receita

`GET /api/v1/receitas/{id}/imagem` — pública

| Parâmetro | Tipo | Descrição |
| --- | --- | --- |
| `id` | UUID (path) | Identificador da receita |

**Resposta `200 OK`** — não usa o envelope JSON: o corpo é o binário da imagem, com `Content-Type` `image/jpeg`, `image/png` ou `image/webp`. Pode ser usada direto em `<img src="...">`.

**Erros**

| Status | Motivo |
| --- | --- |
| `404` | `Receita não encontrada` ou `Receita não possui imagem` |

---

### Deletar imagem da receita

`DELETE /api/v1/receitas/{id}/imagem` — requer token

Remove a imagem da receita (a receita em si permanece). Só o dono da receita pode remover.

| Parâmetro | Tipo | Descrição |
| --- | --- | --- |
| `id` | UUID (path) | Identificador da receita |

**Resposta `204 No Content`** — sem corpo.

**Erros**

| Status | Motivo |
| --- | --- |
| `401` | Token ausente, inválido ou expirado |
| `403` | `Receita não pertence ao usuário autenticado` |
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
| `204` | Sucesso sem corpo (deletar receita, deletar imagem) |
| `400` | Requisição inválida ou regra de negócio violada (validação de campos, JSON malformado, e-mail já cadastrado, receita duplicada, atualização com campo vazio, imagem vazia ou em formato inválido) |
| `401` | Não autenticado: credenciais inválidas no login, refresh token inválido/expirado, ou token ausente/inválido/expirado |
| `403` | Sem permissão: a receita (ou a imagem) pertence a outro usuário. Como as receitas são públicas para leitura, não se usa 404 para escondê-las |
| `409` | Conflito de integridade dos dados (ex.: cadastro simultâneo com o mesmo e-mail) |
| `429` | Limite de tentativas de login excedido (ver `Retry-After`) |
| `404` | Recurso não encontrado (receita inexistente ou sem imagem, ou rota inexistente) |
| `413` | Arquivo enviado maior que o limite (5MB) |
| `405` / `415` | Método HTTP ou tipo de conteúdo não suportado pela rota |
| `500` | Erro interno do servidor (mensagem genérica; o detalhe fica apenas no log) |

## Exemplo rápido com curl

```bash
# 1. Cadastrar
curl -X POST http://localhost:8080/api/v1/usuarios \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maria Silva","email":"maria@email.com","senha":"Senha@1234"}'

# 2. Login (copie o "token" da resposta)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@email.com","senha":"Senha@1234"}'

# 2b. Renovar o token (troque pelo "refreshToken" recebido; guarde o novo par)
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'

# 3. Listar receitas (pública, não precisa de token)
curl http://localhost:8080/api/v1/receitas

# 4. Enviar imagem da receita (multipart, exige token)
curl -X PUT http://localhost:8080/api/v1/receitas/<id>/imagem \
  -H "Authorization: Bearer <token>" \
  -F "file=@foto.jpg"

# 5. Baixar a imagem (pública)
curl http://localhost:8080/api/v1/receitas/<id>/imagem --output foto.jpg

# 6. Atualizar parcialmente uma receita (exige token)
curl -X PATCH http://localhost:8080/api/v1/receitas/<id> \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"rendimento":"16 fatias"}'

# 7. Remover só a imagem da receita (exige token)
curl -X DELETE http://localhost:8080/api/v1/receitas/<id>/imagem \
  -H "Authorization: Bearer <token>"

# 8. Deletar a receita inteira (exige token)
curl -X DELETE http://localhost:8080/api/v1/receitas/<id> \
  -H "Authorization: Bearer <token>"
```
