# 🛒 BiniTech PDV

Plataforma **SaaS multi-tenant** de Frente de Caixa (PDV — Ponto de Venda), desenvolvida com **Arquitetura Hexagonal**, utilizando **Spring Boot 3** no backend e **Angular 21** no frontend. Cada lojista opera em um *tenant* isolado, com cobrança recorrente via **Stripe**.

> Este repositório contém **apenas o backend** (API REST). O frontend Angular vive em repositório próprio e é deployado separadamente: **[niicsz/BiniTech-PDV-frontend](https://github.com/niicsz/BiniTech-PDV-frontend)**. O backend expõe a API em `/api/**` e libera o domínio do frontend via CORS (`CORS_ALLOWED_ORIGINS`).

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Multi-Tenancy](#-multi-tenancy)
- [Billing e Planos](#-billing-e-planos)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Pré-requisitos](#-pré-requisitos)
- [Configuração e Execução](#-configuração-e-execução)
- [Variáveis de Ambiente](#-variáveis-de-ambiente)
- [Autenticação e Autorização](#-autenticação-e-autorização)
- [API Endpoints](#-api-endpoints)
- [Mensageria e E-mail](#-mensageria-e-e-mail)
- [Logging e Observabilidade](#-logging-e-observabilidade)
- [Testes](#-testes)
- [Docker](#-docker)
- [Estrutura do Projeto](#-estrutura-do-projeto)

---

## 🎯 Visão Geral

O **BiniTech PDV** é uma plataforma SaaS de frente de caixa onde cada cliente (lojista) opera em um **tenant** próprio. As funcionalidades incluem:

### PDV e Vendas
- **Tela de PDV** — Leitura rápida por código de barras ou pesquisa por nome do produto (autocompletar), carrinho de compras e finalização de venda.
- **Múltiplas formas de pagamento** — Dinheiro, Cartão de Crédito, Cartão de Débito, PIX e Crediário (Fiado), com suporte a pagamento misto na mesma venda.
- **Crediário / Fiado** — Vendas no crediário com registro de nome e telefone do cliente. Controle completo de devedores com opção de marcar como pago.
- **Gestão de Devedores** — Tela dedicada para vendas pendentes no crediário, agrupadas por cliente, com indicador de dias em atraso e link direto para WhatsApp.
- **Venda sem estoque** — Ao tentar vender um produto sem estoque suficiente, o sistema oferece atualizar o estoque automaticamente ou vender mesmo assim (`skipStockValidation`).
- **Alerta de estoque baixo** — Após finalizar uma venda, alerta automaticamente quando produtos vendidos ficam com estoque abaixo de 5 unidades.
- **Nota impressa com nome do operador** — O comprovante de venda exibe o nome do usuário logado.
- **Relatório de vendas** — Consulta por data ou período, com cálculo de receita, custo, lucro e valor por método de pagamento.

### Produtos
- **Cadastro de produtos** — CRUD completo com código de barras, descrição, preço, preço de custo, estoque, categoria e status ativo/inativo.
- **Cache de produtos** — Resultados de consultas cacheados no Redis (TTL configurado por operação).
- **Filtro por categoria e busca** — Filtre por categoria ou pesquise por descrição/código de barras, com indicadores de produtos ativos e estoque baixo.

### Plataforma / SaaS
- **Auto-cadastro de lojas (tenants)** — Registro público de loja, com aprovação por um Super Admin.
- **Ciclo de vida do tenant** — `PENDING_APPROVAL → ACTIVE → BLOCKED/CANCELLED`, com bloqueio automático por inadimplência.
- **Assinaturas e cobrança** — Planos pagos via Stripe (Checkout + Customer Portal) e cobrança diária baseada em uso.
- **Gestão de usuários por tenant** — `TENANT_ADMIN` registra operadores da própria loja; `SUPER_ADMIN` administra a plataforma.
- **Recuperação de senha** — Fluxos de "esqueci minha senha" e troca de senha, com envio de e-mail assíncrono.

### Transversal
- **Autenticação JWT** — Login com access token e refresh token, logout com invalidação via Redis (blacklist), controle de roles.
- **Personalização Visual** — Modo Escuro (Dark Mode) e customização de cores da interface (cor primária e cabeçalho).
- **Logging estruturado** — Logs com sanitização de dados sensíveis (backend com SLF4J + `LogSanitizer`, frontend com console estruturado).
- **Virtual Threads** — Habilitadas via Spring Boot 3 + Java 21 para maior throughput em operações de I/O.

---

## 🏢 Multi-Tenancy

Cada loja é um **tenant**, identificado por um `slug` único. Os dados (produtos, vendas, usuários) são isolados por `tenantId`.

| Conceito | Descrição |
|---|---|
| **Tenant** | A loja. Possui `name`, `slug`, `status`, `planId`, `billingEmail` e datas de trial/bloqueio. |
| **Status do Tenant** | `PENDING_APPROVAL`, `ACTIVE`, `BLOCKED`, `CANCELLED` (enum `TenantStatus`). |
| **Super Admin** | Usuário sem `tenantId` (`role = SUPER_ADMIN`), criado na inicialização. Aprova, bloqueia e ativa tenants. |
| **TenantValidationFilter** | Filtro que bloqueia requisições de tenants com status `BLOCKED`/`CANCELLED`, retornando código `TENANT_BLOCKED`. |

Fluxo típico:

1. Lojista faz auto-cadastro via `POST /api/public/tenants` (status inicial `PENDING_APPROVAL`).
2. Um `SUPER_ADMIN` aprova via `POST /api/admin/tenants/{id}/approve` — dispara e-mail de aprovação (RabbitMQ).
3. O tenant assina um plano (Stripe Checkout) e passa a `ACTIVE`.
4. Inadimplência ou cancelamento move o tenant para `BLOCKED`/`CANCELLED`.

---

## 💳 Billing e Planos

A cobrança é feita via **Stripe** (Checkout Sessions + Customer Portal + Webhooks). A `Subscription` espelha o estado da assinatura do Stripe, e cada ciclo gera uma `Invoice`.

### Planos (`PlanConfig`)

| Plano | Mensalidade base | Máx. produtos | Máx. vendas/mês | Máx. operadores |
|---|---|---|---|---|
| `starter` | R$ 99,00 | 200 | 300 | 1 |
| `pro` | R$ 199,00 | 500 | 1000 | 3 |
| `enterprise` | R$ 349,00 | 2000 | 5000 | 10 |
| `free` | R$ 0,00 | ilimitado | ilimitado | ilimitado |

> O plano `free` é uma cortesia vitalícia (sem limites). Os demais são cobrados via Stripe.

### Cobrança por excedente

Quando o uso ultrapassa os limites do plano, a fatura soma um valor de excedente:

| Excedente | Valor |
|---|---|
| Por produto acima do limite | R$ 0,10 |
| Por venda acima do limite | R$ 0,05 |
| Por operador acima do limite | R$ 30,00 |

### Estados da assinatura e da fatura

- `SubscriptionStatus`: `PENDING`, `ACTIVE`, `PAST_DUE`, `CANCELLED`
- `InvoiceStatus`: `PENDING`, `PAID`, `OVERDUE`, `CANCELLED`

### Processos

- **Stripe Webhooks** (`POST /webhooks/stripe`) — Sincronizam pagamentos, falhas e cancelamentos vindos do Stripe.
- **DailyBillingJob** — Job agendado (`@Scheduled`, diário à meia-noite) que avalia o uso, gera faturas e aplica bloqueios por inadimplência.

---

## 🚀 Tecnologias

### Backend

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.12 |
| Spring Security | 6.5.x |
| Spring Data MongoDB | — |
| Spring Data Redis | — |
| Spring AMQP (RabbitMQ) | 3.2.x |
| Spring Boot Mail | — |
| Spring Boot Actuator | — |
| Stripe Java SDK | 29.2.0 |
| SLF4J / Logback | — |
| JWT (jjwt) | 0.12.6 |
| BouncyCastle (Argon2) | 1.84 |
| OWASP Java Encoder | 1.3.1 |
| OpenAPI Generator | 7.12.0 |
| MapStruct | 1.6.3 |
| Lombok | 1.18.36 |
| SpringDoc OpenAPI (Swagger UI) | 2.8.17 |
| JaCoCo | 0.8.12 |
| OWASP Dependency Check | 12.1.1 |
| Spotify fmt (code formatter) | 2.25 |

### Frontend

| Tecnologia | Versão |
|---|---|
| Angular | 21 |
| Angular Material / CDK | ~21.2.4 |
| TypeScript | ~5.9 |
| RxJS | ~7.8 |

### Infraestrutura

| Tecnologia | Versão |
|---|---|
| MongoDB | 7 |
| RabbitMQ | 3 (management) |
| Redis | — (instância separada, não inclusa no docker-compose) |
| Docker / Docker Compose | — |
| Node.js (build) | 25 |

---

## 🏗 Arquitetura

O projeto segue a **Arquitetura Hexagonal (Ports & Adapters)**, separando claramente as responsabilidades:

```
┌──────────────────────────────────────────────────────────────┐
│              Frontend (Angular 21) — repo/deploy separado      │
│  Login │ POS │ Products │ Sales │ Debtors │ Password Reset     │
└────────────────────────────┬─────────────────────────────────┘
                             │ HTTP (REST API + JWT) — cross-origin + CORS
┌────────────────────────────▼─────────────────────────────────┐
│                       Adapters (Inbound)                      │
│  AuthController │ ProductController │ SaleController           │
│  TenantController │ BillingController │ StripeWebhookController │
│  EmailEventConsumer (RabbitMQ)                                 │
│  GlobalExceptionHandler │ WebMapper │ AuthenticatedUserProvider│
├───────────────────────────────────────────────────────────────┤
│                     Application (Use Cases)                   │
│  AuthUseCaseImpl │ ProductUseCaseImpl │ SaleUseCaseImpl        │
│  TenantUseCaseImpl │ BillingUseCaseImpl │ PasswordResetUseCase │
├───────────────────────────────────────────────────────────────┤
│                       Ports (Interfaces)                      │
│  Inbound:  AuthUseCasePort │ ProductUseCasePort │ SaleUseCase  │
│            TenantUseCasePort │ BillingUseCasePort │ ...         │
│  Outbound: UserRepositoryPort │ ProductRepositoryPort │ ...     │
│            TenantRepositoryPort │ SubscriptionRepositoryPort   │
│            InvoiceRepositoryPort │ EmailServicePort            │
├───────────────────────────────────────────────────────────────┤
│                      Adapters (Outbound)                      │
│  *RepositoryAdapter │ Documents │ PersistenceMapper            │
│  JavaMailEmailServiceAdapter │ RabbitMQEmailPublisher          │
├───────────────────────────────────────────────────────────────┤
│                       Config / Security                      │
│  SecurityConfig │ JwtTokenProvider │ JwtAuthenticationFilter   │
│  TenantValidationFilter │ TokenBlacklistService │ RedisCache   │
│  StripeGateway │ BillingStripeConfig │ PlanConfig │ DailyBilling│
│  RabbitMQConfig │ CorsConfig │ DataInitializer                 │
└──────────┬──────────────────┬──────────────────┬──────────────┘
           │                  │                  │
    ┌──────▼──────┐   ┌────────▼───────┐  ┌───────▼────────┐
    │  MongoDB 7  │   │     Redis       │  │   RabbitMQ      │
    │ (dados/SaaS)│   │(blacklist/cache)│  │ (fila de e-mail)│
    └─────────────┘   └────────────────┘  └────────────────┘
                              │
                       ┌──────▼──────┐
                       │   Stripe    │
                       │  (billing)  │
                       └─────────────┘
```

### Estrutura de pacotes do backend

```
com.binitech.pdv
├── domain/
│   ├── Product, Sale, SaleItem, Payment, User, RefreshToken
│   ├── Tenant, Subscription, Invoice, EmailEvent
│   └── exception/
│       └── BusinessException, ResourceNotFoundException, EmailProcessingException
├── application/
│   ├── ports/
│   │   ├── inbound/   (Auth, Product, Sale, Tenant, Billing, PasswordReset UseCasePort)
│   │   └── outbound/  (User, RefreshToken, Product, Sale, Tenant,
│   │                    Subscription, Invoice RepositoryPort, EmailServicePort)
│   └── usecases/      (Auth, Product, Sale, Tenant, Billing, PasswordReset Impl)
├── adapters/
│   ├── inbound/
│   │   ├── web/       (Auth, Product, Sale, Tenant, Billing,
│   │   │               StripeWebhook Controllers, AuthenticatedUserProvider,
│   │   │               GlobalExceptionHandler, mapper/WebMapper)
│   │   └── EmailEventConsumer        (consumidor RabbitMQ)
│   └── outbound/
│       ├── persistence/  (*RepositoryAdapter, document/, mapper/, repository/)
│       ├── JavaMailEmailServiceAdapter, NoOpEmailServiceAdapter
│       └── RabbitMQEmailPublisher
├── config/
│   ├── SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter
│   ├── TenantValidationFilter, TokenBlacklistService, RedisCacheConfig
│   ├── StripeGateway, StripeProperties, BillingStripeConfig, PlanConfig, DailyBillingJob
│   ├── RabbitMQConfig, CorsConfig, BeanConfig, DataInitializer
│   ├── PepperedPasswordEncoder
│   └── DotenvEnvironmentPostProcessor
└── utils/
    ├── LogSanitizer
    └── enums/ (Role, PaymentMethod, TenantStatus, SubscriptionStatus, InvoiceStatus)
```

---

## ✅ Pré-requisitos

- **Java 21+**
- **Docker** e **Docker Compose** (para MongoDB e RabbitMQ)
- **Redis** acessível (instância local ou container separado)
- **Maven** (ou use o wrapper `mvnw` incluído)
- (Opcional) Conta **Stripe** (test mode) e servidor **SMTP** para billing e e-mails

---

## ⚙ Configuração e Execução

### 1. Subir a infraestrutura (MongoDB + RabbitMQ)

```bash
docker compose up -d
```

- MongoDB: `localhost:27017`
- RabbitMQ: `localhost:5672` (painel de management em `http://localhost:15672`, usuário/senha `guest`/`guest`)

> **Redis:** O Redis **não** está incluído no `docker-compose.yml`. Configure e inicie uma instância separadamente e informe a URL na variável `REDIS_URL`.

### 2. Backend (Spring Boot)

```bash
./mvnw clean install
./mvnw spring-boot:run
```

> **Windows:** use `mvnw.cmd` no lugar de `./mvnw`.

O backend estará disponível em **http://localhost:8080**.

### 3. Frontend (Angular)

O frontend está em repositório separado: **[niicsz/BiniTech-PDV-frontend](https://github.com/niicsz/BiniTech-PDV-frontend)**. Para rodá-lo localmente, clone-o e siga o README de lá:

```bash
git clone https://github.com/niicsz/BiniTech-PDV-frontend.git
cd BiniTech-PDV-frontend
npm ci
npm start
```

O `npm start` sobe em **http://localhost:4200** e faz proxy das requisições `/api` para o backend na porta `8080` (`proxy.conf.json`).

> **Produção:** front e back são **deploys independentes** em domínios distintos. O frontend chama a URL pública do backend (via `API_BASE`) e o backend libera esse domínio no CORS (`CORS_ALLOWED_ORIGINS`). O backend **não serve mais** os estáticos do Angular.

---

## 🔐 Variáveis de Ambiente

O projeto utiliza variáveis de ambiente para configuração sensível. Você pode defini-las via `.env`, variáveis do sistema ou `docker-compose.yml`:

### Core

| Variável | Descrição | Padrão |
|---|---|---|
| `MONGODB_URI` | URI de conexão com o MongoDB | — (obrigatório) |
| `REDIS_URL` | URL de conexão com o Redis | — (obrigatório) |
| `PORT` | Porta do servidor backend | `8080` |
| `JWT_SECRET` | Chave secreta para assinatura dos tokens JWT | — (obrigatório) |
| `JWT_ACCESS_EXPIRATION` | Expiração do access token (ms) | — (obrigatório) |
| `JWT_REFRESH_EXPIRATION` | Expiração do refresh token (ms) | — (obrigatório) |
| `ADMIN_USERNAME` | Username do **super admin** criado na inicialização | — (obrigatório) |
| `ADMIN_PASSWORD` | Senha do super admin | — (obrigatório) |
| `SECURITY_PEPPER` | Pepper concatenado às passwords antes do hash Argon2 | — (obrigatório) |
| `SECURITY_DUMMY_PASSWORD_HASH` | Hash usado para mitigar timing attacks em logins inexistentes | — (obrigatório) |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas pelo CORS — domínio público do frontend (em dev, `http://localhost:4200`) | — (obrigatório) |
| `APP_FRONTEND_URL` | URL pública do frontend (usada em links de e-mail e retorno do Stripe) | `http://localhost:4200` |

### RabbitMQ

| Variável | Descrição | Padrão |
|---|---|---|
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |
| `RABBITMQ_PORT` | Porta do RabbitMQ | `5672` |
| `RABBITMQ_USERNAME` | Usuário do RabbitMQ | `guest` |
| `RABBITMQ_PASSWORD` | Senha do RabbitMQ | `guest` |

### E-mail (SMTP)

| Variável | Descrição | Padrão |
|---|---|---|
| `MAIL_HOST` | Host SMTP (vazio desabilita o envio real) | — |
| `MAIL_PORT` | Porta SMTP | `587` |
| `MAIL_USERNAME` | Usuário SMTP | — |
| `MAIL_PASSWORD` | Senha SMTP | — |

### Stripe (billing)

| Variável | Descrição |
|---|---|
| `STRIPE_SECRET_KEY` | Chave secreta da API do Stripe |
| `STRIPE_WEBHOOK_SECRET` | Secret de validação dos webhooks |
| `STRIPE_PRICE_STARTER` | ID do *price* do plano Starter |
| `STRIPE_PRICE_PRO` | ID do *price* do plano Pro |
| `STRIPE_PRICE_ENTERPRISE` | ID do *price* do plano Enterprise |

---

## 🔑 Autenticação e Autorização

O sistema utiliza **JWT (JSON Web Tokens)** com **Spring Security** para proteger as rotas da API.

### Roles

| Role | Permissões |
|---|---|
| `SUPER_ADMIN` | Administração da plataforma: aprova/bloqueia/ativa tenants, acesso a `/api/admin/**` e métricas. Não pertence a nenhum tenant. |
| `ADMIN` | Administrador legado / nível de loja com acesso amplo. |
| `TENANT_ADMIN` | Administra a própria loja: registra operadores, gerencia produtos, vendas, devedores e billing do tenant. |
| `OPERATOR` | Acesso ao PDV, produtos, vendas e devedores da loja. |

### Fluxo de autenticação

1. **Login** (`POST /api/auth/login`) — Retorna `accessToken` e `refreshToken`.
2. O `accessToken` é enviado no header `Authorization: Bearer <token>` em cada requisição.
3. Quando o `accessToken` expira, o frontend usa o `refreshToken` para obter um novo par via `POST /api/auth/refresh`.
4. **Logout** (`POST /api/auth/logout`) — Invalida o `accessToken` na blacklist do Redis e remove o `refreshToken` do banco.
5. **Registro** (`POST /api/auth/register`) — Acessível por `SUPER_ADMIN`, `ADMIN` e `TENANT_ADMIN`.

### Recuperação e troca de senha

| Endpoint | Descrição | Acesso |
|---|---|---|
| `POST /api/auth/forgot-password` | Solicita link de redefinição (envia e-mail) | Público |
| `POST /api/auth/reset-password` | Redefine a senha com o token recebido | Público |
| `POST /api/auth/change-password` | Troca a senha do usuário autenticado | Autenticado |

### Inicialização

Na primeira execução, o `DataInitializer` cria automaticamente um usuário **super admin** (sem tenant) com as credenciais de `ADMIN_USERNAME` / `ADMIN_PASSWORD`.

### 🔒 Hashing de Passwords — Argon2id + Pepper

As passwords são protegidas com a técnica mais robusta disponível:

1. **Argon2id** — algoritmo vencedor da Password Hashing Competition, resistente a ataques por GPU e side-channel. Implementado via **BouncyCastle 1.84**. Parâmetros OWASP:
   - Salt: 16 bytes (gerado automaticamente)
   - Hash: 32 bytes
   - Parallelism: 1
   - Memória: 19 456 KiB (~19 MB)
   - Iterações: 2

2. **Pepper** — valor secreto (`SECURITY_PEPPER`) concatenado à password **antes** do hash pelo `PepperedPasswordEncoder`. Este valor:
   - Vive **apenas** nas variáveis de ambiente do servidor
   - **Nunca** é guardado na base de dados
   - Garante que mesmo com acesso total à BD, os hashes são **inúteis** sem o pepper

> ⚠️ **IMPORTANTE:** Se o pepper for alterado, todas as passwords existentes ficam inválidas e os usuários terão de redefinir as suas credenciais.

### 🔴 Blacklist de Tokens (Redis)

O `TokenBlacklistService` armazena no Redis os access tokens invalidados via logout. Cada entrada expira automaticamente quando o próprio token expiraria. O `JwtAuthenticationFilter` verifica a blacklist a cada requisição.

### 📦 Cache de Produtos (Redis)

O `RedisCacheConfig` configura caches Redis para operações de leitura de produtos (`products_by_user`, `product_by_id`, `product_by_barcode`, `products_all`), com TTLs entre 5 e 10 minutos.

---

## 📡 API Endpoints

A API é documentada via **OpenAPI 3.0** (gerado a partir de `swagger.yaml`) e acessível pelo **Swagger UI**:

- 📄 **Swagger UI:** http://localhost:8080/swagger-ui.html
- 📋 **API Docs (JSON):** http://localhost:8080/api-docs

### Auth

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/api/auth/login` | Fazer login | Público |
| `POST` | `/api/auth/refresh` | Renovar access token | Público |
| `POST` | `/api/auth/logout` | Logout e invalidar token | Autenticado |
| `POST` | `/api/auth/register` | Registrar novo usuário | SUPER_ADMIN / ADMIN / TENANT_ADMIN |
| `POST` | `/api/auth/forgot-password` | Solicitar redefinição de senha | Público |
| `POST` | `/api/auth/reset-password` | Redefinir senha com token | Público |
| `POST` | `/api/auth/change-password` | Trocar a própria senha | Autenticado |

### Tenants

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/api/public/tenants` | Auto-cadastro de loja | Público |
| `GET` | `/api/public/tenants/slug/{slug}` | Buscar tenant por slug | Público |
| `GET` | `/api/admin/tenants` | Listar tenants | SUPER_ADMIN |
| `GET` | `/api/admin/tenants/{id}` | Detalhar tenant | SUPER_ADMIN |
| `GET` | `/api/admin/tenants/{id}/users` | Listar usuários do tenant | SUPER_ADMIN |
| `POST` | `/api/admin/tenants/{id}/approve` | Aprovar tenant | SUPER_ADMIN |
| `POST` | `/api/admin/tenants/{id}/block` | Bloquear tenant | SUPER_ADMIN |
| `POST` | `/api/admin/tenants/{id}/activate` | Ativar tenant | SUPER_ADMIN |

### Billing

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `GET` | `/api/billing/subscription` | Assinatura atual do tenant | Autenticado |
| `GET` | `/api/billing/invoices` | Faturas do tenant | Autenticado |
| `GET` | `/api/billing/checkout` | Iniciar checkout do Stripe | Autenticado |
| `GET` | `/api/billing/portal` | Abrir o Customer Portal do Stripe | Autenticado |
| `POST` | `/webhooks/stripe` | Webhook do Stripe (eventos de pagamento) | Público (assinado) |

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
| `/actuator/health`, `/actuator/info` | Público |
| `/actuator/**` (metrics, etc.) | SUPER_ADMIN / ADMIN |

---

## 📨 Mensageria e E-mail

O envio de e-mails (ex.: aprovação de loja, redefinição de senha) é **assíncrono** via **RabbitMQ**, evitando bloquear o request principal.

| Componente | Responsabilidade |
|---|---|
| `RabbitMQConfig` | Declara o exchange `email.exchange`, a fila durável `email.queue` e o binding com routing key `email.approval`. |
| `RabbitMQEmailPublisher` | Publica `EmailEvent` na fila. |
| `EmailEventConsumer` | Consome a fila e delega o envio ao `EmailServicePort`. |
| `JavaMailEmailServiceAdapter` | Envia e-mails via SMTP (Spring Mail). |
| `NoOpEmailServiceAdapter` | Implementação no-op usada quando o SMTP não está configurado (ambientes de dev/test). |

Falhas de processamento sinalizam `EmailProcessingException`, com suporte a retry (Spring Retry).

---

## 📊 Logging e Observabilidade

O projeto conta com logging estruturado **com sanitização de dados sensíveis** em todas as camadas.

### LogSanitizer

O utilitário `LogSanitizer` mascara automaticamente dados sensíveis antes de qualquer log:

- `maskUsername(username)` — exibe apenas o primeiro e último caractere (ex: `a***n`)
- `maskId(id)` — exibe apenas os primeiros e últimos 4 caracteres (ex: `abc1***ef90`)

### Backend (SLF4J / Logback)

| Camada | Nível |
|---|---|
| **Controllers** | `INFO` — requisições recebidas e respostas de sucesso |
| **Exception Handler** (`GlobalExceptionHandler`) | `WARN` — negócio/validação/not found/acesso negado; `ERROR` — erros internos |
| **Use Cases** | `INFO` — operações; `WARN` — validações/duplicatas; `DEBUG` — consultas |
| **Security** (`JwtAuthenticationFilter`, `JwtTokenProvider`) | `WARN` — tokens inválidos/expirados; `DEBUG` — fluxo de autenticação |
| **Billing / Mensageria** (`DailyBillingJob`, `StripeWebhookController`, `EmailEventConsumer`) | `INFO` — eventos processados; `WARN`/`ERROR` — falhas |

### Frontend (Console estruturado)

Serviços, interceptors, guards e componentes usam prefixos padronizados (ex.: `[AuthService]`, `[AuthInterceptor]`, `[AuthGuard]`, `[PosScreen]`) com `console.info` / `console.warn` / `console.error`.

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
│   ├── GlobalExceptionHandlerTest
│   └── StripeWebhookControllerTest
├── application/usecases/
│   ├── AuthUseCaseImplTest, ProductUseCaseImplTest
│   ├── SaleUseCaseImplTest, BillingUseCaseImplTest
├── config/
│   ├── JwtAuthenticationFilterTest, JwtTokenProviderTest
│   ├── PepperedPasswordEncoderTest, PlanConfigTest, StripePropertiesTest
├── domain/
│   ├── ProductTest, SaleTest, SaleItemTest, RefreshTokenTest
└── integration/
    ├── AuthControllerIT, ProductControllerIT, SaleControllerIT
```

> Os testes de integração utilizam **Flapdoodle Embedded MongoDB** e **Spring Boot Test** com `@ActiveProfiles("test")`, sem necessidade de infraestrutura externa.

---

## 🐳 Docker

O projeto inclui um **Dockerfile multi-stage** (2 estágios) que compila o backend numa imagem otimizada:

1. **backend-build** — Compila o Spring Boot com Eclipse Temurin JDK 21
2. **runtime** — Imagem final mínima com Eclipse Temurin JRE 21, executando com usuário não-root para segurança

A imagem inclui **HEALTHCHECK** integrado via `/actuator/health`.

### Build da imagem

```bash
docker build -t binitech-pdv .
```

### Subir a infraestrutura (MongoDB + RabbitMQ)

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
  -e RABBITMQ_HOST=host.docker.internal \
  -e JWT_SECRET=sua-chave-secreta \
  -e JWT_ACCESS_EXPIRATION=3600000 \
  -e JWT_REFRESH_EXPIRATION=604800000 \
  -e ADMIN_USERNAME=admin \
  -e ADMIN_PASSWORD=sua-senha-admin \
  -e SECURITY_PEPPER=seu-pepper-secreto \
  -e SECURITY_DUMMY_PASSWORD_HASH=seu-hash-dummy \
  -e CORS_ALLOWED_ORIGINS=http://localhost:4200 \
  binitech-pdv
```

> Esta imagem é **API-only**. O frontend é buildado e servido pelo seu próprio repositório/deploy ([niicsz/BiniTech-PDV-frontend](https://github.com/niicsz/BiniTech-PDV-frontend)).

---

## 📂 Estrutura do Projeto

```
pdv/
├── docker-compose.yml          # Sobe MongoDB + RabbitMQ (Redis não incluso)
├── Dockerfile                  # Build multi-stage (backend, API-only)
├── owasp-suppressions.xml      # Supressões do OWASP Dependency Check
├── pom.xml                     # Configuração Maven
├── mvnw / mvnw.cmd             # Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/binitech/pdv/
│   │   │   ├── domain/         # Entidades de domínio e exceções
│   │   │   ├── application/    # Portas (inbound/outbound) e casos de uso
│   │   │   ├── adapters/       # Controllers, Repositories, Mappers, Mensageria
│   │   │   ├── config/         # Security, JWT, Redis, Stripe, RabbitMQ, Tenant, Beans
│   │   │   └── utils/          # LogSanitizer, Enums
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── openapi/swagger.yaml
│   └── test/
│       └── java/com/binitech/pdv/
│           ├── adapters/, application/, config/, domain/
│           └── integration/    # Testes de integração (AuthControllerIT, etc.)
└── target/                     # Build artifacts
```

> **Frontend:** em repositório separado — [niicsz/BiniTech-PDV-frontend](https://github.com/niicsz/BiniTech-PDV-frontend) (Angular 21 servido via Nginx).

---

## 📝 Autor

Este projeto é desenvolvido por **Nicolas Bezerra Bini**.
