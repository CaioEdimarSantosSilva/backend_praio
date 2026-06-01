# Praiô — Backend

API REST do sistema de monitoramento das 12 praias de Praia Grande SP. Fornece dados de balneabilidade, clima, condições marítimas, qualidade do ar e gerenciamento de usuários.

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.x |
| Maven | 3.9+ |
| MongoDB | Atlas (nuvem) ou local |
| Spring Security | JWT stateless |
| Springdoc OpenAPI (Swagger) | 2.x |
| Lombok | — |
| jjwt | 0.12.x |

**APIs externas consumidas pelo backend:**

- [Open-Meteo](https://open-meteo.com/) — dados climáticos (temperatura, vento, UV, precipitação)
- [Open-Meteo Marine](https://marine-api.open-meteo.com/) — dados marítimos (ondas, maré, temperatura da água)
- [CETESB ArcGIS](https://arcgis.cetesb.sp.gov.br/) — balneabilidade semanal das praias
- [AQICN](https://aqicn.org/) — qualidade do ar via estação CETESB Santos

---

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Conta no MongoDB Atlas (ou instância local) com banco `praio`

---

## Configuração e execução

```bash
# 1. Entrar na pasta backend
cd backend-praio

# 2. Copiar o arquivo de variáveis de ambiente
cp .env.example .env

# 3. Preencher as variáveis no .env (ver seção abaixo)

# 4. Rodar a aplicação
./mvnw spring-boot:run
```

A aplicação sobe na porta **8080** por padrão.

---

## URLs locais

| Recurso | URL |
|---|---|
| API base | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

---

## Variáveis de ambiente (`.env`)

Copie `.env.example` para `.env` e preencha os valores. **Nunca comite o `.env` real.**

| Variável | Obrigatória | Descrição |
|---|---|---|
| `MONGODB_URI` | Sim | URI de conexão com o MongoDB Atlas |
| `JWT_SECRET` | Sim | Segredo HMAC-SHA256 (mín. 32 bytes). Gere com `openssl rand -base64 48` |
| `JWT_EXPIRATION_MS` | Não | Expiração do token em ms (padrão: 604800000 = 7 dias) |
| `API_CETESB_ARCGIS_URL` | Não | URL do Feature Layer ArcGIS da CETESB (padrão já configurado) |
| `CETESB_USUARIO` | Não | Usuário CETESB QUALAR (sem isso, qualidade do ar retorna null) |
| `CETESB_SENHA` | Não | Senha CETESB QUALAR |
| `CETESB_ESTACAO_CODIGO` | Não | Código da estação CETESB (padrão: 72 = Cubatão-Centro) |
| `AQICN_TOKEN` | Não | Token AQICN para qualidade do ar (gratuito em aqicn.org/api) |
| `UPLOADS_DIR` | Não | Diretório para fotos de perfil (padrão: `uploads`) |

---

## Endpoints da API

### Autenticação — `/api/auth`

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Autentica e retorna token JWT |
| POST | `/api/auth/cadastro` | Público | Cria conta e retorna token JWT |

**Login — corpo:**
```json
{ "email": "user@email.com", "senha": "senha123" }
```

**Cadastro — corpo:**
```json
{ "nome": "João", "email": "user@email.com", "senha": "senha123" }
```

**Resposta (login e cadastro):**
```json
{ "token": "eyJ...", "id": "...", "nome": "João", "email": "user@email.com", "roles": ["ROLE_USER"] }
```

---

### Praias — `/api/praias`

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/praias` | Público | Lista as 12 praias (resumo) |
| GET | `/api/praias/{id}` | Público | Detalhes completos de uma praia |
| GET | `/api/praias/balneabilidade/{status}` | Público | Filtra por balneabilidade (`EXCELENTE`, `MUITO_BOA`, `SATISFATORIA`, `PROPRIA`, `IMPROPRIA`, `SEM_DADOS`) |
| GET | `/api/praias/melhores?scoreMinimo=7.0` | Público | Praias com score acima do mínimo |
| POST | `/api/praias/atualizar-balneabilidade` | Público | Força atualização de balneabilidade via CETESB ArcGIS |

---

### Avaliações — `/api/praias/{praiaId}/avaliacoes`

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/praias/{praiaId}/avaliacoes` | Público | Lista avaliações visíveis de uma praia |
| POST | `/api/praias/{praiaId}/avaliacoes` | Autenticado | Cria avaliação (nota 1–5, mensagem, fotos) |

---

### Perfil do usuário — `/api/usuario`

> Requer header `Authorization: Bearer {token}`

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/usuario/perfil` | Dados do usuário autenticado |
| PUT | `/api/usuario/perfil` | Atualiza nome e/ou senha |
| GET | `/api/usuario/avaliacoes` | Lista minhas avaliações |
| PUT | `/api/usuario/avaliacoes/{id}` | Atualiza minha avaliação |
| DELETE | `/api/usuario/avaliacoes/{id}` | Remove minha avaliação |
| POST | `/api/usuario/favoritos/{praiaId}` | Adiciona praia aos favoritos |
| DELETE | `/api/usuario/favoritos/{praiaId}` | Remove praia dos favoritos |

---

### Administração — `/api/admin`

> Requer `Authorization: Bearer {token}` com `ROLE_ADMIN`

**Usuários:**

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/admin/usuarios` | Lista todos os usuários |
| PATCH | `/api/admin/usuarios/{id}/toggle-ativo` | Ativa/desativa usuário |
| DELETE | `/api/admin/usuarios/{id}` | Remove usuário permanentemente |

**Atualizações forçadas (ignora TTL de 30 min):**

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/admin/atualizacoes/clima` | Atualiza dados climáticos (Open-Meteo) |
| POST | `/api/admin/atualizacoes/maritimo` | Atualiza dados marítimos (Open-Meteo Marine) |
| POST | `/api/admin/atualizacoes/ar` | Atualiza qualidade do ar (AQICN/CETESB) |
| POST | `/api/admin/atualizacoes/balneabilidade` | Atualiza balneabilidade (CETESB ArcGIS) |
| POST | `/api/admin/atualizacoes/tudo` | Atualiza clima + marítimo + qualidade do ar |

**Praias:**

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/admin/praias` | Lista todas as praias para edição |
| GET | `/api/admin/praias/{id}` | Dados completos de uma praia |
| PUT | `/api/admin/praias/{id}` | Atualiza nome, coordenadas ou imagem |

**Avaliações:**

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/admin/avaliacoes` | Lista todas as avaliações para moderação |
| PATCH | `/api/admin/avaliacoes/{id}/toggle-ativo` | Oculta/exibe avaliação sem apagar |
| DELETE | `/api/admin/avaliacoes/{id}` | Remove avaliação permanentemente |

---

## Arquitetura

```
src/main/java/com/praio/backend/
├── config/          # CORS, cache, segurança, Swagger, DataInitializer
├── controller/      # Camada REST (BeachController, AuthController, AdminController, ...)
├── service/         # Lógica de negócio e integração com APIs externas
├── repository/      # Interfaces Spring Data MongoDB
├── model/           # Documentos MongoDB (Beach, Usuario, AvaliacaoPraia, ...)
├── dto/             # Data Transfer Objects de request e response
├── security/        # JWT (JwtUtil, JwtAuthFilter, UserDetailsServiceImpl)
└── exception/       # GlobalExceptionHandler, BeachNotFoundException
```

**Fluxo de dados externos:**

1. Na inicialização, o seed insere as 12 praias fixas (se a coleção estiver vazia) e busca a balneabilidade atual da CETESB.
2. Dados climáticos e marítimos são buscados sob demanda (TTL de 30 minutos por praia).
3. Balneabilidade é atualizada automaticamente toda quinta-feira às 08h via cron.
4. Um score composto (0–10) é recalculado a cada atualização com base em balneabilidade, clima, condições marítimas e qualidade do ar.

---

## Praias monitoradas

Canto do Forte, Boqueirão, Guilhermina, Aviação, Tupi, Ocian, Mirim, Maracanã, Caiçara, Real, Flórida, Solemar.
