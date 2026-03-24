# 🛒 BiniTech PDV

Sistema Frente de Caixa (PDV — Ponto de Venda) desenvolvido com **Arquitetura Hexagonal**, utilizando **Spring Boot 3** no backend e **Angular 19** no frontend.

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
- [Docker](#-docker)
- [Estrutura do Projeto](#-estrutura-do-projeto)

---

## 🎯 Visão Geral

O **BiniTech PDV** é um sistema completo de frente de caixa que permite:

- **Autenticação JWT** — Login com access token e refresh token, controle de roles (ADMIN / OPERATOR).
- **Cadastro de produtos** — CRUD completo com código de barras, descrição, preço, estoque e **categoria**.
- **Filtro por categoria** — Na listagem de produtos, filtre rapidamente por categoria cadastrada.
- **Tela de PDV** — Leitura rápida por código de barras, carrinho de compras e finalização de venda.
- **Múltiplas formas de pagamento** — Dinheiro, Cartão de Crédito, Cartão de Débito e PIX.
- **Nota impressa com nome do operador** — O comprovante de venda exibe o nome do usuário logado.
- **Relatório de vendas** — Consulta de vendas por data ou período.
- **Registro de usuários** — Somente administradores podem registrar novos usuários.
- **Personalização Visual** — Suporte nativo a Modo Escuro (Dark Mode) e customização de cores da interface (cor primária e cabeçalho).

---

## 🚀 Tecnologias

### Backend
| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 3.4.3 |
| Spring Security | — |
| Spring Data MongoDB | — |
| JWT (jjwt) | 0.12.6 |
| OpenAPI Generator | 7.12.0 |
| MapStruct | 1.6.3 |
| Lombok | 1.18.36 |
| SpringDoc OpenAPI (Swagger UI) | 2.8.6 |
| Spotify fmt (code formatter) | 2.25 |

### Frontend
| Tecnologia | Versão |
|---|---|
| Angular | 19 |
| TypeScript | ~5.6 |
| RxJS | ~7.8 |

### Infraestrutura
| Tecnologia | Versão |
|---|---|
| MongoDB | 7 |
| Docker / Docker Compose | — |
| Node.js (build) | 20 |

---

## 🏗 Arquitetura

O projeto segue a **Arquitetura Hexagonal (Ports & Adapters)**, separando claramente as responsabilidades:

```
┌─────────────────────────────────────────────────────────┐
│                      Frontend (Angular 19)              │
│    Login │ POS Screen │ Product List │ Sales Report     │
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
│   └── DotenvEnvironmentPostProcessor
└── utils/
```

---

## ✅ Pré-requisitos

- **Java 17+**
- **Node.js 20+** e **npm**
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
| `ADMIN` | Acesso completo: PDV, produtos, vendas, registro de usuários |
| `OPERATOR` | Acesso ao PDV, produtos e vendas |

### Fluxo de autenticação

1. **Login** (`POST /api/auth/login`) — Retorna `accessToken` e `refreshToken`.
2. O `accessToken` é enviado no header `Authorization: Bearer <token>` em cada requisição.
3. Quando o `accessToken` expira, o frontend usa o `refreshToken` para obter um novo par de tokens via `POST /api/auth/refresh`.
4. **Registro** (`POST /api/auth/register`) — Somente acessível por usuários `ADMIN`.

### Inicialização

Na primeira execução, o `DataInitializer` cria automaticamente um usuário admin com as credenciais definidas nas variáveis `ADMIN_USERNAME` e `ADMIN_PASSWORD`.

### 🔒 Hashing de Passwords — Argon2id + Pepper

As passwords são protegidas com a técnica mais robusta disponível:

1. **Argon2id** — algoritmo vencedor da Password Hashing Competition, resistente a ataques por GPU e side-channel. Parâmetros OWASP:
   - Salt: 16 bytes (gerado automaticamente)
   - Hash: 32 bytes
   - Parallelism: 1
   - Memória: 19 456 KiB (~19 MB)
   - Iterações: 2

2. **Pepper** — um valor secreto (`SECURITY_PEPPER`) que é concatenado à password **antes** do hash. Este valor:
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

### Sales

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/api/sales` | Registrar uma nova venda | Autenticado |
| `GET` | `/api/sales` | Listar vendas (filtro por data/período) | Autenticado |
| `GET` | `/api/sales/{id}` | Buscar venda por ID | Autenticado |

### Formas de Pagamento

- `CASH` — Dinheiro
- `CREDIT_CARD` — Cartão de Crédito
- `DEBIT_CARD` — Cartão de Débito
- `PIX` — PIX

---

## 🖥 Funcionalidades do Frontend

### Tela de PDV (Ponto de Venda)

- Leitura rápida de produtos por **código de barras**
- Carrinho de compras com atalhos de teclado (`F2` alterar quantidade, `F5` dinheiro, `F7` crédito, `F8` débito, `F9` PIX, `Del` remover item)
- Modal de pagamento com suporte a **múltiplas formas de pagamento** na mesma venda
- **Comprovante de venda** com dados da venda, itens, pagamentos e **nome do operador logado**
- Impressão da nota via `window.print()`

### Cadastro de Produtos

- Formulário completo: código de barras, descrição, preço de venda, preço de custo, estoque e **categoria**
- **Filtro por categoria** — dropdown que filtra a listagem de produtos por categoria cadastrada
- Edição e exclusão de produtos
- Coluna de categoria visível na tabela de produtos

### Relatório de Vendas

- Consulta por data específica ou período (data inicial / data final)
- Exibição de itens vendidos, pagamentos e totais

### Aparência e Tema

- Alternância entre **Modo Claro** e **Modo Escuro** (Dark Mode).
- Modal de **Configurações de Aparência** que permite customizar a Cor Primária e a Cor do Cabeçalho.
- Persistência das preferências de tema do usuário utilizando `localStorage`.

---

## 🐳 Docker

O projeto inclui um **Dockerfile multi-stage** que compila frontend e backend em uma única imagem otimizada.

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
  -e ADMIN_PASSWORD=sua-senha-admin \
  binitech-pdv
```
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
│   │   │   ├── config/         # Security, JWT, CORS, Beans
│   │   │   └── utils/
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── openapi/swagger.yaml
│   └── test/
├── frontend/
│   ├── src/app/
│   │   ├── auth/               # Login, Register, Guards, Interceptors
│   │   ├── pos/                # Tela de PDV (carrinho, pagamento)
│   │   ├── products/           # Listagem e cadastro de produtos
│   │   ├── sales/              # Relatório de vendas
│   │   └── shared/             # Models compartilhados
│   ├── proxy.conf.json
│   └── package.json
└── target/                     # Build artifacts
```

---

## 📝 Autor

Este projeto é desenvolvido por **Nicolas Bezerra Bini**.
