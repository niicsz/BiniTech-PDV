# Importação e migração de produtos

A importação foi implementada como microserviço backend separado, product-import-service, com contrato OpenAPI, arquitetura hexagonal, deploy e MongoDB próprios. A SPA Angular existente apenas ganhou a rota Produtos → Importar produtos; não há microfrontend.

    SPA Angular
         | Bearer + CSV/Excel
    Product Import API ----> BiniTech Auth
         |                       introspecção
         +----> MongoDB exclusivo (jobs, linhas e GridFS)
         |
         +----> RabbitMQ ----> Worker ----> BiniTech PDV ----> MongoDB do PDV

## Limites de responsabilidade

- O importador é dono de arquivos, mapeamentos, pré-visualizações, progresso, erros e auditoria do job.
- O PDV é o único dono de produtos e aplica suas regras atuais de usuário, role e tenant.
- O Auth continua sendo o único responsável por autenticação e sessão.
- O RabbitMQ existente é reutilizado; nenhum broker ou banco adicional foi introduzido além do Mongo exclusivo solicitado.
- O Redis do PDV continua responsável pelo cache de produtos; o adapter batch invalida os caches após gravação.

## Segurança

- Tenant, usuário e role vêm da sessão validada no Auth, nunca do request da SPA.
- O PDV revalida a identidade em seu banco e exige tenant ACTIVE.
- Endpoints internos aceitam somente X-Product-Import-Service-Key.
- Todo job, linha, arquivo GridFS e consulta usa tenantId.
- Upload limitado a 20 MiB, 50.000 linhas e 200 colunas, com proteção de ZIP bomb do Apache POI.
- Extensão e conteúdo são analisados; arquivos vazios, corrompidos, protegidos ou malformados são recusados.

## Índice de código de barras

Para garantir concorrência na criação, a definição uk_barcode_tenantId é única. Em bancos existentes, primeiro procure agrupamentos por tenantId e barcode cuja contagem seja maior que um. Somente depois de resolver cada resultado, crie o índice único em uma janela controlada. Não remova automaticamente produtos duplicados: isso exige decisão do lojista.

## Execução local

docker compose up product-import-mongodb rabbitmq product-import-service inicia o importador com um Mongo separado em localhost:27018. Auth e PDV devem estar nas portas 8081 e 8080 usando a mesma chave local indicada no compose.

Consulte também product-import-service/README.md e product-import-service/src/main/resources/openapi/swagger.yaml.
