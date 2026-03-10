# 🛒 BiniTech PDV

Sistema Frente de Caixa (PDV — Ponto de Venda) desenvolvido com **Arquitetura Hexagonal**, utilizando **Spring Boot 4** no backend e **Angular 19** no frontend.

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Pré-requisitos](#-pré-requisitos)
- [Configuração e Execução](#-configuração-e-execução)
- [API Endpoints](#-api-endpoints)
- [Estrutura do Projeto](#-estrutura-do-projeto)

---

## 🎯 Visão Geral

O **BiniTech PDV** é um sistema completo de frente de caixa que permite:

- **Cadastro de produtos** — CRUD completo com código de barras, descrição, preço e estoque.
- **Tela de PDV** — Leitura rápida por código de barras, carrinho de compras e finalização de venda.
- **Múltiplas formas de pagamento** — Dinheiro, Cartão de Crédito, Cartão de Débito e PIX.
- **Relatório de vendas** — Consulta de vendas por data ou período.

---

## 🚀 Tecnologias

### Backend
| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.3 |
| Spring Data MongoDB | — |
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

---

## 🏗 Arquitetura

O projeto segue a **Arquitetura Hexagonal (Ports & Adapters)**, separando claramente as responsabilidades:

```
┌─────────────────────────────────────────────────────────┐
│                      Frontend (Angular 19)              │
│         POS Screen │ Product List │ Sales Report        │
└────────────────────────────┬────────────────────────────┘
                             │ HTTP (REST API)
┌────────────────────────────▼────────────────────────────┐
│                   Adapters (Inbound)                    │
│         ProductController  │  SaleController            │
│         GlobalExceptionHandler  │  Mappers              │
├─────────────────────────────────────────────────────────┤
│                  Application (Use Cases)                │
│     ProductUseCaseImpl  │  SaleUseCaseImpl              │
├─────────────────────────────────────────────────────────┤
│                     Ports (Interfaces)                  │
│  Inbound: ProductUseCasePort │ SaleUseCasePort          │
│  Outbound: ProductRepositoryPort │ SaleRepositoryPort   │
├─────────────────────────────────────────────────────────┤
│                   Adapters (Outbound)                   │
│   ProductRepositoryAdapter  │  SaleRepositoryAdapter    │
│            Documents  │  Mappers  │  Repositories       │
└────────────────────────────┬────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │    MongoDB 7    │
                    └─────────────────┘
```

### Estrutura de pacotes do backend

```
com.binitech.pdv
├── domain/                          # Entidades de domínio (Product, Sale, SaleItem, Payment)
│   └── exception/                   # Exceções de domínio
├── application/
│   ├── ports/
│   │   ├── inbound/                 # Portas de entrada (ProductUseCasePort, SaleUseCasePort)
│   │   └── outbound/               # Portas de saída (ProductRepositoryPort, SaleRepositoryPort)
│   └── usecases/                    # Implementação dos casos de uso
├── adapters/
│   ├── inbound/web/                 # Controllers REST (gerados via OpenAPI + implementações)
│   │   └── mapper/                  # Mappers de DTO ↔ Domain
│   └── outbound/persistence/       # Adaptadores de persistência MongoDB
│       ├── document/                # Documentos MongoDB
│       ├── mapper/                  # Mappers de Document ↔ Domain
│       └── repository/             # Interfaces Spring Data MongoDB
├── config/                          # Configurações (Beans, CORS)
└── utils/                           # Utilitários
```

---

## ✅ Pré-requisitos

- **Java 17+**
- **Node.js 18+** e **npm**
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
# Compilar e gerar os artefatos OpenAPI
./mvnw clean install

# Executar a aplicação
./mvnw spring-boot:run
```

> **Windows:** use `mvnw.cmd` no lugar de `./mvnw`.

O backend estará disponível em **http://localhost:8080**.

### 3. Frontend (Angular)

```bash
cd frontend

# Instalar dependências
npm install

# Iniciar o servidor de desenvolvimento
npm start
```

O frontend estará disponível em **http://localhost:4200** e fará proxy das requisições `/api` para o backend na porta `8080`.

---

## 📡 API Endpoints

A API é documentada via **OpenAPI 3.0** e acessível pelo **Swagger UI**:

- 📄 **Swagger UI:** http://localhost:8080/swagger-ui.html
- 📋 **API Docs (JSON):** http://localhost:8080/api-docs

### Products

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/products` | Listar todos os produtos |
| `POST` | `/api/products` | Cadastrar um novo produto |
| `GET` | `/api/products/{id}` | Buscar produto por ID |
| `PUT` | `/api/products/{id}` | Atualizar um produto |
| `DELETE` | `/api/products/{id}` | Remover um produto |
| `GET` | `/api/products/barcode/{barcode}` | Buscar produto por código de barras |

### Sales

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/sales` | Registrar uma nova venda |
| `GET` | `/api/sales` | Listar vendas (filtro por data/período) |
| `GET` | `/api/sales/{id}` | Buscar venda por ID |

### Formas de Pagamento

- `CASH` — Dinheiro
- `CREDIT_CARD` — Cartão de Crédito
- `DEBIT_CARD` — Cartão de Débito
- `PIX` — PIX

---

## 📂 Estrutura do Projeto

```
pdv/
├── docker-compose.yml               # MongoDB via Docker
├── pom.xml                          # Configuração Maven (backend)
├── mvnw / mvnw.cmd                  # Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/binitech/pdv/   # Código-fonte Java (Hexagonal)
│   │   └── resources/
│   │       ├── application.yaml     # Configurações da aplicação
│   │       └── openapi/swagger.yaml # Especificação OpenAPI
│   └── test/                        # Testes
├── frontend/                        # Aplicação Angular
│   ├── src/app/
│   │   ├── pos/                     # Módulo PDV (tela de caixa)
│   │   ├── products/                # Módulo de Produtos
│   │   ├── sales/                   # Módulo de Relatório de Vendas
│   │   └── shared/                  # Models compartilhados
│   ├── proxy.conf.json              # Proxy para API backend
│   └── package.json
└── target/                          # Artefatos compilados
```

---

## 📝 Licença

Este projeto é desenvolvido por **Nicolas Bezerra Bini**.

