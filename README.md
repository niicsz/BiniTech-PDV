# 🛒 BiniTech PDV

Sistema Frente de Caixa (PDV — Ponto de Venda) desenvolvido com **Arquitetura Hexagonal**, utilizando **Spring Boot 3** no backend e **Angular 21** no frontend.

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Pré-requisitos](#-pré-requisitos)
- [Configuração e Execução](#-configuração-e-execução)
- [Variáveis de Ambiente](#-variáveis-de-ambiente)
- [Autenticação e Autorização](#-autenticação-e-autorização)
- [API Endpoints](#-api-endpoints)
- [Logging e Observabilidade](#-logging-e-observabilidade)
- [Testes](#-testes)
- [Docker](#-docker)
- [Estrutura do Projeto](#-estrutura-do-projeto)

---

## 🎯 Visão Geral

O **BiniTech PDV** é um sistema completo de frente de caixa que permite:

- **Autenticação JWT** — Login com access token e refresh token, logout com invalidação via Redis (blacklist), controle de roles (ADMIN / OPERATOR).
- **Cadastro de produtos** — CRUD completo com código de barras, descrição, preço, preço de custo, estoque, categoria e status ativo/inativo.
- **Cache de produtos** — Resultados de consultas de produtos cacheados no Redis (TTL configurado por operação).
- **Filtro por categoria e busca** — Na listagem de produtos, filtre rapidamente por categoria ou pesquise por descrição/código de barras. Indicadores de produtos ativos e estoque baixo.
- **Tela de PDV** — Leitura rápida por código de barras ou pesquisa por nome do produto (autocompletar), carrinho de compras e finalização de venda.
- **Múltiplas formas de pagamento** — Dinheiro, Cartão de Crédito, Cartão de Débito, PIX e Crediário (Fiado), com suporte a pagamento misto na mesma venda.
- **Crediário / Fiado** — Vendas no crediário com registro de nome e telefone do cliente. Controle completo de devedores com opção de marcar como pago.
- **Gestão de Devedores** — Tela dedicada para visualização de vendas pendentes no crediário, agrupadas por cliente, com indicador de dias em atraso e link direto para WhatsApp.
- **Venda sem estoque** — Ao tentar vender um produto sem estoque suficiente, o sistema oferece a opção de atualizar o estoque automaticamente ou vender mesmo assim (skip stock validation).
- **Alerta de estoque baixo** — Após finalizar uma venda, o sistema alerta automaticamente quando produtos vendidos ficam com estoque abaixo de 5 unidades.
- **Nota impressa com nome do operador** — O comprovante de venda exibe o nome do usuário logado.
- **Relatório de vendas** — Consulta de vendas por data ou período, com cálculo de receita, custo e lucro.
- **Registro de usuários** — Somente administradores podem registrar novos usuários.
- **Personalização Visual** — Suporte nativo a Modo Escuro (Dark Mode) e customização de cores da interface (cor primária e cabeçalho).
- **Logging estruturado** — Logs com sanitização de dados sensíveis em toda a aplicação (backend com SLF4J + `LogSanitizer`, frontend com console estruturado).
- **Virtual Threads** — Habilitadas via Spring Boot 3 + Java 21 para maior throughput em operações de I/O.

---

## 🚀 Tecnologias

### Backend

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.3 |
| Spring Security | — |
| Spring Data MongoDB | — |
| Spring Data Redis | — |
| Spring Boot Actuator | — |
| SLF4J / Logback | — |
| JWT (jjwt) | 0.12.6 |
| BouncyCastle (Argon2) | 1.84 |
| OWASP Java Encoder | 1.3.1 |
| OpenAPI Generator | 7.12.0 |
| MapStruct | 1.6.3 |
| Lombok | 1.18.36 |
| SpringDoc OpenAPI (Swagger UI) | 2.8.6 |
| JaCoCo | 0.8.12 |
| OWASP Dependency Check | 10.0.4 |
| Spotify fmt (code formatter) | 2.25 |

### Frontend

| Tecnologia | Versão |
|---|---|
| Angular | 21 |
| Angular Material | ~21.2.4 |
| Angular CDK | ~21.2.4 |
| TypeScript | ~5.9 |
| RxJS | ~7.8 |

### Infraestrutura

| Tecnologia | Versão |
|---|---|
| MongoDB | 7 |
| Redis | — (instância separada, não inclusa no docker-compose) |
| Docker / Docker Compose | — |
| Node.js (build) | 25 |

---

## 🏗 Arquitetura

O projeto segue a **Arquitetura Hexagonal (Ports & Adapters)**, separando claramente as responsabilidades:

```
┌─────────────────────────────────────────────────────────┐
│                      Frontend (Angular 21)              │
│  Login │ POS Screen │ Products │ Sales │ Debtors        │
└────────────────────────────┬────────────────────────────┘
                             │ HTTP (REST API + JWT)
┌────────────────────────────▼────────────────────────────┐
│                   Adapters (Inbound)                    │
│  AuthController │ ProductController │ SaleController    │
│         GlobalExceptionHandler  │  WebMapper            │
│              AuthenticatedUserProvider                  │
├─────────────────────────────────────────────────────────┤
│                  Application (Use Cases)                │
│  AuthUseCaseImpl │ ProductUseCaseImpl │ SaleUseCaseImpl │
├─────────────────────────────────────────────────────────┤
│                     Ports (Interfaces)                  │
│  Inbound: AuthUseCasePort │ ProductUseCasePort          │
│           SaleUseCasePort                               │
│  Outbound: UserRepositoryPort │ RefreshTokenRepositoryPort │
│            ProductRepositoryPort │ SaleRepositoryPort   │
├─────────────────────────────────────────────────────────┤
│                   Adapters (Outbound)                   │
│  UserRepositoryAdapter │ RefreshTokenRepositoryAdapter  │
│  ProductRepositoryAdapter  │  SaleRepositoryAdapter     │
│             Documents │ PersistenceMapper │ Repositories│
├─────────────────────────────────────────────────────────┤
│                   Config / Security                     │
│  SecurityConfig │ JwtTokenProvider │ JwtAuthFilter      │
│  TokenBlacklistService │ RedisCacheConfig               │
│  CorsConfig │ DataInitializer │ BeanConfig              │
│  PepperedPasswordEncoder │ SpaWebConfig                 │
│  DotenvEnvironmentPostProcessor                         │
└────────────┬─────────────────────────┬──────────────────┘
             │                         │
    ┌────────▼────────┐      ┌─────────▼────────┐
    │    MongoDB 7    │      │      Redis        │
    │  (dados gerais) │      │ (blacklist/cache) │
    └─────────────────┘      └──────────────────┘
```

### Estrutura de pacotes do backend

```
com.binitech.pdv
├── domain/
│   ├── Product, Sale, SaleItem, Payment, User, RefreshToken
│   └── exception/
│       └── BusinessException, ResourceNotFoundException
├── application/
│   ├── ports/
│   │   ├── inbound/   (AuthUseCasePort, ProductUseCasePort, SaleUseCasePort)
│   │   └── outbound/  (UserRepositoryPort, RefreshTokenRepositoryPort,
│   │                    ProductRepositoryPort, SaleRepositoryPort)
│   └── usecases/      (AuthUseCaseImpl, ProductUseCaseImpl, SaleUseCaseImpl)
├── adapters/
│   ├── inbound/web/
│   │   ├── AuthController, ProductController, SaleController
│   │   ├── AuthenticatedUserProvider
│   │   ├── GlobalExceptionHandler
│   │   └── mapper/ (WebMapper)
│   └── outbound/persistence/
│       ├── UserRepositoryAdapter, RefreshTokenRepositoryAdapter
│       ├── ProductRepositoryAdapter, SaleRepositoryAdapter
│       ├── document/
│       ├── mapper/ (PersistenceMapper)
│       └── repository/
├── config/
│   ├── SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter
│   ├── TokenBlacklistService, RedisCacheConfig
│   ├── CorsConfig, BeanConfig, DataInitializer
│   ├── PepperedPasswordEncoder, SpaWebConfig
│   └── DotenvEnvironmentPostProcessor
└── utils/
    ├── LogSanitizer
    └── Enum/ (Role, PaymentMethod)
```

---

## ✅ Pré-requisitos

- **Java 21+**
- **Node.js 25+** e **npm**
- **Docker** e **Docker Compose** (para o MongoDB)
- **Redis** acessível (instância local ou container separado)
- **Maven** (ou use o wrapper `mvnw` incluído)

---

## ⚙ Configuração e Execução

### 1. Subir o banco de dados (MongoDB)

```bash
docker compose up -d
```

O MongoDB ficará acessível em `localhost:27017`.

> **Redis:** O Redis **não** está incluído no `docker-compose.yml`. Configure e inicie uma instância Redis separadamente (local ou via container) e informe a URL na variável `REDIS_URL`.

### 2. Backend (Spring Boot)

```bash
./mvnw clean install

./mvnw spring-boot:run
```

> **Windows:** use `mvnw.cmd` no lugar de `./mvnw`.

O backend estará disponível em **http://localhost:8080**.

### 3. Frontend (Angular)

```bash
cd frontend

npm install

npm start
```

O frontend estará disponível em **http://localhost:4200** e fará proxy das requisições `/api` para o backend na porta `8080`.

> **Produção:** O Dockerfile multi-stage compila o frontend Angular e o serve como arquivos estáticos pelo próprio Spring Boot (via `SpaWebConfig`), eliminando a necessidade de um servidor separado para o frontend.

---

## 🔐 Variáveis de Ambiente

O projeto utiliza variáveis de ambiente para configuração sensível. Você pode defini-las via `.env`, variáveis do sistema ou `docker-compose.yml`:

| Variável | Descrição | Padrão |
|---|---|---|
| `MONGODB_URI` | URI de conexão com o MongoDB | `mongodb://localhost:27017/binitech_pdv` |
| `REDIS_URL` | URL de conexão com o Redis | — (obrigatório) |
| `PORT` | Porta do servidor backend | `8080` |
| `JWT_SECRET` | Chave secreta para assinatura dos tokens JWT | — (obrigatório) |
| `JWT_ACCESS_EXPIRATION` | Tempo de expiração do access token (ms) | `3600000` (1h) |
| `JWT_REFRESH_EXPIRATION` | Tempo de expiração do refresh token (ms) | `604800000` (7d) |
| `ADMIN_USERNAME` | Username do admin criado na inicialização | — (obrigatório) |
| `ADMIN_PASSWORD` | Senha do admin criado na inicialização | — (obrigatório) |
| `SECURITY_PEPPER` | Valor secreto (pepper) concatenado às passwords antes do hash Argon2 | — (obrigatório) |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas pelo CORS | `http://localhost:4200` |

---

## 🔑 Autenticação e Autorização

O sistema utiliza **JWT (JSON Web Tokens)** com **Spring Security** para proteger as rotas da API.

### Roles

| Role | Permissões |
|---|---|
| `ADMIN` | Acesso completo: PDV, produtos, vendas, devedores, registro de usuários |
| `OPERATOR` | Acesso ao PDV, produtos, vendas e devedores (apenas dados próprios) |

### Fluxo de autenticação

1. **Login** (`POST /api/auth/login`) — Retorna `accessToken` e `refreshToken`.
2. O `accessToken` é enviado no header `Authorization: Bearer <token>` em cada requisição.
3. Quando o `accessToken` expira, o frontend usa o `refreshToken` para obter um novo par de tokens via `POST /api/auth/refresh`.
4. **Logout** (`POST /api/auth/logout`) — Invalida o `accessToken` adicionando-o à blacklist no Redis e remove o `refreshToken` do banco.
5. **Registro** (`POST /api/auth/register`) — Somente acessível por usuários `ADMIN`.

### Inicialização

Na primeira execução, o `DataInitializer` cria automaticamente um usuário admin com as credenciais definidas nas variáveis `ADMIN_USERNAME` e `ADMIN_PASSWORD`.

### 🔒 Hashing de Passwords — Argon2id + Pepper

As passwords são protegidas com a técnica mais robusta disponível:

1. **Argon2id** — algoritmo vencedor da Password Hashing Competition, resistente a ataques por GPU e side-channel. Implementado via **BouncyCastle 1.84**. Parâmetros OWASP:
   - Salt: 16 bytes (gerado automaticamente)
   - Hash: 32 bytes
   - Parallelism: 1
   - Memória: 19 456 KiB (~19 MB)
   - Iterações: 2

2. **Pepper** — um valor secreto (`SECURITY_PEPPER`) concatenado à password **antes** do hash pelo `PepperedPasswordEncoder`. Este valor:
   - Vive **apenas** nas variáveis de ambiente do servidor
   - **Nunca** é guardado na base de dados
   - Garante que mesmo com acesso total à BD, os hashes são **inúteis** sem o pepper

> ⚠️ **IMPORTANTE:** Se o pepper for alterado, todas as passwords existentes ficam inválidas e os utilizadores terão de redefinir as suas credenciais.

### 🔴 Blacklist de Tokens (Redis)

O `TokenBlacklistService` armazena no Redis os access tokens invalidados via logout. Cada entrada expira automaticamente quando o próprio token expiraria (`JWT_ACCESS_EXPIRATION`). O `JwtAuthenticationFilter` verifica a blacklist a cada requisição.

### 📦 Cache de Produtos (Redis)

O `RedisCacheConfig` configura caches Redis para operações de leitura de produtos:

| Cache | TTL |
|---|---|
| `products_by_user` | 5 minutos |
| `product_by_id` | 10 minutos |
| `product_by_barcode` | 10 minutos |
| `products_all` | 5 minutos |

---

## 📡 API Endpoints

A API é documentada via **OpenAPI 3.0** (gerado a partir de `swagger.yaml`) e acessível pelo **Swagger UI**:

- 📄 **Swagger UI:** http://localhost:8080/swagger-ui.html
- 📋 **API Docs (JSON):** http://localhost:8080/api-docs

### Auth

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/api/auth/login` | Fazer login | Público |
| `POST` | `/api/auth/register` | Registrar novo usuário | ADMIN |
| `POST` | `/api/auth/refresh` | Renovar access token | Público |
| `POST` | `/api/auth/logout` | Logout e invalidar token | Autenticado |

### Products

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `GET` | `/api/products` | Listar todos os produtos | Autenticado |
| `POST` | `/api/products` | Cadastrar um novo produto | Autenticado |
| `GET` | `/api/products/{id}` | Buscar produto por ID | Autenticado |
| `PUT` | `/api/products/{id}` | Atualizar um produto | Autenticado |
| `DELETE` | `/api/products/{id}` | Remover um produto | Autenticado |
| `GET` | `/api/products/barcode/{barcode}` | Buscar produto por código de barras | Autenticado |

#### Campos do Produto

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `barcode` | string | Sim | Código de barras do produto |
| `description` | string | Sim | Descrição do produto |
| `price` | number | Sim | Preço de venda |
| `costPrice` | number | Não | Preço de custo |
| `stockQuantity` | integer | Sim | Quantidade em estoque |
| `category` | string | Não | Categoria do produto (ex: Bebidas, Alimentos, Limpeza) |
| `active` | boolean | Não | Se o produto está ativo (padrão: `true`) |

### Sales

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/api/sales` | Registrar uma nova venda | Autenticado |
| `GET` | `/api/sales` | Listar vendas (filtro por data/período) | Autenticado |
| `GET` | `/api/sales/{id}` | Buscar venda por ID | Autenticado |
| `GET` | `/api/sales/debtors` | Listar vendas crediário não pagas (devedores) | Autenticado |
| `PATCH` | `/api/sales/{id}/mark-paid` | Marcar venda crediário como paga | Autenticado |

#### Campos da Venda (CreateSaleDTO)

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `items` | array | Sim | Lista de itens da venda (`productId` + `quantity`) |
| `payments` | array | Sim | Lista de pagamentos (`method` + `amount`) |
| `customerName` | string | Não | Nome do cliente (obrigatório para crediário) |
| `customerPhone` | string | Não | Telefone do cliente (obrigatório para crediário) |
| `skipStockValidation` | boolean | Não | Ignorar validação de estoque |

### Formas de Pagamento

- `CASH` — Dinheiro
- `CREDIT_CARD` — Cartão de Crédito
- `DEBIT_CARD` — Cartão de Débito
- `PIX` — PIX
- `CREDIARIO` — Crediário / Fiado

### Actuator

| Endpoint | Acesso |
|---|---|
| `/actuator/health` | Público |
| `/actuator/info` | Público |
| `/actuator/metrics` | ADMIN |

---

## 📊 Logging e Observabilidade

O projeto conta com logging estruturado **com sanitização de dados sensíveis** em todas as camadas.

### LogSanitizer

O utilitário `LogSanitizer` mascara automaticamente dados sensíveis antes de qualquer log:

- `maskUsername(username)` — exibe apenas o primeiro e último caractere (ex: `a***n`)
- `maskId(id)` — exibe apenas os primeiros e últimos 4 caracteres (ex: `abc1***ef90`)

### Backend (SLF4J / Logback)

| Camada | Classe | Logs |
|---|---|---|
| **Controllers** | `AuthController`, `ProductController`, `SaleController` | `INFO` — requisições recebidas e respostas de sucesso |
| **Exception Handler** | `GlobalExceptionHandler` | `WARN` — exceções de negócio, validação, not found, acesso negado; `ERROR` — erros internos |
| **Use Cases** | `AuthUseCaseImpl`, `ProductUseCaseImpl`, `SaleUseCaseImpl` | `INFO` — operações de negócio; `WARN` — falhas de validação, duplicatas; `DEBUG` — consultas |
| **Security** | `JwtAuthenticationFilter`, `JwtTokenProvider` | `WARN` — tokens inválidos ou expirados; `DEBUG` — fluxo de autenticação |
| **Config** | `SecurityConfig`, `DataInitializer`, `BeanConfig` | `INFO` — inicialização de configurações |

### Frontend (Console estruturado)

| Camada | Prefixo | Logs |
|---|---|---|
| **Services** | `[AuthService]`, `[ProductService]`, `[SaleService]` | `console.info` — chamadas à API e respostas |
| **Interceptor** | `[AuthInterceptor]` | `console.warn` — erros 401; `console.error` — falhas de renovação de token |
| **Guards** | `[AuthGuard]`, `[AdminGuard]` | `console.debug` — acesso permitido; `console.warn` — acesso negado |
| **Components** | `[LoginComponent]`, `[PosScreen]`, `[ProductList]`, `[DebtorsList]`, etc. | `console.info` — ações; `console.error` — falhas |

---

## 🧪 Testes

O projeto possui cobertura de testes unitários e de integração com relatório gerado pelo **JaCoCo**.

### Executar os testes

```bash
./mvnw test
```

### Gerar relatório de cobertura (JaCoCo)

```bash
./mvnw verify
```

O relatório HTML é gerado em `target/site/jacoco/index.html`.

### Verificar vulnerabilidades (OWASP Dependency Check)

```bash
./mvnw dependency-check:check
```

A build falha automaticamente se alguma dependência possuir CVE com score ≥ 8.

### Estrutura de testes

```
src/test/java/com/binitech/pdv/
├── adapters/inbound/web/
│   └── GlobalExceptionHandlerTest
├── application/usecases/
│   ├── AuthUseCaseImplTest
│   ├── ProductUseCaseImplTest
│   └── SaleUseCaseImplTest
├── config/
│   ├── JwtAuthenticationFilterTest
│   ├── JwtTokenProviderTest
│   └── PepperedPasswordEncoderTest
├── domain/
│   ├── ProductTest, SaleTest, SaleItemTest, RefreshTokenTest
└── integration/
    ├── AuthControllerIT
    ├── ProductControllerIT
    └── SaleControllerIT
```

> Os testes de integração utilizam **Flapdoodle Embedded MongoDB** e **Spring Boot Test** com `@ActiveProfiles("test")`, sem necessidade de infraestrutura externa.

---

## 🐳 Docker

O projeto inclui um **Dockerfile multi-stage** (3 estágios) que compila frontend e backend em uma única imagem otimizada:

1. **frontend-build** — Compila o Angular com Node.js 25 Alpine
2. **backend-build** — Compila o Spring Boot com Eclipse Temurin JDK 21, copiando o build do frontend para `resources/static/`
3. **runtime** — Imagem final mínima com Eclipse Temurin JRE 21, executando com usuário não-root para segurança

A imagem inclui **HEALTHCHECK** integrado via `/actuator/health`.

### Build da imagem

```bash
docker build -t binitech-pdv .
```

### Subir o MongoDB

```bash
docker compose up -d
```

### Executar a aplicação em container

```bash
docker run -d \
  --name binitech-pdv-app \
  -p 8080:8080 \
  -e MONGODB_URI=mongodb://host.docker.internal:27017/binitech_pdv \
  -e REDIS_URL=redis://host.docker.internal:6379 \
  -e JWT_SECRET=sua-chave-secreta \
  -e JWT_ACCESS_EXPIRATION=3600000 \
  -e JWT_REFRESH_EXPIRATION=604800000 \
  -e ADMIN_USERNAME=admin \
  -e ADMIN_PASSWORD=sua-senha-admin \
  -e SECURITY_PEPPER=seu-pepper-secreto \
  -e CORS_ALLOWED_ORIGINS=http://localhost:8080 \
  binitech-pdv
```

> Em produção, o frontend Angular é servido como arquivos estáticos pelo Spring Boot (via `SpaWebConfig`), sendo acessível diretamente em **http://localhost:8080**.

---

## 📂 Estrutura do Projeto

```
pdv/
    ├── docker-compose.yml          # Sobe o MongoDB (Redis não incluso)
├── Dockerfile                  # Build multi-stage (frontend + backend)
├── owasp-suppressions.xml      # Supressões do OWASP Dependency Check
├── pom.xml                     # Configuração Maven
├── mvnw / mvnw.cmd             # Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/binitech/pdv/
│   │   │   ├── domain/         # Entidades de domínio e exceções
│   │   │   ├── application/    # Portas (inbound/outbound) e casos de uso
│   │   │   ├── adapters/       # Controllers, Repositories, Mappers
│   │   │   ├── config/         # Security, JWT, Redis, CORS, Beans, SpaWebConfig
│   │   │   └── utils/          # LogSanitizer, Enums (Role, PaymentMethod)
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── openapi/swagger.yaml
│   └── test/
│       └── java/com/binitech/pdv/
│           ├── adapters/, application/, config/, domain/
│           └── integration/    # Testes de integração (AuthControllerIT, etc.)
├── frontend/
│   ├── src/app/
│   │   ├── auth/               # Login, Register, Guards, Interceptors
│   │   ├── pos/                # Tela de PDV (carrinho, pagamento, busca)
│   │   │   ├── components/     # PosScreen, CartTable, PaymentModal
│   │   │   └── services/       # ProductService, SaleService
│   │   ├── products/           # Listagem e cadastro de produtos
│   │   ├── sales/              # Relatório de vendas (receita, custo, lucro)
│   │   ├── debtors/            # Gestão de devedores (crediário)
│   │   └── shared/
│   │       ├── components/     # SettingsModal (configurações de aparência)
│   │       ├── models/         # DTOs e modelos (api.models, cart.model)
│   │       └── services/       # ThemeService (dark mode, cores custom)
│   ├── proxy.conf.json
│   └── package.json
└── target/                     # Build artifacts
```

---

## 📝 Autor

Este projeto é desenvolvido por **Nicolas Bezerra Bini**.
