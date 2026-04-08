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
- [Docker](#-docker)
- [Estrutura do Projeto](#-estrutura-do-projeto)

---

## 🎯 Visão Geral

O **BiniTech PDV** é um sistema completo de frente de caixa que permite:

- **Autenticação JWT** — Login com access token e refresh token, controle de roles (ADMIN / OPERATOR).
- **Cadastro de produtos** — CRUD completo com código de barras, descrição, preço, preço de custo, estoque, **categoria** e **status ativo/inativo**.
- **Filtro por categoria e busca** — Na listagem de produtos, filtre rapidamente por categoria ou pesquise por descrição/código de barras. Indicadores de **produtos ativos** e **estoque baixo**.
- **Tela de PDV** — Leitura rápida por código de barras **ou pesquisa por nome do produto** (autocompletar), carrinho de compras e finalização de venda.
- **Múltiplas formas de pagamento** — Dinheiro, Cartão de Crédito, Cartão de Débito, PIX e **Crediário (Fiado)**, com suporte a **pagamento misto** na mesma venda.
- **Crediário / Fiado** — Vendas no crediário com registro de nome e telefone do cliente. Controle completo de devedores com opção de marcar como pago.
- **Gestão de Devedores** — Tela dedicada para visualização de vendas pendentes no crediário, agrupadas por cliente, com indicador de dias em atraso e link direto para **WhatsApp**.
- **Venda sem estoque** — Ao tentar vender um produto sem estoque suficiente, o sistema oferece a opção de atualizar o estoque automaticamente ou vender mesmo assim (skip stock validation).
- **Alerta de estoque baixo** — Após finalizar uma venda, o sistema alerta automaticamente quando produtos vendidos ficam com estoque abaixo de 5 unidades.
- **Nota impressa com nome do operador** — O comprovante de venda exibe o nome do usuário logado.
- **Relatório de vendas** — Consulta de vendas por data ou período, com cálculo de **receita, custo e lucro**.
- **Registro de usuários** — Somente administradores podem registrar novos usuários.
- **Personalização Visual** — Suporte nativo a Modo Escuro (Dark Mode) e customização de cores da interface (cor primária e cabeçalho).
- **Logging estruturado** — Logs de info, warning e erro em toda a aplicação (backend com SLF4J, frontend com console estruturado) para facilitar monitoramento e depuração.

---

## 🚀 Tecnologias

### Backend
| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 3.4.3 |
| Spring Security | — |
| Spring Data MongoDB | — |
| SLF4J / Logback | — |
| JWT (jjwt) | 0.12.6 |
| BouncyCastle (Argon2) | 1.80 |
| OpenAPI Generator | 7.12.0 |
| MapStruct | 1.6.3 |
| Lombok | 1.18.36 |
| SpringDoc OpenAPI (Swagger UI) | 2.8.6 |
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
│         GlobalExceptionHandler  │  Mappers              │
├─────────────────────────────────────────────────────────┤
│                  Application (Use Cases)                │
│  AuthUseCaseImpl │ ProductUseCaseImpl │ SaleUseCaseImpl │
├─────────────────────────────────────────────────────────┤
│                     Ports (Interfaces)                  │
│  Inbound: AuthUseCasePort │ ProductUseCasePort │ SaleUseCasePort  │
│  Outbound: UserRepositoryPort │ RefreshTokenRepositoryPort       │
│            ProductRepositoryPort │ SaleRepositoryPort            │
├─────────────────────────────────────────────────────────┤
│                   Adapters (Outbound)                   │
│  UserRepositoryAdapter │ RefreshTokenRepositoryAdapter  │
│  ProductRepositoryAdapter  │  SaleRepositoryAdapter     │
│            Documents  │  Mappers  │  Repositories       │
├─────────────────────────────────────────────────────────┤
│                   Config / Security                     │
│  SecurityConfig │ JwtTokenProvider │ JwtAuthFilter       │
│  CorsConfig │ DataInitializer │ BeanConfig              │
│  PepperedPasswordEncoder │ SpaWebConfig                 │
└────────────────────────────┬────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │    MongoDB 7    │
                    └─────────────────┘
```

### Estrutura de pacotes do backend

```
com.binitech.pdv
├── domain/
│   ├── Product, Sale, SaleItem, Payment, User, RefreshToken
│   └── exception/
├── application/
│   ├── ports/
│   │   ├── inbound/   (AuthUseCasePort, ProductUseCasePort, SaleUseCasePort)
│   │   └── outbound/  (UserRepositoryPort, RefreshTokenRepositoryPort,
│   │                    ProductRepositoryPort, SaleRepositoryPort)
│   └── usecases/      (AuthUseCaseImpl, ProductUseCaseImpl, SaleUseCaseImpl)
├── adapters/
│   ├── inbound/web/
│   │   ├── AuthController, ProductController, SaleController
│   │   ├── GlobalExceptionHandler
│   │   └── mapper/
│   └── outbound/persistence/
│       ├── UserRepositoryAdapter, RefreshTokenRepositoryAdapter
│       ├── ProductRepositoryAdapter, SaleRepositoryAdapter
│       ├── document/
│       ├── mapper/
│       └── repository/
├── config/
│   ├── SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter
│   ├── CorsConfig, BeanConfig, DataInitializer
│   ├── PepperedPasswordEncoder, SpaWebConfig
│   └── DotenvEnvironmentPostProcessor
└── utils/
```

---

## ✅ Pré-requisitos

- **Java 17+**
- **Node.js 25+** e **npm**
- **Docker** e **Docker Compose** (para o MongoDB)
- **Maven** (ou use o wrapper `mvnw` incluído)

---

## ⚙ Configuração e Execução

### 1. Subir o banco de dados (MongoDB)

```bash
docker compose up -d
```

O MongoDB ficará acessível em `localhost:27017` com o banco `binitech_pdv`.

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
| `PORT` | Porta do servidor backend | `8080` |
| `JWT_SECRET` | Chave secreta para assinatura dos tokens JWT | — (obrigatório) |
| `JWT_ACCESS_EXPIRATION` | Tempo de expiração do access token (ms) | `3600000` (1h) |
| `JWT_REFRESH_EXPIRATION` | Tempo de expiração do refresh token (ms) | `604800000` (7d) |
| `ADMIN_USERNAME` | Username do admin criado na inicialização | `admin` |
| `ADMIN_PASSWORD` | Senha do admin criado na inicialização | — (obrigatório) |
| `SECURITY_PEPPER` | Valor secreto (pepper) concatenado às passwords antes do hash Argon2. **Nunca guardar na BD.** | — (obrigatório) |
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
4. **Registro** (`POST /api/auth/register`) — Somente acessível por usuários `ADMIN`.

### Inicialização

Na primeira execução, o `DataInitializer` cria automaticamente um usuário admin com as credenciais definidas nas variáveis `ADMIN_USERNAME` e `ADMIN_PASSWORD`.

### 🔒 Hashing de Passwords — Argon2id + Pepper

As passwords são protegidas com a técnica mais robusta disponível:

1. **Argon2id** — algoritmo vencedor da Password Hashing Competition, resistente a ataques por GPU e side-channel. Implementado via **BouncyCastle**. Parâmetros OWASP:
   - Salt: 16 bytes (gerado automaticamente)
   - Hash: 32 bytes
   - Parallelism: 1
   - Memória: 19 456 KiB (~19 MB)
   - Iterações: 2

2. **Pepper** — um valor secreto (`SECURITY_PEPPER`) que é concatenado à password **antes** do hash pelo `PepperedPasswordEncoder`. Este valor:
   - Vive **apenas** nas variáveis de ambiente do servidor
   - **Nunca** é guardado na base de dados
   - Garante que mesmo com acesso total à BD, os hashes são **inúteis** sem o pepper

> ⚠️ **IMPORTANTE:** Se o pepper for alterado, todas as passwords existentes ficam inválidas e os utilizadores terão de redefinir as suas credenciais.

---

## 📡 API Endpoints

A API é documentada via **OpenAPI 3.0** e acessível pelo **Swagger UI**:

- 📄 **Swagger UI:** http://localhost:8080/swagger-ui.html
- 📋 **API Docs (JSON):** http://localhost:8080/api-docs

### Auth

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/api/auth/login` | Fazer login | Público |
| `POST` | `/api/auth/register` | Registrar novo usuário | ADMIN |
| `POST` | `/api/auth/refresh` | Renovar access token | Público |

### Products

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `GET` | `/api/products` | Listar todos os produtos | Autenticado |
| `POST` | `/api/products` | Cadastrar um novo produto | Autenticado |
| `GET` | `/api/products/{id}` | Buscar produto por ID | Autenticado |
| `PUT` | `/api/products/{id}` | Atualizar um produto | Autenticado |
| `DELETE` | `/api/products/{id}` | Remover (desativar) um produto | Autenticado |
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
| `skipStockValidation` | boolean | Não | Ignorar validação de estoque (vendas sem estoque) |

### Formas de Pagamento

- `CASH` — Dinheiro
- `CREDIT_CARD` — Cartão de Crédito
- `DEBIT_CARD` — Cartão de Débito
- `PIX` — PIX
- `CREDIARIO` — Crediário / Fiado

---

## 📊 Logging e Observabilidade

O projeto conta com um sistema de logging estruturado em todas as camadas, facilitando o monitoramento, depuração e auditoria.

### Backend (SLF4J / Logback)

Todos os componentes do backend utilizam **SLF4J** com níveis de log adequados:

| Camada | Classe | Logs |
|---|---|---|
| **Controllers** | `AuthController`, `ProductController`, `SaleController` | `INFO` — requisições recebidas e respostas de sucesso |
| **Exception Handler** | `GlobalExceptionHandler` | `WARN` — exceções de negócio, validação, not found, acesso negado; `ERROR` — erros internos com stack trace |
| **Use Cases** | `AuthUseCaseImpl`, `ProductUseCaseImpl`, `SaleUseCaseImpl` | `INFO` — operações de negócio (login, registro, CRUD, vendas); `WARN` — falhas de validação, permissões, duplicatas; `DEBUG` — consultas e listagens |
| **Security** | `JwtAuthenticationFilter`, `JwtTokenProvider` | `DEBUG` — autenticação JWT configurada; `WARN` — tokens inválidos ou expirados; `INFO` — inicialização |
| **Config** | `BeanConfig`, `SecurityConfig`, `DataInitializer` | `INFO` — inicialização de beans e configurações |
| **Infra** | `DotenvEnvironmentPostProcessor` | `INFO` — carregamento de variáveis .env; `ERROR` — falha na leitura |

#### Exemplos de logs do backend

```
INFO  AuthController       - Requisição de login recebida para o usuário: admin
INFO  AuthUseCaseImpl      - Login realizado com sucesso: userId=abc123 username=admin role=ADMIN
WARN  AuthUseCaseImpl      - Login falhou - senha inválida para o usuário: operador1
INFO  ProductUseCaseImpl   - Produto criado com sucesso: id=xyz789 barcode=7891234567890
WARN  GlobalExceptionHandler - Erro de negócio: Estoque insuficiente para 'Coca-Cola'. Disponível: 2, Solicitado: 5
ERROR GlobalExceptionHandler - Erro interno inesperado: NullPointerException [stack trace]
INFO  SaleUseCaseImpl      - Venda criada com sucesso: id=sale123 total=157.50 userId=abc123
```

### Frontend (Console estruturado)

Todos os serviços e componentes do frontend utilizam **console** com prefixos de identificação:

| Camada | Prefixo | Logs |
|---|---|---|
| **Services** | `[AuthService]`, `[ProductService]`, `[SaleService]` | `console.info` — chamadas à API e respostas de sucesso |
| **Interceptor** | `[AuthInterceptor]` | `console.warn` — erros 401 e refresh de token; `console.error` — falhas de renovação |
| **Guards** | `[AuthGuard]`, `[AdminGuard]` | `console.debug` — acesso permitido; `console.warn` — acesso negado |
| **Components** | `[LoginComponent]`, `[PosScreen]`, `[ProductList]`, etc. | `console.info` — ações do usuário e sucesso; `console.error` — falhas; `console.warn` — validações e alertas |

#### Exemplos de logs do frontend

```
[AuthService] Realizando login para o usuário: admin
[AuthService] Login realizado com sucesso: admin role: ADMIN
[PosScreen] Tela PDV inicializada para: admin
[PosScreen] Produtos carregados para busca offline: 47
[PosScreen] Venda finalizada com sucesso: id= sale123 total= 157.50
[PosScreen] Produtos com estoque baixo detectados: 2 ["Coca-Cola (3)", "Biscoito (1)"]
[AuthInterceptor] Recebido 401, tentando renovar token para: /api/products
[AdminGuard] Acesso negado - usuário não é ADMIN, redirecionando para /pdv
```

---

## 🖥 Funcionalidades do Frontend

### Tela de PDV (Ponto de Venda)

- Leitura rápida de produtos por **código de barras** ou **pesquisa por nome** com dropdown de autocompletar (até 8 sugestões)
- Carrinho de compras com atalhos de teclado:
  - `F2` — Alterar quantidade do item selecionado
  - `F4` — Pagamento em Dinheiro
  - `F7` — Pagamento em Cartão de Crédito
  - `F8` — Pagamento em Cartão de Débito
  - `F9` — Pagamento via PIX
  - `F10` — Pagamento via Crediário / Fiado
  - `Del` — Remover item selecionado
  - `Esc` — Cancelar / Fechar modal
  - `↑ ↓` — Navegar nas sugestões do autocompletar
- Modal de pagamento com suporte a **múltiplas formas de pagamento** na mesma venda (pagamento misto)
- Cálculo automático de **troco** e valor **restante** a pagar
- **Crediário / Fiado** — Ao selecionar crediário, campos de nome e telefone do cliente são exibidos para registro
- **Venda sem estoque** — Ao tentar adicionar um produto com estoque insuficiente, o sistema oferece opções: adicionar estoque e vender, ou vender sem validar estoque
- **Alerta de estoque baixo** — Após finalizar a venda, exibe alerta se algum produto vendido ficou com estoque abaixo de 5 unidades
- **Comprovante de venda** com dados da venda, itens, pagamentos e **nome do operador logado**
- Impressão da nota via `window.print()`
- Sidebar com **resumo da venda** (itens distintos, total de produtos, valor total)

### Cadastro de Produtos

- Formulário completo: código de barras, descrição, preço de venda, preço de custo, estoque, **categoria** e **status ativo/inativo**
- **Filtro por categoria** — Dropdown que filtra a listagem de produtos por categoria cadastrada
- **Busca por texto** — Campo de pesquisa que filtra por descrição ou código de barras
- **Indicadores** — Contadores de produtos ativos e produtos com estoque baixo (≤ 5 unidades)
- Edição e exclusão de produtos
- Coluna de categoria visível na tabela de produtos

### Relatório de Vendas

- Consulta por data específica ou período (data inicial / data final)
- Exibição de itens vendidos, pagamentos e totais
- Cálculo de **receita total**, **custo total** e **lucro** por venda e no período

### Gestão de Devedores (Crediário)

- Tela dedicada com **lista de devedores** agrupados por cliente
- **Cards de resumo** — Total de devedores, vendas pendentes e valor total a receber
- **Indicador de atraso** — Exibe quantidade de dias sem pagamento com destaque visual para débitos em atraso
- **Link para WhatsApp** — Botão de contato direto com o cliente via WhatsApp
- **Marcar como pago** — Botão para quitar individualmente cada venda pendente
- **Dados do cliente** — Nome e telefone exibidos para cada devedor

### Aparência e Tema

- Alternância entre **Modo Claro** e **Modo Escuro** (Dark Mode) com detecção automática da preferência do sistema
- Modal de **Configurações de Aparência** que permite customizar a Cor Primária e a Cor do Cabeçalho
- Botão de **Restaurar Padrões** nas configurações de aparência
- Persistência das preferências de tema do usuário utilizando `localStorage`

---

## 🐳 Docker

O projeto inclui um **Dockerfile multi-stage** (3 estágios) que compila frontend e backend em uma única imagem otimizada:

1. **frontend-build** — Compila o Angular com Node.js 25 Alpine
2. **backend-build** — Compila o Spring Boot com Eclipse Temurin JDK 17, copiando o build do frontend para `resources/static/`
3. **runtime** — Imagem final mínima com Eclipse Temurin JRE 17, executando com usuário não-root para segurança

### Build da imagem

```bash
docker build -t binitech-pdv .
```

### Executar com Docker Compose

O `docker-compose.yml` sobe o MongoDB:

```bash
docker compose up -d
```

### Executar a aplicação em container

```bash
docker run -d \
  --name binitech-pdv-app \
  -p 8080:8080 \
  -e MONGODB_URI=mongodb://host.docker.internal:27017/binitech_pdv \
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
├── docker-compose.yml          # Sobe o MongoDB
├── Dockerfile                  # Build multi-stage (frontend + backend)
├── pom.xml                     # Configuração Maven
├── mvnw / mvnw.cmd             # Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/binitech/pdv/
│   │   │   ├── domain/         # Entidades de domínio
│   │   │   ├── application/    # Portas e casos de uso
│   │   │   ├── adapters/       # Controllers e Repositories
│   │   │   ├── config/         # Security, JWT, CORS, Beans, SpaWebConfig
│   │   │   └── utils/
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── openapi/swagger.yaml
│   └── test/
├── frontend/
│   ├── src/app/
│   │   ├── auth/               # Login, Register, Guards, Interceptors
│   │   ├── pos/                # Tela de PDV (carrinho, pagamento, busca)
│   │   │   ├── components/     # PosScreen, CartTable, PaymentModal
│   │   │   └── services/       # ProductService, SaleService
│   │   ├── products/           # Listagem e cadastro de produtos
│   │   ├── sales/              # Relatório de vendas (receita, custo, lucro)
│   │   ├── debtors/            # Gestão de devedores (crediário)
│   │   │   └── components/     # DebtorsList (agrupamento, WhatsApp, mark paid)
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
