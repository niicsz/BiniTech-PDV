# Guia para novos contribuidores do BiniTech PDV

Este guia explica como o produto é dividido em serviços, como eles conversam, como rodar tudo na sua máquina e como uma mudança sai do seu branch e chega à produção.

Tudo aqui foi conferido em 13/11/2026 contra o código da `main` de cada repositório, as configurações do GitHub e os projetos na Railway. Se algo no guia divergir do código, o código vale: corrija o guia no mesmo PR.

## Sumário

1. [O que é o produto](#1-o-que-é-o-produto)
2. [Mapa dos repositórios](#2-mapa-dos-repositórios)
3. [Como os serviços conversam](#3-como-os-serviços-conversam)
4. [Arquitetura dentro de cada serviço](#4-arquitetura-dentro-de-cada-serviço)
5. [Autenticação e autorização](#5-autenticação-e-autorização)
6. [Mensageria (RabbitMQ)](#6-mensageria-rabbitmq)
7. [Integrações externas](#7-integrações-externas)
8. [Rodando na sua máquina](#8-rodando-na-sua-máquina)
9. [Do branch à produção](#9-do-branch-à-produção)
10. [Infraestrutura e deploy na Railway](#10-infraestrutura-e-deploy-na-railway)
11. [Mudanças que tocam mais de um serviço](#11-mudanças-que-tocam-mais-de-um-serviço)
12. [Feature flags](#12-feature-flags)
13. [Segredos e segurança](#13-segredos-e-segurança)
14. [Pontos de atenção conhecidos](#14-pontos-de-atenção-conhecidos)
15. [Glossário](#15-glossário)

## Como abrir os desenhos

Os desenhos ficam em [`diagramas/`](diagramas) no formato `.excalidraw`. No VS Code, instale a extensão **Excalidraw** (`pomdtr.excalidraw-editor`) e abra o arquivo: ele vira um quadro editável. Também dá para arrastar o arquivo em <https://excalidraw.com>. O GitHub não mostra esse formato como imagem.

| Desenho | O que mostra |
|---|---|
| [`01-visao-geral.excalidraw`](diagramas/01-visao-geral.excalidraw) | Todos os serviços e quem chama quem |
| [`02-integracoes-externas.excalidraw`](diagramas/02-integracoes-externas.excalidraw) | Sistemas de terceiros usados por cada serviço |
| [`03-arquitetura-hexagonal.excalidraw`](diagramas/03-arquitetura-hexagonal.excalidraw) | Camadas dentro de um serviço e o fluxo API-first |
| [`04-autenticacao.excalidraw`](diagramas/04-autenticacao.excalidraw) | Login e validação de sessão entre os serviços |
| [`05-mensageria.excalidraw`](diagramas/05-mensageria.excalidraw) | Exchanges, filas e consumidores no RabbitMQ |
| [`06-fluxo-de-pr.excalidraw`](diagramas/06-fluxo-de-pr.excalidraw) | Os 12 passos de um PR até a produção |
| [`07-infra-railway.excalidraw`](diagramas/07-infra-railway.excalidraw) | Serviços de produção e de homologação na Railway |

## 1. O que é o produto

O BiniTech PDV é um SaaS de frente de caixa (ponto de venda). Cada loja é um **tenant**: tem seus usuários, produtos e vendas isolados pelo `tenantId`. A loja paga uma assinatura mensal e usa o sistema pelo navegador, em <https://www.binitechpdv.com.br>.

Quem usa:

- **Lojista (administrador da loja):** cadastra produtos e operadores, vê relatórios, configura nota fiscal, Pix e delivery, e cuida da assinatura.
- **Operador de caixa:** registra vendas, abre e fecha o turno de caixa, consulta devedores.
- **Cliente do delivery:** faz pedido pela página pública da loja (`/pedir/:slug`), sem login.
- **Super admin:** administra a plataforma (lojas, planos e cobrança).

## 2. Mapa dos repositórios

Todos ficam em `github.com/niicsz`. Cada serviço tem repositório, build, testes, CI e deploy próprios.

| Repositório | O que é | Stack | Porta local | Banco | Visibilidade |
|---|---|---|---|---|---|
| `BiniTech-PDV` | API principal: vendas, produtos, lojas, assinaturas, turno de caixa, Pix, delivery | Java 25, Spring Boot 4.1.2 | 8080 | MongoDB `binitech_pdv` + Redis | público |
| `BiniTech-PDV-frontend` | Aplicação web (landing, login, PDV, relatórios, delivery) | Angular 21, TypeScript 5.9, Nginx | 4200 (dev) | não tem | público |
| `BiniTech-Auth` | Login, tokens, sessões e 2FA (TOTP) | Java 25, Spring Boot 4.1.2 | 8081 | MongoDB `binitech_auth` | público |
| `BiniTech-Product-Import` | Importação de produtos por CSV, XLSX e XLS | Java 25, Spring Boot 4.1.2 | 8082 | MongoDB `binitech_product_import` (com GridFS) | público |
| `BiniTech-Fiscal` | NFC-e e NF-e de devolução direto na SEFAZ | Java 25, Spring Boot 4.1.2 | 8083 | MongoDB `binitech_fiscal` | público |
| `BiniTech-Alerts` | Alertas de lentidão e queda da produção | Kotlin, Spring Boot 4.1.2 | 8084 | MongoDB `binitech_alerts` | público |
| `BiniTech-Alerts-App` | App Android que toca alarme quando chega um alerta | Kotlin, Jetpack Compose | não se aplica | não tem | público |
| `BiniTech-Verify` | Códigos de verificação por e-mail, SMS ou WhatsApp | Java 25, Spring Boot 4.1.2 | 8085 | MongoDB `binitech_verify` | público |

Observações:

- O `BiniTech-Alerts-App` é o app Android de plantão. Não tem CI nem deploy: o APK é gerado com `gradle assembleRelease` e instalado no celular, onde se informa a URL do Alerts e o `ALERTS_API_TOKEN`.
- O `BiniTech-Verify` é novo. O código está na `main`, mas o serviço ainda não tem deploy em produção e nenhum outro serviço o chama. A integração com o Auth é a tarefa BTPDV-220.
- Cada serviço é dono do seu banco. Um serviço nunca lê o banco de outro: quando precisa de um dado, chama a API do dono.
- O README de cada repositório detalha a API e as variáveis daquele serviço. Alguns trechos estão desatualizados; veja a [seção 14](#14-pontos-de-atenção-conhecidos).

## 3. Como os serviços conversam

Desenho: [`01-visao-geral.excalidraw`](diagramas/01-visao-geral.excalidraw).

### Chamadas HTTP

| De | Para | Como | Para quê |
|---|---|---|---|
| Navegador | frontend | HTTPS | Baixar a aplicação Angular (arquivos estáticos no Nginx) |
| frontend | PDV | REST em `/api/**`, base em `API_BASE` | Quase tudo: login, vendas, produtos, assinatura, Pix, delivery |
| frontend | Product-Import | REST, base em `IMPORT_API_BASE` | Enviar a planilha, revisar e acompanhar a importação |
| frontend | Fiscal | REST em `/api/fiscal/**`, base em `FISCAL_API_BASE` | Dados fiscais da loja, emitir, cancelar e baixar o cupom |
| PDV | Auth | HTTP na rede privada, `AUTH_SERVICE_URL` | Login, refresh, logout, validação de sessão e 2FA |
| PDV | Auth | `/api/internal/identities/**` com o cabeçalho `X-Auth-Service-Key` | Criar conta, trocar e recuperar senha, zerar 2FA |
| Product-Import | Auth | `GET /api/auth/session` | Validar o token do usuário |
| Product-Import | PDV | `/api/internal/product-import/{authorize,lookup,apply}` com `X-Product-Import-Service-Key` | Confirmar usuário e loja, consultar e gravar produtos em lote |
| Fiscal | PDV | `GET /api/auth/me`, repassando o token do usuário | Validar a sessão e saber o papel do usuário |
| Alerts | PDV, Fiscal e site | `GET` a cada 30 s nas URLs de `ALERTS_PROBE_TARGETS` | Sonda de disponibilidade |
| App Android | Alerts | SSE em `/api/alerts/stream`, Bearer `ALERTS_API_TOKEN` | Receber os alertas e tocar o alarme |

O navegador nunca chama o Auth direto: login, refresh e logout passam pelo PDV (`/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`).

O frontend descobre as três URLs de API em tempo de execução. No start do contêiner, o script `docker-entrypoint.d/40-env-js.sh` gera o `env.js` com `window.__env` a partir das variáveis `API_BASE`, `IMPORT_API_BASE`, `FISCAL_API_BASE`, `GA_MEASUREMENT_ID` e `META_PIXEL_ID`. Mudar uma URL não exige novo build.

### Mensagens assíncronas

O PDV não chama o Fiscal nem o Alerts por HTTP. Ele publica eventos no RabbitMQ e cada serviço consome a sua fila. Detalhes na [seção 6](#6-mensageria-rabbitmq).

## 4. Arquitetura dentro de cada serviço

Desenho: [`03-arquitetura-hexagonal.excalidraw`](diagramas/03-arquitetura-hexagonal.excalidraw).

Todos os serviços de backend seguem **arquitetura hexagonal** (portas e adaptadores) com a mesma organização de pacotes:

```
domain/                 entidades, regras e exceções de negócio
application/
  ports/inbound/        o que o serviço oferece (interfaces *UseCasePort)
  ports/outbound/       o que o serviço precisa de fora (repositórios, gateways, publicadores)
  usecases/             implementação dos casos de uso
adapters/
  inbound/              controllers HTTP e consumidores de fila
  outbound/             MongoDB, Redis, RabbitMQ e clientes HTTP de outros serviços
config/                 montagem dos beans, segurança, filas, jobs agendados
```

A regra é uma só: **as dependências apontam para dentro**. Um adaptador conhece a porta; a porta e o domínio nunca conhecem um adaptador. No Auth e no Verify, o teste `HexagonalArchitectureTest` compila `domain` e `application` sozinhos e quebra o build se alguém importar Spring, MongoDB ou um adaptador nessas camadas.

### API-first

PDV, Product-Import, Fiscal, Alerts e Verify são **API-first**:

1. O contrato fica em `src/main/resources/openapi/swagger.yaml`.
2. No build, o `openapi-generator` (7.27.0) gera as interfaces `*Api` e os DTOs.
3. O controller implementa a interface gerada.

Para criar ou mudar um endpoint, comece pelo `swagger.yaml`; não edite código gerado. Com o serviço no ar, a documentação interativa fica em `/swagger-ui.html` e o contrato em `/api-docs`. O Auth é a exceção: os controllers dele são escritos à mão e a API está descrita no README.

### Padrões de código

- **Formatação:** o `fmt-maven-plugin` (google-java-format) roda no build e reformata o código. Um arquivo com erro de sintaxe faz o build falhar já nessa etapa, com `Failed to format file`.
- **Sem comentários no código.** Nomes claros e testes explicam a intenção.
- **Dinheiro:** `BigDecimal`, gravado como texto no MongoDB (`spring.data.mongodb.representation.big-decimal: string`).
- **Threads virtuais** ligadas em todos os serviços (`spring.threads.virtual.enabled: true`).
- **Logs:** nunca registre senha, token, chave ou dado pessoal. No PDV, use o `LogSanitizer`.
- **Mensagens para o usuário** em português, prontas para aparecer na tela.

## 5. Autenticação e autorização

Desenho: [`04-autenticacao.excalidraw`](diagramas/04-autenticacao.excalidraw).

**Autenticação é do Auth; autorização é do PDV.** O Auth sabe quem é o usuário e se a sessão vale. O PDV sabe o papel dele (`role`), a que loja pertence e se a loja está ativa.

1. **Login.** O navegador chama `POST /api/auth/login` no PDV, que repassa ao Auth. O Auth devolve um `accessToken` (JWT) e um `refreshToken` (opaco, de uso único). Os padrões são 15 minutos e 24 horas; em produção os valores vêm de `JWT_ACCESS_EXPIRATION` e `JWT_REFRESH_EXPIRATION`.
2. **2FA.** Se o usuário ativou o segundo fator, o login devolve um desafio em vez de tokens e termina em `POST /api/auth/login/two-factor`, com o código do aplicativo autenticador ou um código de recuperação.
3. **Chamada autenticada ao PDV.** O navegador manda `Authorization: Bearer <accessToken>`. O PDV valida a sessão no Auth (`GET /api/auth/session`, com cache de 30 segundos por padrão, `AUTH_SESSION_CACHE_TTL`) e aplica papel e situação da loja a partir do próprio cadastro. Se o Auth não responder, a requisição é negada.
4. **Chamada ao Fiscal.** O Fiscal repassa o token ao PDV em `GET /api/auth/me` e recebe usuário, papel e loja.
5. **Chamada ao Product-Import.** O importador valida o token no Auth e depois confirma usuário, papel e loja no PDV. O `tenantId` nunca vem do frontend.

Chamadas entre serviços sem usuário usam **chave de serviço** em cabeçalho, comparada em tempo constante:

| Cabeçalho | Quem envia | Quem confere |
|---|---|---|
| `X-Auth-Service-Key` | PDV | Auth, em `/api/internal/identities/**` |
| `X-Product-Import-Service-Key` | Product-Import | PDV, em `/api/internal/product-import/**` |
| `X-Verify-Service-Key` | quem chamar o Verify (futuramente o Auth) | Verify, em `/api/verifications/**` |

Essas chaves nunca vão para o navegador.

## 6. Mensageria (RabbitMQ)

Desenho: [`05-mensageria.excalidraw`](diagramas/05-mensageria.excalidraw).

| Exchange (tipo) | Chave de roteamento | Fila | Publica | Consome |
|---|---|---|---|---|
| `binitech.sales` (topic) | `sale.completed` | `fiscal.sales.completed` | PDV | Fiscal |
| `binitech.sales` (topic) | `sale.returned` | `fiscal.sales.returned` | PDV | Fiscal |
| `binitech.observability` (topic) | `request.slow` | `alerts.requests.slow` | PDV | Alerts |
| `email.exchange` (topic) | `email.approval` | `email.queue` | PDV | PDV (envia pelo Resend) |
| `product-import.exchange` (direct) | `product-import.process` | `product-import.jobs` | Product-Import | Product-Import |

Garantias que você precisa conhecer antes de mexer:

- **Vendas não se perdem.** O PDV publica com confirmação do broker (publisher confirms). Se o RabbitMQ não confirmar, a venda fica marcada e o `SaleFiscalOutboxJob` tenta de novo a cada 30 segundos.
- **Mensagem repetida não duplica nota.** O Fiscal tem índice único por venda (`tenantId + saleId`) e por devolução (`tenantId + returnId`).
- **Fila morta.** No Fiscal, uma mensagem que falha é reprocessada até 6 vezes, com espera crescente de 2 a 60 segundos, e depois vai para `fiscal.sales.completed.dead` ou `fiscal.sales.returned.dead`. No Product-Import, a mensagem rejeitada vai para `product-import.jobs.dead`.
- **Lentidão não inunda a fila.** O PDV publica `request.slow` para requisições acima de 1 segundo, no máximo um evento por rota a cada 10 segundos. Na fila do Alerts a mensagem vale 10 minutos e o limite é de 10.000 mensagens.
- Quem declara a fila é quem consome. O PDV só declara os exchanges e a `email.queue`.

Ao criar um evento novo: defina o registro do evento no publicador, declare fila e ligação no consumidor, torne o consumo idempotente e pense no que acontece quando a mensagem chega duas vezes ou fora de ordem.

## 7. Integrações externas

Desenho: [`02-integracoes-externas.excalidraw`](diagramas/02-integracoes-externas.excalidraw).

| Serviço | Sistema externo | Uso |
|---|---|---|
| PDV | **Asaas** | Assinatura da loja por cartão ou Pix. API REST v3 e webhook em `POST /webhooks/asaas`, autenticado por token |
| PDV | **Mercado Pago** | Pix das vendas e do delivery, com QR dinâmico e baixa automática pelo webhook `/webhooks/mercadopago` |
| PDV | **Resend** | E-mails transacionais (recuperação de senha, avisos do teste grátis) |
| PDV | **Meta Conversions API** | Evento de cadastro de loja, só quando o visitante consentiu |
| frontend | **Google Analytics 4** e **Meta Pixel** | Eventos de uso, sem dados pessoais |
| Fiscal | **SEFAZ** | Emissão de NFC-e e NF-e por SOAP, assinada com o certificado A1 de cada loja |
| Fiscal | **ViaCEP** | Endereço e código IBGE a partir do CEP |
| Verify | **Resend**, **Mobizon** e **WhatsApp Cloud API** | Envio do código por e-mail ou SMS; no WhatsApp o cliente manda o código e a Meta avisa pelo webhook `/webhooks/whatsapp` |

Sem a chave de um provedor, o recurso correspondente fica desligado. Para desenvolver, use as contas de teste (sandbox) de cada provedor; nunca use credenciais de produção na sua máquina.

## 8. Rodando na sua máquina

### Pré-requisitos

- Java 25 (Temurin) e Docker.
- Node.js 25 para o frontend.
- Maven não é obrigatório nos repositórios que trazem o wrapper (`./mvnw`): PDV, Auth e Verify. Nos demais, use o Maven 3.9 instalado.

### Subir o essencial (PDV + Auth + frontend)

```bash
git clone https://github.com/niicsz/BiniTech-PDV.git
git clone https://github.com/niicsz/BiniTech-PDV-frontend.git

cd BiniTech-PDV
docker compose up -d mongodb rabbitmq
docker compose -f docker-compose.auth.yml up -d
./mvnw spring-boot:run
```

Em outro terminal:

```bash
cd BiniTech-PDV-frontend
npm ci
npm start
```

O frontend sobe em <http://localhost:4200> e encaminha `/api` para `http://localhost:8080` (`proxy.conf.json`).

O que cada arquivo de compose sobe:

| Arquivo | Serviços |
|---|---|
| `docker-compose.yml` | `mongodb` (27017), `rabbitmq` (5672, painel em 15672), `product-import-mongodb` (27018) e `product-import-service` (8082) |
| `docker-compose.auth.yml` | `auth-mongodb` (27018), `redis` (6379) e `auth-service` (8081), construído a partir do repositório do Auth |

O PDV lê variáveis de um arquivo `.env` na raiz. As obrigatórias são `MONGODB_URI`, `REDIS_URL`, `AUTH_SERVICE_KEY`, `PRODUCT_IMPORT_SERVICE_KEY`, `ADMIN_USERNAME` e `CORS_ALLOWED_ORIGINS`. A lista completa, com padrões, está em `src/main/resources/application.yaml` e no README de cada serviço. Gere segredos locais com `openssl rand -base64 48` e nunca copie os de produção.

### Testes

| Repositório | Comando | O que roda |
|---|---|---|
| Backends | `./mvnw verify` (ou `mvn verify`) | Formatação, geração do OpenAPI, compilação e todos os testes |
| frontend | `npm run build -- --configuration=production` e `npm run test:auth` | Build de produção e testes de renovação de sessão |

Os testes de backend não precisam de banco externo: usam portas simuladas, `MockMvc`, `MockRestServiceServer` e, onde há teste de repositório, MongoDB embarcado.

## 9. Do branch à produção

Desenho: [`06-fluxo-de-pr.excalidraw`](diagramas/06-fluxo-de-pr.excalidraw).

### Passo a passo

1. **Branch a partir da `main`**, com prefixo do tipo de mudança: `feat/`, `fix/`, `chore/`, `docs/`, `ci/` ou `infra/`. Exemplo: `feat/delivery-pix-qr`.
2. **Código e testes locais.** Rode `./mvnw verify` (backend) ou o build e os testes do frontend antes de enviar.
3. **Commits** no padrão Conventional Commits, em inglês: `feat: Pix QR code for delivery orders`. Não inclua linhas de atribuição a ferramentas de IA em commits nem em PRs.
4. **Abra o PR para a `main`.** Título e descrição em português. A descrição diz o que entra, como foi testado e o **plano de rollback**. Se a mudança tem tarefa no kanban, cite o código (`BTPDV-123`).
5. **Checks automáticos.** Veja a tabela abaixo. Corrija o que falhar com novos commits no mesmo PR.
6. **Homologação.** Ao abrir o PR, um bot comenta "Subir para homologação? Responda sim ou não". Só a resposta de quem é dono ou colaborador do repositório conta.
7. **Gate.** Depois do "sim", o workflow confere que o commit ainda é o mais recente do PR e espera até 20 minutos por todos os checks exigidos. Se um falhar, não sobe e o bot explica qual. Se entrar commit novo depois do "sim", é preciso responder de novo.
8. **Aprovação.** O deploy usa o environment `homologacao`, que exige a aprovação do mantenedor (`niicsz`).
9. **Deploy em homologação.** O workflow roda `railway up` no projeto `binitech-homolog` e confere o healthcheck: três respostas `UP` seguidas em `/actuator/health` (no frontend, resposta 200 na página inicial). O bot avisa no PR.
10. **Validação.** Teste o fluxo alterado em homologação.
11. **Merge.** O mantenedor aprova (check **Aprovação do mantenedor**) e faz o merge, só com tudo verde. O histórico da `main` é linear (squash ou rebase).
12. **Produção.** A Railway detecta o push na `main`, constrói a imagem pelo `Dockerfile` e troca a versão quando o healthcheck responde. Confira os logs e o fluxo real.

### Checks por repositório

| Repositório | GitHub Actions (`ci.yml`) | Outros checks no PR |
|---|---|---|
| PDV | Backend Build & Test, Trivy Dependency Scan, Docker Build | SonarCloud, CodeQL (`actions` e `java-kotlin`), Snyk, revisão automática do Copilot |
| frontend | Frontend Build, Docker Build | SonarCloud |
| Auth | Tests and hexagonal architecture, Trivy Dependency Scan, Docker build | SonarCloud, Snyk |
| Fiscal | Backend Build & Test, Trivy Dependency Scan, Docker Build | SonarCloud |
| Product-Import | Backend Build & Test, Trivy Dependency Scan, Docker Build | SonarCloud |
| Alerts | Backend Build & Test, Trivy Dependency Scan, Docker Build | nenhum |
| Verify | Backend Build & Test, Trivy Dependency Scan, Docker Build | nenhum |
| Alerts-App | não tem CI | nenhum |

O Trivy falha o build se achar vulnerabilidade `HIGH` ou `CRITICAL` nas dependências empacotadas.

### O que é regra do GitHub e o que é regra do time

- Em todos os repositórios, a regra **Review do mantenedor** exige PR para alterar a `main`, com 1 aprovação de code owner (`@niicsz`, pelo `.github/CODEOWNERS`). Aprovação antiga cai quando entra commit novo. O administrador do repositório pode dispensar essa regra nos próprios PRs, porque o GitHub não deixa o autor aprovar o próprio PR.
- Para valer também nos PRs do mantenedor, o workflow **Aprovação para merge** cria o check **Aprovação do mantenedor**, que só fica verde quando `@niicsz` aprova no environment `aprovacao-merge`. Cada commit novo pede aprovação de novo. A regra **Aprovação para merge** torna esse check obrigatório, sem exceção para ninguém, nem para administrador. O workflow usa `pull_request_target`: roda sempre a versão da `main`, então um PR não consegue alterar o próprio check. Um PR aberto antes de 13/11/2026 precisa ser atualizado com a `main` para o check aparecer.
- No **PDV**, outra regra impede apagar o branch e fazer force push, e pede revisão automática do Copilot.
- Os demais checks (CI, SonarCloud, CodeQL, Snyk) não são obrigatórios no GitHub: "só faz merge com tudo verde" é o critério do mantenedor ao aprovar. O gate automático desses checks existe na subida para homologação.
- O fluxo de homologação (passos 6 a 9) hoje está ativo no **PDV** e no **frontend**. Nos outros repositórios o workflow está em PR aberto: Auth #13, Fiscal #8, Product-Import #7 e Alerts #3.

### Rollback

- **Código:** na Railway, abra o serviço, escolha o deploy anterior e use **Redeploy**. Depois reverta o commit na `main` com um PR, para o próximo deploy não trazer o problema de volta.
- **Configuração:** se a mudança depende de variável ou feature flag, o rollback mais rápido é desligar a flag.
- **Dados:** descreva no PR se a mudança altera dados de forma que a versão anterior não entende. Prefira mudanças compatíveis nos dois sentidos.

## 10. Infraestrutura e deploy na Railway

Desenho: [`07-infra-railway.excalidraw`](diagramas/07-infra-railway.excalidraw).

Tudo roda na Railway, em dois projetos separados.

### Produção: projeto `steadfast-growth`

| Serviço | Origem do deploy | Endereço público |
|---|---|---|
| `frontend` | GitHub `BiniTech-PDV-frontend`, branch `main` | `www.binitechpdv.com.br` e `binitech-pdv.up.railway.app` |
| `BiniTech-PDV` | GitHub `BiniTech-PDV`, branch `main` | `binitech-pdv-api.up.railway.app` |
| `BiniTech-Auth` | GitHub `BiniTech-Auth`, branch `main` | `binitech-auth-production.up.railway.app` |
| `BiniTech-Fiscal` | GitHub `BiniTech-Fiscal`, branch `main` | `binitech-fiscal-production.up.railway.app` |
| `BiniTech-Product-Import` | GitHub `BiniTech-Product-Import`, branch `main` | `binitech-product-import-production.up.railway.app` |
| `BiniTech-Alerts` | Deploy manual com `railway up` (repositório ainda não ligado à Railway) | `binitech-alerts-production.up.railway.app` |
| `BiniTech-Verify` | Serviço criado, ainda sem deploy | não tem |
| `rabbitmq` | Imagem `rabbitmq:4-management-alpine` | só rede privada |
| `MongoDB` | Imagem `mongo:8.0` | só rede privada; bancos do Auth e do Alerts |
| `MongoDB-jKw9` | Imagem `mongo:8.0` | só rede privada; banco do Product-Import |

O MongoDB e o Redis usados pelo PDV e pelo Fiscal são acessados pelo proxy TCP da Railway (endereços `*.proxy.rlwy.net`).

Os serviços se falam pela rede privada quando possível: o PDV e o Product-Import chamam o Auth em `binitech-auth.railway.internal:8081`, e o Product-Import chama o PDV em `binitech-pdv.railway.internal:8080`. O Fiscal chama o PDV pelo endereço público.

### Homologação: projeto `binitech-homolog`

| Serviço | Endereço público |
|---|---|
| `api` (PDV) | `api-production-a15a0.up.railway.app` |
| `frontend` | `frontend-production-e05e2.up.railway.app` |
| `fiscal` | `fiscal-production-aac1.up.railway.app` |
| `alerts` | `alerts-production-304b.up.railway.app` |
| `auth` | não tem |
| `verify` | não tem |
| `rabbitmq` (`rabbitmq:3-management`), `MongoDB` (`mongo:8.0`), `Redis` (`redis:8.2`) | só rede privada |

Nenhum serviço de homologação está ligado ao GitHub: o deploy vem do workflow "Subir para homologação", a partir do PR. Não existe Product-Import em homologação.

### Como um deploy acontece

- O build usa o `Dockerfile` do repositório, em duas etapas: nos backends, compila com JDK 25 e roda com JRE 25, como usuário sem privilégios; no frontend, builda com Node 25 e serve pelo Nginx.
- O `railway.json` define o healthcheck em `/actuator/health` (espera de até 300 segundos) e reinício em caso de falha (`ON_FAILURE`, até 10 tentativas). A versão nova só recebe tráfego depois que o healthcheck responde.
- As variáveis de ambiente ficam no serviço da Railway, nunca no repositório. Uma variável pode apontar para a de outro serviço, por exemplo `${{MongoDB.MONGO_URL}}`.
- O CORS de cada backend libera só os domínios do frontend (`CORS_ALLOWED_ORIGINS`).

## 11. Mudanças que tocam mais de um serviço

Uma funcionalidade pode exigir PRs em vários repositórios. Combine a ordem antes e escreva essa ordem na descrição de cada PR.

Regras práticas:

1. **Quem oferece vai antes de quem consome.** Publique primeiro o serviço que ganha o endpoint ou passa a aceitar o campo novo; depois o que chama. Exemplos já praticados: Auth, depois PDV, depois frontend; Fiscal, depois PDV, depois frontend.
2. **Mudança compatível.** Campo novo é opcional até todos os consumidores estarem atualizados. Não remova nem renomeie campo de contrato ou de evento no mesmo deploy em que o consumidor ainda usa o antigo.
3. **Eventos:** o consumidor tem que aceitar a mensagem antiga e a nova enquanto os dois lados não estiverem atualizados.
4. **Variáveis antes do código.** Crie a variável na Railway antes do merge que precisa dela, para o deploy não subir quebrado.
5. **Flag desligada primeiro.** Funcionalidade grande entra desligada, é ligada em homologação e só então em produção.

## 12. Feature flags

São variáveis de ambiente lidas na subida do serviço. O padrão no código é sempre **desligado**.

| Variável | Serviço | O que liga | Em produção hoje |
|---|---|---|---|
| `PIX_ENABLED` | PDV | Pix integrado com o Mercado Pago | `true` |
| `DELIVERY_ENABLED` | PDV | Delivery e página pública de pedidos | `true` |
| `SIGNUP_AUTO_APPROVE` | PDV | Loja nova entra ativa, sem aprovação manual (padrão `true`) | padrão |
| `VERIFY_WHATSAPP_ENABLED` | Verify | Canal WhatsApp e o webhook dele | `false` |
| `VERIFY_LOG_CODES` | Verify | Escreve o código no log quando não há provedor; só para desenvolvimento | não definida |
| `FISCAL_ENVIRONMENT` | Fiscal | `2` usa a SEFAZ de homologação, `1` a de produção | `2` |

Para criar uma flag: propriedade no `application.yaml` com padrão desligado, teste para os dois estados e uma linha no README do serviço.

## 13. Segredos e segurança

- **Nada de segredo no repositório**, em issue, PR, log ou conversa. Segredos ficam nas variáveis da Railway e nos secrets do GitHub.
- Se um segredo aparecer onde não devia, trate como vazado: gere outro, troque na Railway e avise o mantenedor.
- Chaves de serviço têm no mínimo 32 caracteres e são comparadas em tempo constante.
- Senhas usam Argon2 com pepper; segredos de 2FA, certificados A1 e credenciais do Mercado Pago ficam cifrados com AES-256-GCM.
- Webhooks são autenticados: token no Asaas, assinatura no Mercado Pago e na Meta.
- Dado de uma loja nunca aparece para outra: toda consulta filtra por `tenantId`, que vem da sessão e nunca do corpo da requisição.
- Entrada do usuário tem limite de tamanho e validação no contrato OpenAPI e no domínio.
- Vulnerabilidade encontrada: avise o mantenedor em privado, não abra issue pública.

## 14. Pontos de atenção conhecidos

Coisas que hoje podem confundir quem chega:

- **`docs/architecture.md` do PDV está desatualizado.** Ele descreve Stripe e um contêiner único com o frontend embutido. Hoje a cobrança é pelo Asaas e o frontend tem repositório e deploy próprios. Use este guia.
- **READMEs com versões antigas.** O README do PDV fala em Spring Boot 3 no primeiro parágrafo e o do Auth em Java 21; os dois serviços rodam em Java 25 e Spring Boot 4.1.2. O README do frontend ainda cita Stripe.
- **Porta 27018 repetida.** `docker-compose.yml` (Mongo do importador) e `docker-compose.auth.yml` (Mongo do Auth) publicam a mesma porta no host. Para subir os dois arquivos completos ao mesmo tempo, mude uma delas.
- **Redis fora do `docker-compose.yml`.** O Redis local vem do `docker-compose.auth.yml`.
- **CI do PDV só roda em PR.** Nos outros repositórios o CI também roda no push para a `main`.
- **Bloqueio de merge.** A aprovação do mantenedor é exigida pelo GitHub; os checks de CI ainda não são obrigatórios. Veja a [seção 9](#o-que-é-regra-do-github-e-o-que-é-regra-do-time).
- **Alerts sem `railway.json` na `main`.** O healthcheck e a política de reinício dele estão no PR #2.
- **Fiscal em homologação da SEFAZ.** `FISCAL_ENVIRONMENT=2` em produção: as notas ainda não têm valor fiscal.

## 15. Glossário

| Termo | Significado |
|---|---|
| **Tenant** | Uma loja cliente. Todos os dados dela carregam o `tenantId` |
| **PDV** | Ponto de venda; também o nome do serviço principal |
| **Fiado / crediário** | Venda para pagar depois, com controle de devedores |
| **Turno de caixa** | Abertura, sangria, suprimento e fechamento do caixa |
| **NFC-e** | Nota fiscal de consumidor eletrônica (modelo 65), o cupom da venda |
| **NF-e de devolução** | Nota modelo 55 que reverte uma venda com NFC-e autorizada |
| **SEFAZ** | Secretaria da Fazenda do estado, que autoriza as notas |
| **Certificado A1** | Certificado digital da loja, usado para assinar as notas |
| **Contingência** | Emissão offline da NFC-e quando a SEFAZ está fora do ar |
| **2FA / TOTP** | Segundo fator de login com código do aplicativo autenticador |
| **Chave de serviço** | Segredo que um serviço envia a outro para provar quem é |
| **Fila morta (DLQ)** | Fila para onde vai a mensagem que falhou em todas as tentativas |
| **Homologação** | Ambiente de teste parecido com a produção (projeto `binitech-homolog`) |
| **Healthcheck** | Rota `/actuator/health`, usada para saber se o serviço está no ar |
| **BTPDV-123** | Código de uma tarefa no kanban do projeto |
