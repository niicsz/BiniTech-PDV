# Plano de Implementação — BiniTech PDV: Multi-Tenancy + SaaS Billing

> Este documento descreve, em formato de **User Stories**, tudo o que foi implementado no
> conjunto de mudanças não commitadas analisado em **2026-06-03**. As alterações foram
> **revertidas/removidas** após a geração deste plano, para serem refeitas de forma
> organizada e incremental seguindo este roteiro.
>
> Stack: **Backend** Spring Boot (arquitetura hexagonal — ports & adapters, MongoDB, Redis,
> JWT) · **Frontend** Angular (standalone components, Material) · **Infra** Docker Compose,
> RabbitMQ, Stripe.

---

## Visão Geral

O objetivo central é transformar o PDV de uma aplicação **single-tenant** (um único negócio,
usuários `ADMIN`/`OPERATOR`) em uma plataforma **SaaS multi-tenant** com:

- Isolamento de dados por `tenantId` em todas as entidades de negócio.
- Hierarquia de papéis: `SUPER_ADMIN` (dono da plataforma) → `TENANT_ADMIN` (dono de um
  cliente/loja) → `ADMIN`/`OPERATOR` (usuários internos do tenant).
- Auto-cadastro público de novos tenants (signup), com aprovação manual pelo super admin.
- Provisionamento automático do usuário admin do tenant + e-mail de boas-vindas com senha
  temporária (via RabbitMQ + JavaMail).
- Cobrança recorrente (assinaturas, planos, faturas com excedentes) integrada ao
  **Stripe** (Billing + Checkout) via webhooks, com bloqueio automático de inadimplentes.
- Páginas públicas de marketing (landing, sobre nós, termos, privacidade) e dashboard
  administrativo do super admin.

---

## EPIC 1 — Fundação Multi-Tenant

### US-1.1 — Modelo de papéis hierárquicos
**Como** plataforma SaaS, **quero** papéis distintos para o dono da plataforma, o dono do
tenant e os usuários internos, **para** controlar permissões em cada nível.

**Critérios de aceite:**
- Enum `Role` passa a conter `SUPER_ADMIN`, `ADMIN`, `TENANT_ADMIN`, `OPERATOR`.
- `SUPER_ADMIN` não pertence a nenhum tenant (`tenantId == null`).
- `TENANT_ADMIN` pode registrar usuários dentro do próprio tenant.

**Arquivos:** `utils/Enum/Role.java`, `openapi/swagger.yaml` (enum de roles).

### US-1.2 — Campo `tenantId` nas entidades de negócio
**Como** sistema, **quero** vincular cada registro a um tenant, **para** isolar os dados de
cada cliente.

**Critérios de aceite:**
- `User`, `Product`, `Sale`, `RefreshToken` recebem o campo `tenantId` (domínio + documento
  Mongo + mapper de persistência).
- Construtores legados mantidos por sobrecarga (`tenantId = null`) para compatibilidade.
- `toString()` atualizado incluindo `tenantId`.

**Arquivos:** `domain/{User,Product,Sale,RefreshToken}.java`,
`adapters/outbound/persistence/document/{User,Product,Sale,RefreshToken}Document.java`,
`adapters/outbound/persistence/mapper/PersistenceMapper.java`.

### US-1.3 — Entidade Tenant
**Como** super admin, **quero** uma entidade Tenant, **para** representar cada cliente da
plataforma com seu ciclo de vida.

**Critérios de aceite:**
- Domínio `Tenant`: `id`, `name`, `slug` (único), `status`, `planId`, `billingEmail`,
  `trialEndsAt`, `blockedAt`, `blockReason`, `createdAt`, `updatedAt`.
- Enum `TenantStatus`: `PENDING_APPROVAL`, `ACTIVE`, `BLOCKED`, `CANCELLED`.
- Documento Mongo `TenantDocument`, repositório `SpringDataTenantRepository`
  (`findBySlug`, `existsBySlug`, `findAllByStatus`), porta `TenantRepositoryPort` e adapter
  `TenantRepositoryAdapter`.

**Arquivos:** `domain/Tenant.java`, `utils/Enum/TenantStatus.java`,
`document/TenantDocument.java`, `repository/SpringDataTenantRepository.java`,
`ports/outbound/TenantRepositoryPort.java`,
`adapters/outbound/persistence/TenantRepositoryAdapter.java`.

### US-1.4 — Propagação do `tenantId` no JWT e contexto de segurança
**Como** sistema, **quero** carregar o `tenantId` no token de acesso, **para** escopar todas
as requisições autenticadas sem nova consulta.

**Critérios de aceite:**
- `JwtTokenProvider.generateAccessToken(...)` inclui a claim `tenantId`; novo
  `getTenantIdFromToken(...)`.
- `JwtAuthenticationFilter` lê a claim e a coloca em `authentication.setDetails(tenantId)`.
- `AuthenticatedUserProvider.getTenantId()` expõe o `tenantId` do contexto.

**Arquivos:** `config/JwtTokenProvider.java`, `config/JwtAuthenticationFilter.java`,
`adapters/inbound/web/AuthenticatedUserProvider.java`.

### US-1.5 — Escopo de dados por tenant nas consultas
**Como** tenant, **quero** ver apenas meus produtos/vendas/usuários, **para** garantir o
isolamento entre clientes.

**Critérios de aceite:**
- Repositórios Spring Data ganham métodos `...ByTenantId` /
  `findByUsernameAndTenantId` / `findByUsernameAndTenantIdIsNull` /
  `existsByUsernameAndTenantId` / `countByTenantId` / `countByTenantIdAndActiveTrue` /
  `deleteByUserIdAndTenantId`.
- `ProductUseCaseImpl` e `SaleUseCaseImpl` passam a filtrar/gravar por `tenantId` obtido do
  usuário autenticado; portas inbound/outbound de Product e Sale ajustadas.
- Controllers de Product e Sale repassam o `tenantId` do `AuthenticatedUserProvider`.

**Arquivos:** `repository/SpringData{User,Product,Sale,RefreshToken}Repository.java`,
`ports/{inbound,outbound}/{Product,Sale,User,RefreshToken}*Port.java`,
`usecases/{Product,Sale}UseCaseImpl.java`,
`adapters/.../{Product,Sale,User,RefreshToken}RepositoryAdapter.java`,
`web/{Product,Sale}Controller.java`, `web/mapper/WebMapper.java`.

### US-1.6 — Filtro de validação de tenant
**Como** plataforma, **quero** bloquear requisições de tenants inválidos/suspensos, **para**
fazer cumprir o status da assinatura em tempo de requisição.

**Critérios de aceite:**
- `TenantValidationFilter` roda após o `JwtAuthenticationFilter` para rotas `/api/**`
  (exceto `/api/auth/**`, `/api/admin/**`, `/api/public/**`).
- Respostas de erro JSON: `TENANT_NOT_FOUND` (403), `TENANT_BLOCKED`/`CANCELLED` (402
  Payment Required), `TENANT_PENDING` (403).

**Arquivos:** `config/TenantValidationFilter.java`, `config/SecurityConfig.java`.

### US-1.7 — Regras de autorização atualizadas
**Como** plataforma, **quero** rotas protegidas por papel, **para** separar área pública,
do tenant e do super admin.

**Critérios de aceite:**
- `/api/public/**` e `/webhooks/**` liberados; CSRF ignora `/webhooks/**`.
- `/api/admin/**` exige `SUPER_ADMIN`.
- `/api/auth/register` permite `SUPER_ADMIN`, `ADMIN`, `TENANT_ADMIN`.
- `/actuator/**` permite `SUPER_ADMIN` e `ADMIN`.

**Arquivos:** `config/SecurityConfig.java`.

---

## EPIC 2 — Autenticação Multi-Tenant

### US-2.1 — Login com escopo de tenant
**Como** usuário de um tenant, **quero** autenticar dentro do meu tenant, **para** evitar
colisão de usernames entre tenants.

**Critérios de aceite:**
- `AuthUseCasePort.login(username, password, tenantId)`; quando `tenantId` informado usa
  `findByUsernameAndTenantId`, senão `findByUsernameAndTenantIdIsNull` (super admin).
- `AuthResult` passa a carregar `tenantId`; refresh tokens criados/excluídos por
  `(userId, tenantId)`.
- Proteção contra timing attack mantida (dummy hash defensivo) e tratamento de hash inválido.

**Arquivos:** `ports/inbound/AuthUseCasePort.java`, `usecases/AuthUseCaseImpl.java`,
`web/AuthController.java`, `swagger.yaml` (DTOs com `tenantId`).

### US-2.2 — Registro com associação obrigatória de tenant
**Como** TENANT_ADMIN, **quero** criar usuários ligados ao meu tenant, **para** gerenciar
minha equipe.

**Critérios de aceite:**
- `register(username, password, role, tenantId)`; valida que papéis não-`SUPER_ADMIN` exigem
  `tenantId` (`validateTenantAssociation`).
- Se o request não traz `tenantId`, herda o do usuário autenticado.
- Unicidade de username verificada por tenant.

**Arquivos:** `usecases/AuthUseCaseImpl.java`, `web/AuthController.java`.

### US-2.3 — Super admin como bootstrap da plataforma
**Como** operador da plataforma, **quero** um super admin padrão, **para** acessar a área
administrativa no primeiro deploy.

**Critérios de aceite:**
- `DataInitializer` cria/atualiza o admin padrão como `SUPER_ADMIN` com `tenantId = null`
  (lookup via `findByUsernameAndTenantIdIsNull`).

**Arquivos:** `config/DataInitializer.java`.

---

## EPIC 3 — Onboarding de Tenants (Auto-cadastro)

### US-3.1 — Signup público de tenant
**Como** visitante, **quero** cadastrar minha loja escolhendo um plano, **para** começar a
usar o sistema.

**Critérios de aceite:**
- `POST /api/public/tenants` (`CreateTenantRequest`: name, planId ∈ {starter,pro,enterprise},
  billingEmail válido) cria o tenant com `status = PENDING_APPROVAL`.
- `slug` gerado a partir do nome (normalização NFD, lowercase, hífens), unicidade validada.
- `GET /api/public/tenants/slug/{slug}` para consulta pública.

**Arquivos:** `web/TenantController.java`, `ports/inbound/TenantUseCasePort.java`,
`usecases/TenantUseCaseImpl.java`.

### US-3.2 — Aprovação e provisionamento de tenant
**Como** super admin, **quero** aprovar um tenant, **para** liberar o acesso e criar o
usuário administrador inicial.

**Critérios de aceite:**
- `POST /api/admin/tenants/{id}/approve` muda status para `ACTIVE`, limpa bloqueios.
- Provisiona usuário `TENANT_ADMIN` (username = slug) com senha temporária aleatória
  (`SecureRandom`, 12 chars), idempotente (`existsByUsernameAndTenantId`).
- Dispara e-mail de boas-vindas com as credenciais.

**Arquivos:** `usecases/TenantUseCaseImpl.java`.

### US-3.3 — Bloqueio/gestão de status de tenant
**Como** super admin, **quero** bloquear ou alterar o status de um tenant, **para**
controlar inadimplência e abusos.

**Critérios de aceite:**
- `POST /api/admin/tenants/{id}/block` (`BlockTenantRequest.reason`) → `BLOCKED` com
  `blockedAt`/`blockReason`.
- `updateTenantStatus(...)` reaproveitável; `ACTIVE` limpa bloqueios.

**Arquivos:** `web/TenantController.java`, `usecases/TenantUseCaseImpl.java`.

---

## EPIC 4 — Notificações por E-mail (assíncronas)

### US-4.1 — Porta de serviço de e-mail
**Como** domínio, **quero** uma abstração de envio de e-mail, **para** manter a arquitetura
hexagonal e permitir múltiplas implementações.

**Critérios de aceite:**
- `EmailServicePort.sendTenantApprovalEmail(to, tenantName, tenantSlug, username, tempPassword)`.
- Record de domínio `EmailEvent`.

**Arquivos:** `ports/outbound/EmailServicePort.java`, `domain/EmailEvent.java`.

### US-4.2 — Publicação/consumo via RabbitMQ
**Como** sistema, **quero** enfileirar e-mails, **para** desacoplar o envio do fluxo de
aprovação e dar resiliência.

**Critérios de aceite:**
- `RabbitMQConfig`: exchange `email.exchange`, fila durável `email.queue`, routing key
  `email.approval`, conversor JSON.
- `RabbitMQEmailPublisher` (ativo com `spring.rabbitmq.host`) implementa a porta e publica
  `EmailEvent`.
- `EmailEventConsumer` (`@RabbitListener`) consome e delega ao adapter JavaMail.

**Arquivos:** `config/RabbitMQConfig.java`,
`adapters/outbound/RabbitMQEmailPublisher.java`,
`adapters/inbound/EmailEventConsumer.java`.

### US-4.3 — Envio real (JavaMail) e fallback NoOp
**Como** plataforma, **quero** enviar e-mail HTML quando configurado e logar quando não,
**para** funcionar em dev e produção.

**Critérios de aceite:**
- `JavaMailEmailServiceAdapter` (`@ConditionalOnProperty spring.mail.host`) envia e-mail HTML
  de aprovação com link para o frontend.
- `NoOpEmailServiceAdapter` (`@ConditionalOnMissingBean`) loga as credenciais quando o e-mail
  não está configurado.

**Arquivos:** `adapters/outbound/JavaMailEmailServiceAdapter.java`,
`adapters/outbound/NoOpEmailServiceAdapter.java`.

---

## EPIC 5 — Billing / Assinaturas / Stripe

### US-5.1 — Catálogo de planos e limites
**Como** plataforma, **quero** definir planos com limites e tarifas de excedente, **para**
calcular cobranças.

**Critérios de aceite:**
- `PlanConfig`: planos `starter`/`pro`/`enterprise` com `maxProducts`, `maxSalesPerMonth`,
  `maxOperators`, `baseMonthlyFee`; tarifas de excedente
  (`EXCESS_PER_PRODUCT`, `EXCESS_PER_SALE`, `EXCESS_PER_OPERATOR`). Os price ids do Stripe
  ficam em `StripeProperties` (config por ambiente).

**Arquivos:** `config/PlanConfig.java`.

### US-5.2 — Entidades de assinatura e fatura
**Como** sistema de cobrança, **quero** modelar assinaturas e faturas, **para** rastrear o
ciclo de pagamento de cada tenant.

**Critérios de aceite:**
- Domínio `Subscription` (tier, status, períodos, `nextBillingDate`, `failedPaymentCount`,
  IDs Stripe: `stripeSubscriptionId`/`stripeCustomerId`/`stripePriceId`) + enum
  `SubscriptionStatus`.
- Domínio `Invoice` (amount, baseAmount, excessAmount, status, dueDate, paidAt) + enum
  `InvoiceStatus`.
- Documentos Mongo, repositórios, portas e adapters correspondentes.

**Arquivos:** `domain/{Subscription,Invoice}.java`,
`utils/Enum/{SubscriptionStatus,InvoiceStatus}.java`,
`document/{Subscription,Invoice}Document.java`,
`repository/SpringData{Subscription,Invoice}Repository.java`,
`ports/outbound/{Subscription,Invoice}RepositoryPort.java`,
`adapters/outbound/persistence/{Subscription,Invoice}RepositoryAdapter.java`.

### US-5.3 — Caso de uso de billing
**Como** plataforma, **quero** criar assinaturas, gerar faturas com excedentes e processar
pagamentos, **para** monetizar o serviço.

**Critérios de aceite:**
- `createSubscription(tenantId, planTier)` (período de 30 dias).
- `generateMonthlyInvoice(...)` calcula base + excedentes (produtos/vendas/operadores) e
  descrição localizada.
- `markInvoicePaid(...)` (reativa tenant `BLOCKED`), `recordPaymentFailure(...)` (→
  `PAST_DUE`/`OVERDUE`), `blockOverdueTenants(graceDays)`.
- Consultas: `getInvoicesForTenant`, `getSubscriptionForTenant`.

**Arquivos:** `ports/inbound/BillingUseCasePort.java`,
`usecases/BillingUseCaseImpl.java`, `config/BeanConfig.java` (wiring dos beans
`tenantUseCasePort` e `billingUseCasePort`).

### US-5.4 — Webhook Stripe
**Como** plataforma, **quero** receber notificações de pagamento, **para** atualizar faturas
e assinaturas automaticamente.

**Critérios de aceite:**
- `POST /webhooks/stripe` verifica a assinatura via SDK do Stripe
  (`Webhook.constructEvent`, header `Stripe-Signature`).
- Trata `checkout.session.completed` (ativa assinatura/tenant), `invoice.paid` (renovação),
  `invoice.payment_failed` (registra falha) e `customer.subscription.deleted` (cancela).
- Responde 200 nos eventos tratados/ignorados; 400 para assinatura inválida; erros são logados.

**Arquivos:** `web/StripeWebhookController.java`, `config/StripeGateway.java`.

### US-5.5 — API de billing do tenant
**Como** TENANT_ADMIN, **quero** ver minha assinatura e faturas, **para** acompanhar minha
cobrança.

**Critérios de aceite:**
- `GET /api/billing/subscription` e `GET /api/billing/invoices` escopados pelo `tenantId`
  autenticado.

**Arquivos:** `web/BillingController.java`.

### US-5.6 — Job diário de cobrança
**Como** plataforma, **quero** bloquear inadimplentes automaticamente, **para** suspender
acesso após o período de carência.

**Critérios de aceite:**
- `DailyBillingJob` (`@Scheduled` 00:00) chama `blockOverdueTenants(GRACE_DAYS=3)`.
- Agendamento habilitado na aplicação (`@EnableScheduling`).

**Arquivos:** `config/DailyBillingJob.java`, `BiniTechPdvApplication.java`.

---

## EPIC 6 — Frontend Multi-Tenant & Páginas Públicas

### US-6.1 — Auth service ciente de tenant
**Como** app Angular, **quero** armazenar/enviar o `tenantId`, **para** suportar login
multi-tenant e papéis de super admin.

**Critérios de aceite:**
- `AuthService.login(username, password, tenantId?)`; persiste/limpa `tenantId` no
  localStorage; `isSuperAdmin()`, `getTenantId()`.
- Modelos `LoginRequest`/`AuthResponse` com `tenantId?`.

**Arquivos:** `auth/services/auth.service.ts`, `shared/models/api.models.ts`.

### US-6.2 — Roteamento e shell de navegação
**Como** usuário, **quero** rotas públicas e área administrativa, **para** navegar conforme
meu papel.

**Critérios de aceite:**
- Rotas: `''` → landing, `/signup`, `/signup/success`, `/sobre-nos`, `/termos`,
  `/privacidade`, `/admin` (protegida por `superAdminGuard`).
- `AppComponent`: `isPublicRoute()` esconde header em páginas públicas; item de menu "Admin"
  visível apenas para super admin.

**Arquivos:** `app.routes.ts`, `app.component.ts`,
`auth/guards/super-admin.guard.ts`.

### US-6.3 — Tela de signup de tenant
**Como** visitante, **quero** um formulário de cadastro, **para** criar minha conta e
escolher um plano.

**Critérios de aceite:**
- `SignupComponent` (reactive form: name, plano, billingEmail) chama
  `TenantPublicService.createTenant`; redireciona para `/signup/success`.
- `SignupSuccessComponent` informa que a conta aguarda aprovação.

**Arquivos:** `signup.component.ts`, `signup-success.component.ts`,
`tenant-public.service.ts`.

### US-6.4 — Dashboard do super admin
**Como** super admin, **quero** gerenciar tenants, **para** aprovar/bloquear contas e
acompanhar MRR.

**Critérios de aceite:**
- `AdminDashboardComponent` lista tenants, aprova/bloqueia (`AdminService`:
  `getTenants`/`approveTenant`/`blockTenant`), exibe rótulos de plano/status e MRR estimado.

**Arquivos:** `admin-dashboard.component.ts`, `admin.service.ts`.

### US-6.5 — Login redesenhado e páginas de marketing
**Como** visitante, **quero** páginas institucionais e um login moderno, **para** conhecer o
produto e acessá-lo.

**Critérios de aceite:**
- `LoginComponent` redesenhado (link para signup; suporte a tenant via rota/query quando
  aplicável).
- `LandingComponent`, `SobreNosComponent`, `TermosComponent`, `PrivacidadeComponent`.

**Arquivos:** `auth/components/login/login.component.ts`, `landing.component.ts`,
`sobre-nos.component.ts`, `termos.component.ts`, `privacidade.component.ts`.

---

## EPIC 7 — Infraestrutura & Configuração

### US-7.1 — Dependências e serviços de infra
**Como** dev, **quero** RabbitMQ e bibliotecas de e-mail/AMQP, **para** rodar o stack
completo localmente.

**Critérios de aceite:**
- `pom.xml`: `spring-boot-starter-mail`, `spring-boot-starter-amqp`.
- `docker-compose.yml`: serviço `rabbitmq` (3-management, portas 5672/15672).

**Arquivos:** `pom.xml`, `docker-compose.yml`.

### US-7.2 — Configuração da aplicação
**Como** dev/ops, **quero** parametrizar e-mail, RabbitMQ, Stripe e URL do frontend,
**para** configurar por ambiente.

**Critérios de aceite:**
- `application.yaml`: blocos `spring.mail`, `spring.rabbitmq`, `stripe` (`secret-key`,
  `webhook-secret`, `price-starter`/`pro`/`enterprise`), `app.frontend-url` (defaults/env vars).
- `GlobalExceptionHandler` cobre novas exceções de negócio.

**Arquivos:** `src/main/resources/application.yaml`,
`web/GlobalExceptionHandler.java`, `swagger.yaml`.

---

## Notas de Implementação / Ordem Sugerida

1. **EPIC 1** (fundação): roles, `tenantId` nas entidades, Tenant, JWT, filtro, segurança.
2. **EPIC 2** (auth multi-tenant) — depende do EPIC 1.
3. **EPIC 4** (e-mail) — pré-requisito do provisionamento do EPIC 3.
4. **EPIC 3** (onboarding) — depende de 1, 2, 4.
5. **EPIC 5** (billing/Stripe) — depende de 1 e 3.
6. **EPIC 6** (frontend) — em paralelo, integra ao final.
7. **EPIC 7** (infra) — habilitar conforme as épicas que dependem dela.

### Pontos de atenção (dívidas/melhorias a revisar ao refazer)
- (Resolvido na migração p/ Stripe) a busca de assinatura por id usa query direta
  `findByStripeSubscriptionId`/`findByStripeCustomerId` em vez de varrer todos os status.
- (Resolvido na migração p/ Stripe) os price ids foram externalizados em `StripeProperties`/env.
- `build_and_create.bat` era um utilitário local de scaffolding — **não** versionar.
- Confirmar índices Mongo para os campos `tenantId` (performance do isolamento).
- Revisar testes: portas/assinaturas mudaram (login/register/generateAccessToken etc.).
