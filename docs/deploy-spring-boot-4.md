# Deploy da migração para Spring Boot 4 e Java 25

Plano de subida e de volta dos 5 serviços migrados (BTPDV-174). Os PRs já estão no `main` de cada repositório; o que falta é o deploy em produção, um serviço por vez, e a conferência depois de cada um.

## Ordem

Do serviço com menos dependentes para o que todos dependem. Só passe para o próximo quando o anterior estiver estável (healthcheck `UP`, sem erro novo no log e sem alerta no app).

| # | Serviço | Repositório | `main` migrado | `main` antes da migração | Porta |
|---|---|---|---|---|---|
| 1 | Product-Import | `BiniTech-Product-Import` | `f8b3910` (PR #6) | `1c0e77e` | 8082 |
| 2 | Alerts | `BiniTech-Alerts` | `154106c` (PR #1) | `13fe6bb` | 8084 |
| 3 | Fiscal | `BiniTech-Fiscal` | `c554c25` (PR #7) | `87b7362` | 8083 |
| 4 | Auth | `BiniTech-Auth` | `6fb31dc` (PRs #11 e #12) | `e1fb718` | 8081 |
| 5 | PDV | `BiniTech-PDV` | `f455a92` (PR #114) | `18f238e` | 8080 |

Alerts vai cedo de propósito: com ele no ar, os serviços seguintes já sobem monitorados.

## Antes de começar

- Confirme que os 8 checks do último commit do `main` estão verdes em cada repositório (Backend Build & Test, Docker Build, Trivy, SonarCloud, CodeQL, Analyze actions e java-kotlin, Snyk).
- Tenha o app de alarme aberto e o log do serviço no Railway visível.
- Avise a loja piloto de que haverá alguns minutos de instabilidade por serviço.

## Para cada serviço

1. No Railway, dispare o deploy do `main` (ou `railway up --service <serviço>` a partir do checkout do commit migrado).
2. Espere o healthcheck do Railway ficar `UP` (`/actuator/health`, timeout de 300 s).
3. Anote o **tempo de subida** (do início do deploy ao primeiro `UP`) e a **memória** depois de 5 minutos estável. Os valores de antes da migração ficam como referência para a subtarefa BTPDV-181; se a memória passar do limite do plano ou a subida demorar mais que o healthcheck, veja [Memória e tempo de subida](#memória-e-tempo-de-subida).
4. Rode a conferência do serviço na tabela abaixo.
5. Se algo falhar e não for resolvido em poucos minutos, faça a [volta](#volta-rollback) daquele serviço antes de seguir.

| Serviço | Conferência |
|---|---|
| Product-Import | Importar uma planilha pequena pelo PDV antigo ainda em produção e conferir os produtos |
| Alerts | Abrir o app Android, confirmar que recebe o SSE e provocar um alerta de teste (ex.: reiniciar o Fiscal depois dele) |
| Fiscal | `GET /actuator/health` `UP`; emitir uma NFC-e em **homologação** da SEFAZ com a loja de teste (IE e certificado A1) |
| Auth | Login com usuário com 2FA (TOTP), troca de senha e logout; o PDV antigo ainda precisa autenticar normalmente |
| PDV | Login pelo frontend, uma venda em dinheiro com baixa de estoque, abertura e fechamento de caixa, uma cobrança Pix em sandbox se o Pix estiver ligado, consulta de delivery |

## Volta (rollback)

Cada serviço volta sozinho, sem precisar desfazer os outros. Dois caminhos:

**Pelo Railway (mais rápido).** Em *Deployments* do serviço, escolha o deploy anterior à migração e use *Redeploy*. Isso recoloca a imagem antiga sem mexer no Git.

**Pelo Git.** Reverta o commit da migração e deixe o CI publicar:

```bash
git checkout main && git pull
git revert <commit migrado>          # ex.: git revert f455a92 no PDV
git push origin main
```

No Auth são dois commits (`6fb31dc` e `cedc135`), revertidos nessa ordem. Em qualquer repositório, `git checkout <main antes da migração>` seguido de `railway up --service <serviço>` publica o código antigo direto do seu computador se o CI estiver indisponível.

### Por que a volta é segura para os dados

- **MongoDB:** `spring.data.mongodb.representation.big-decimal=string` mantém o `BigDecimal` gravado como texto, igual à versão 3.5. Documentos criados pela versão nova são lidos pela antiga.
- **Redis (PDV):** o cache usa `CACHE_VERSION v4`, chaves separadas das da versão anterior. Voltar só provoca cache frio.
- **RabbitMQ:** os consumidores novos e antigos leem data numérica e ISO-8601; mensagens que ficarem na fila entre deploy e volta são processadas pelos dois lados.
- **Auth/PDV:** tokens e sessões não mudaram de formato; ninguém é deslogado na volta.

### O que não volta sozinho

- Produtos importados, vendas e notas emitidas pela versão nova continuam no banco; nada precisa ser apagado.
- NFC-e autorizada em produção pela versão nova continua válida na SEFAZ.

## Memória e tempo de subida

O Java 25 com o JRE do Temurin usa por padrão 25% da memória do container como heap máximo. Se o Railway mostrar memória perto do limite do plano ou o serviço reiniciar por falta de memória, a primeira medida é fixar o heap no `ENTRYPOINT` do `Dockerfile` do serviço, por exemplo `-XX:MaxRAMPercentage=60.0`, e abrir um PR separado só com isso. Se a subida passar dos 300 s do healthcheck, aumente `healthcheckTimeout` no `railway.json` antes de mexer no código.

## Depois dos 5 serviços

- Mover as subtarefas BTPDV-181 e BTPDV-182 do kanban para Finalizado, com os valores de memória e tempo de subida anotados em 181.
- Apagar os secrets que o OWASP usava e que ficaram sem uso:

  ```bash
  gh secret delete NVD_API_KEY --repo niicsz/BiniTech-PDV
  gh secret delete OSSINDEX_USERNAME --repo niicsz/BiniTech-PDV
  gh secret delete OSSINDEX_TOKEN --repo niicsz/BiniTech-PDV
  gh secret delete NVD_API_KEY --repo niicsz/BiniTech-Product-Import
  gh secret delete OSSINDEX_USERNAME --repo niicsz/BiniTech-Product-Import
  gh secret delete OSSINDEX_TOKEN --repo niicsz/BiniTech-Product-Import
  ```

- Manter a loja piloto em observação por um dia útil completo antes de considerar a migração encerrada.
