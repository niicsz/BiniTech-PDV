# Guia de configuração do Stripe (PDV SaaS)

> O código backend + frontend já está pronto e testado. **Nada aqui é programação** — é
> configuração no Dashboard do Stripe e variáveis de ambiente. Siga na ordem.
> Ao final, faça o teste end-to-end (seção 7).

## Como funciona (resumo de 30s)

- A loja entra em `/billing` e clica em **Pagar** → backend cria um **Checkout Session** do
  Stripe (modo assinatura) e redireciona para a página hospedada do Stripe.
- Pagou → o Stripe dispara um **webhook** para `POST /webhooks/stripe` → o backend ativa o
  tenant (`ACTIVE`) e a assinatura.
- Renovação mensal, falha de pagamento e cancelamento também chegam por webhook.
- Loja ativa pode clicar em **Gerenciar assinatura** → abre o **Customer Portal** do Stripe
  (trocar cartão, cancelar, ver faturas).
- **Sem as chaves configuradas, o checkout fica desabilitado** e só funciona a ativação manual
  pelo painel do super admin (`/admin`).

---

## 1. Criar conta / entrar no Dashboard

1. Acesse https://dashboard.stripe.com.
2. Comece em **modo de teste** (toggle "Test mode" ligado, canto superior direito). Só passe
   para produção depois do teste end-to-end OK.
3. País da conta: **Brasil** (para cobrar em BRL e habilitar Pix/cartão).

## 2. Criar os 3 produtos/preços (Prices) recorrentes

Vá em **Product catalog → Add product**. Crie **3 produtos**, cada um com um preço
**recorrente mensal em BRL**:

| Produto    | Recorrência | Moeda | Valor (defina com o dono) |
|------------|-------------|-------|---------------------------|
| Starter    | Mensal      | BRL   | R$ ___                    |
| Pro        | Mensal      | BRL   | R$ ___                    |
| Enterprise | Mensal      | BRL   | R$ ___                    |

> Atenção: o preço tem que ser **Recurring / Monthly** (não "one time").

Depois de criar, abra cada preço e **copie o Price ID** (começa com `price_...`). Você vai
precisar dos 3:

- `price_...` do Starter → vira `STRIPE_PRICE_STARTER`
- `price_...` do Pro → vira `STRIPE_PRICE_PRO`
- `price_...` do Enterprise → vira `STRIPE_PRICE_ENTERPRISE`

> O plano **free (cortesia)** não tem preço no Stripe — é de propósito, não crie nada para ele.

## 3. Criar a chave de API (restricted key `rk_`)

Por segurança usamos uma **restricted key**, não a secret key padrão.

1. **Developers → API keys → Create restricted key**.
2. Dê um nome (ex.: `pdv-backend`).
3. Permissões mínimas (marque **Write** onde indicado):
   - Checkout Sessions → **Write**
   - Billing Portal Sessions → **Write**
   - Subscriptions → **Read**
   - Prices → **Read**
   - (Webhooks são verificados pelo *webhook secret*, não precisam de permissão na key.)
4. Crie e **copie a chave** (`rk_test_...` em teste / `rk_live_...` em produção).
   → vira `STRIPE_SECRET_KEY`.

> Guarde a chave num gerenciador de segredos. Ela só aparece uma vez.

## 4. Criar o endpoint de Webhook

1. **Developers → Webhooks → Add endpoint**.
2. **Endpoint URL:** `https://SEU_DOMINIO/webhooks/stripe`
   (em teste local, use o Stripe CLI — ver seção 6).
3. **Eventos a assinar** (exatamente estes 4):
   - `checkout.session.completed`
   - `invoice.paid`
   - `invoice.payment_failed`
   - `customer.subscription.deleted`
4. Crie o endpoint, abra ele e **copie o Signing secret** (`whsec_...`).
   → vira `STRIPE_WEBHOOK_SECRET`.

## 5. Habilitar o Customer Portal

1. **Settings → Billing → Customer portal**.
2. Ative o portal e marque o que a loja pode fazer (recomendado: atualizar forma de pagamento,
   ver histórico de faturas, cancelar assinatura).
3. Salve.

## 6. Setar as variáveis de ambiente

O backend lê estas 6 variáveis (ver `src/main/resources/application.yaml`, bloco `stripe`):

| Variável                  | Valor                                  |
|---------------------------|----------------------------------------|
| `STRIPE_SECRET_KEY`       | `rk_test_...` (ou `rk_live_...`)       |
| `STRIPE_WEBHOOK_SECRET`   | `whsec_...`                            |
| `STRIPE_PRICE_STARTER`    | `price_...` do Starter                 |
| `STRIPE_PRICE_PRO`        | `price_...` do Pro                     |
| `STRIPE_PRICE_ENTERPRISE` | `price_...` do Enterprise              |
| `APP_FRONTEND_URL`        | URL do frontend (ex.: `https://app.seudominio.com`) — usada para montar as URLs de sucesso/retorno do checkout |

> Em produção, coloque-as no ambiente do container/serviço (não commite as chaves no git).
> Onde já estão as outras envs do backend (ex.: `docker-compose.yml` / variáveis do host de
> deploy) é onde estas entram também.

## 7. Teste end-to-end (modo de teste)

Faça isto **antes** de ir para produção:

1. Suba o backend com as envs de **teste** (`rk_test_...`, `whsec_...`, prices de teste).
2. Encaminhe os webhooks localmente com o Stripe CLI:
   ```bash
   stripe login
   stripe listen --forward-to http://localhost:8080/webhooks/stripe
   ```
   (O `whsec_...` que o `stripe listen` imprime é o que deve estar em `STRIPE_WEBHOOK_SECRET`
   durante o teste local.)
3. No app, logue numa loja de teste com plano pago (não free), vá em `/billing` → **Pagar**.
4. No Checkout do Stripe use o **cartão de teste** `4242 4242 4242 4242`, validade futura,
   CVC qualquer.
5. Confirme:
   - voltou para `/billing?status=success`;
   - o tenant ficou **ACTIVE** (cheque no `/admin` ou no banco);
   - chegou `checkout.session.completed` (veja no terminal do `stripe listen` e nos logs do
     backend: `Webhook Stripe recebido: type=checkout.session.completed`).
6. Clique em **Gerenciar assinatura** → deve abrir o Customer Portal.
7. (Opcional) Dispare eventos extras para validar o resto:
   ```bash
   stripe trigger invoice.payment_failed
   stripe trigger customer.subscription.deleted
   ```
   `customer.subscription.deleted` deve levar o tenant a **CANCELLED** (e o acesso passa a
   retornar 402).

## 8. Ir para produção

1. Refaça os passos 2–5 com o **Test mode desligado** (cria prices/key/webhook de produção
   — são diferentes dos de teste).
2. Troque as 6 envs para os valores `live`.
3. Webhook URL de produção = `https://SEU_DOMINIO/webhooks/stripe` (precisa estar acessível
   pela internet e com HTTPS válido).
4. Faça uma compra real de baixo valor para validar e estorne se quiser.

---

## Checklist rápido

- [ ] 3 Prices recorrentes mensais em BRL criados (Starter/Pro/Enterprise)
- [ ] Restricted key `rk_` criada com as permissões da seção 3
- [ ] Webhook `/webhooks/stripe` criado assinando os 4 eventos
- [ ] Customer Portal habilitado
- [ ] 6 variáveis de ambiente setadas no backend
- [ ] Teste end-to-end com cartão `4242...` passou (tenant vira ACTIVE)
- [ ] Repetido para produção com chaves `live`

## Referências no código (para tirar dúvidas)

- Config das envs: `src/main/resources/application.yaml` (bloco `stripe`) e
  `config/StripeProperties.java`
- Criação de checkout/portal: `config/StripeGateway.java`
- Recebimento de webhooks: `adapters/inbound/web/StripeWebhookController.java`
  (URL `/webhooks/stripe`)
- Regras de ativação/cancelamento: `application/usecases/BillingUseCaseImpl.java`
