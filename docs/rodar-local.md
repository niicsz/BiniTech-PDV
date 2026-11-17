# Rodar o PDV localmente com Docker

O arquivo `docker-compose.local.yml` sobe tudo o que o backend precisa: MongoDB, Redis, RabbitMQ, o serviço de autenticação (BiniTech-Auth) com o banco dele, a criação do administrador da plataforma e o próprio PDV.

## Passo a passo

```sh
cp .env.local.example .env.local
docker compose -f docker-compose.local.yml --env-file .env.local up --build
```

Antes de subir, troque no `.env.local`:

- `AUTH_SERVICE_KEY`, `JWT_SECRET`, `SECURITY_PEPPER` e `PRODUCT_IMPORT_SERVICE_KEY`: `openssl rand -hex 32` gera um valor adequado para cada um.
- `RABBITMQ_PASSWORD` e `ADMIN_PASSWORD` (a senha do administrador precisa de pelo menos 6 caracteres).

Não use `$` nos valores: o Docker Compose interpreta `$texto` como variável e troca por vazio.

## O que fica disponível

| Serviço | Endereço |
| --- | --- |
| PDV (API) | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Auth | http://localhost:8081 |
| Painel do RabbitMQ | http://localhost:15672 |
| MongoDB do PDV | `localhost:27017` |
| Redis | `localhost:6379` |

Todas as portas ficam presas em `127.0.0.1`.

O login do administrador é o `ADMIN_USERNAME` com a `ADMIN_PASSWORD` do `.env.local`. O serviço `seed` cria esse usuário no PDV e a identidade dele no Auth; ele roda uma vez a cada subida e não altera um administrador que já existe.

## Frontend

O frontend fica em outro repositório ([BiniTech-PDV-frontend](https://github.com/niicsz/BiniTech-PDV-frontend)). Com o backend no ar, rode `npm install` e `npm start` lá; ele abre em http://localhost:4200 e encaminha `/api` para a porta 8080.

## O que não está incluído

- Importação de produtos (BiniTech-Product-Import) e emissão fiscal (BiniTech-Fiscal): são serviços de outros repositórios.
- E-mails e cobrança: sem `RESEND_API_KEY` e `ASAAS_API_KEY`, o PDV sobe, mas não envia e-mails nem cria assinaturas.
- Upload de imagens: `IMAGES_STORAGE=none` deixa o recurso desligado.

## Parar e limpar

```sh
docker compose -f docker-compose.local.yml --env-file .env.local down
```

Acrescente `-v` para apagar também os dados dos bancos.
