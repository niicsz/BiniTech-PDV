# Arquitetura — BiniTech PDV (C4 Model)

Diagramas no padrão [C4 Model](https://c4model.com/) em Mermaid. Renderizam direto no GitHub e no VS Code (com a extensão Mermaid).

## C1 — Diagrama de Contexto

O BiniTech PDV é um SaaS multi-tenant de ponto de venda. Cada loja (tenant) assina um plano e seus usuários operam vendas, produtos e relatórios.

```mermaid
C4Context
    title C1 — Contexto do Sistema: BiniTech PDV

    Person(visitante, "Visitante", "Conhece o produto na landing page e cadastra uma nova loja (signup)")
    Person(lojista, "Lojista (Admin do tenant)", "Gerencia produtos, usuários, relatórios e a assinatura da loja")
    Person(operador, "Operador de caixa", "Registra vendas na tela de PDV e consulta fiados (devedores)")
    Person(superadmin, "Super Admin", "Administra a plataforma: tenants, planos e cobrança")

    System(pdv, "BiniTech PDV", "SaaS multi-tenant de ponto de venda: vendas, produtos, fiado, relatórios e billing por assinatura")

    System_Ext(stripe, "Stripe", "Checkout Sessions, Customer Portal e webhooks de assinatura/fatura")
    System_Ext(smtp, "Servidor SMTP", "Envio de e-mails transacionais (reset de senha, notificações)")

    Rel(visitante, pdv, "Acessa landing e faz signup", "HTTPS")
    Rel(lojista, pdv, "Gerencia a loja e a assinatura", "HTTPS")
    Rel(operador, pdv, "Registra vendas", "HTTPS")
    Rel(superadmin, pdv, "Administra a plataforma", "HTTPS")

    Rel(pdv, stripe, "Cria checkout/portal e consulta assinaturas", "HTTPS/REST")
    Rel(stripe, pdv, "Notifica eventos de pagamento", "Webhook HTTPS")
    Rel(pdv, smtp, "Envia e-mails", "SMTP/TLS")
```

## C2 — Diagrama de Contêineres

A SPA Angular é compilada no build do Docker e embutida como recurso estático no jar do Spring Boot — em produção é um único contêiner que serve frontend e API.

```mermaid
C4Container
    title C2 — Contêineres: BiniTech PDV

    Person(usuario, "Usuário", "Visitante, lojista, operador ou super admin")

    System_Boundary(pdv, "BiniTech PDV") {
        Container(spa, "SPA Angular", "Angular, TypeScript", "Telas de PDV, produtos, vendas, devedores, billing, admin e auth. Servida como estático pelo backend")
        Container(api, "API Backend", "Spring Boot 3, Java 21", "API REST com arquitetura hexagonal. JWT + refresh token, multi-tenant, jobs de billing")
        ContainerDb(mongo, "MongoDB", "MongoDB 7", "Tenants, usuários, produtos, vendas, assinaturas, faturas, tokens")
        ContainerDb(redis, "Redis", "Redis", "Cache de leitura e blacklist de access tokens (logout)")
        Container(rabbit, "RabbitMQ", "RabbitMQ 3", "Fila de eventos de e-mail (envio assíncrono)")
    }

    System_Ext(stripe, "Stripe", "Pagamentos e assinaturas")
    System_Ext(smtp, "Servidor SMTP", "Entrega de e-mails")

    Rel(usuario, spa, "Usa", "HTTPS")
    Rel(spa, api, "Chama", "JSON/HTTPS, Bearer JWT")
    Rel(api, mongo, "Lê e grava", "Spring Data MongoDB")
    Rel(api, redis, "Cacheia e consulta blacklist", "Spring Data Redis")
    Rel(api, rabbit, "Publica eventos de e-mail", "AMQP")
    Rel(rabbit, api, "Entrega eventos ao consumer", "AMQP")
    Rel(api, stripe, "Checkout, Customer Portal", "HTTPS/REST")
    Rel(stripe, api, "Webhooks de assinatura/fatura", "HTTPS")
    Rel(api, smtp, "Envia e-mails", "SMTP/TLS")
```

## C3 — Diagrama de Componentes (API Backend)

O backend segue arquitetura hexagonal (ports & adapters): controllers (adapters inbound) → use cases (application) → ports outbound → adapters outbound (persistência, mensageria, Stripe, e-mail).

```mermaid
C4Component
    title C3 — Componentes: API Backend (Spring Boot)

    Container(spa, "SPA Angular", "Angular", "Frontend")
    ContainerDb(mongo, "MongoDB", "MongoDB 7", "Persistência")
    ContainerDb(redis, "Redis", "Redis", "Cache + blacklist")
    Container(rabbit, "RabbitMQ", "RabbitMQ", "Fila de e-mails")
    System_Ext(stripe, "Stripe", "Pagamentos")
    System_Ext(smtp, "SMTP", "E-mail")

    Container_Boundary(api, "API Backend") {
        Component(security, "Filtros de Segurança", "JwtAuthenticationFilter, TenantValidationFilter", "Valida JWT (consultando blacklist no Redis) e o tenant ativo em cada requisição")
        Component(controllers, "Controllers REST", "Auth, Product, Sale, Tenant, Billing Controllers", "Adapters inbound web; mapeiam DTOs via WebMapper")
        Component(webhook, "StripeWebhookController", "Spring MVC", "Recebe e valida webhooks do Stripe (assinatura/fatura)")

        Component(usecases, "Use Cases", "Auth, Product, Sale, Tenant, Billing, PasswordReset UseCaseImpl", "Regras de negócio; implementam os ports inbound (XxxUseCasePort)")
        Component(domain, "Domínio", "Tenant, User, Product, Sale, Subscription, Invoice", "Entidades e exceções de negócio, sem dependência de framework")

        Component(repoAdapters, "Repository Adapters", "XxxRepositoryAdapter + Spring Data + PersistenceMapper", "Implementam os ports outbound de persistência (XxxRepositoryPort)")
        Component(emailPublisher, "RabbitMQEmailPublisher", "Spring AMQP", "Implementa EmailServicePort publicando eventos na fila")
        Component(emailConsumer, "EmailEventConsumer", "@RabbitListener", "Consome a fila e envia via JavaMailEmailServiceAdapter")
        Component(stripeGateway, "StripeGateway", "stripe-java", "Cria Checkout Sessions e sessões do Customer Portal")
        Component(billingJob, "DailyBillingJob", "@Scheduled", "Rotina diária de cobrança/expiração de assinaturas")
    }

    Rel(spa, security, "HTTPS + JWT")
    Rel(security, controllers, "Encaminha requisição autenticada")
    Rel(security, redis, "Consulta blacklist de tokens")
    Rel(controllers, usecases, "Invoca via ports inbound")
    Rel(webhook, usecases, "Aciona BillingUseCase")
    Rel(stripe, webhook, "Eventos", "HTTPS")

    Rel(usecases, domain, "Usa")
    Rel(usecases, repoAdapters, "Via ports outbound")
    Rel(usecases, emailPublisher, "Via EmailServicePort")
    Rel(usecases, stripeGateway, "Checkout / Portal")
    Rel(billingJob, usecases, "Executa rotina de billing")

    Rel(repoAdapters, mongo, "Spring Data MongoDB")
    Rel(emailPublisher, rabbit, "Publica", "AMQP")
    Rel(rabbit, emailConsumer, "Entrega", "AMQP")
    Rel(emailConsumer, smtp, "Envia e-mail", "SMTP/TLS")
    Rel(stripeGateway, stripe, "API REST", "HTTPS")
```

## Notas

- **Multi-tenant**: o isolamento é lógico — todos os documentos carregam `tenantId` e o `TenantValidationFilter` garante o escopo por requisição.
- **Deploy**: o `Dockerfile` multi-stage compila a SPA (Node), embute o `dist/` em `src/main/resources/static/` e empacota tudo num único jar (Temurin 21). Healthcheck via `/actuator/health`.
- **E-mail resiliente**: o envio é desacoplado por fila (RabbitMQ); existe um `NoOpEmailServiceAdapter` para ambientes sem SMTP configurado.
