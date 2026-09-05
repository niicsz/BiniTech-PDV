# BiniTech Auth

Serviço REST de autenticação independente, em Java 21/Spring Boot. Não depende do JAR do PDV, de planos, de vendas, do Stripe ou do RabbitMQ. Pode ser compilado e publicado separadamente.

## Responsabilidades

O serviço autentica credenciais, emite JWTs, renova tokens, encerra sessões e consulta a identidade da sessão. O backend do PDV encaminha suas rotas de login, refresh e logout ao serviço, mantendo URLs e campos de resposta existentes.

Cadastro, alteração/recuperação de senha, gestão de usuários e regras de lojas/planos continuam no PDV. Nesta etapa, os processos compartilham as coleções `users` e `refresh_tokens` do MongoDB e o Redis de revogação. Isso mantém os usuários, hashes Argon2 + pepper e a revogação por troca de senha compatíveis. A separação é de execução e código; o armazenamento de identidades ainda é compartilhado. O serviço consegue autenticar usuários existentes mesmo com o PDV desligado.

## Configuração

| Variável | Uso |
| --- | --- |
| `AUTH_MONGODB_URI` | Conexão ao MongoDB que contém as identidades |
| `AUTH_MONGODB_DATABASE` | Banco das identidades; padrão `binitech_pdv` |
| `AUTH_REDIS_URL` | Mesmo Redis e índice de banco usados pelo PDV |
| `JWT_SECRET` | Mesma chave do PDV, com pelo menos 32 bytes |
| `SECURITY_PEPPER` | Mesmo pepper usado para criar as senhas existentes |
| `JWT_ACCESS_EXPIRATION` | Validade do access token em ms; padrão `900000` |
| `JWT_REFRESH_EXPIRATION` | Validade do refresh token em ms; padrão `86400000` |
| `AUTH_CORS_ALLOWED_ORIGINS` | Origens dos frontends separadas por vírgulas |
| `PORT` | Porta do serviço; padrão `8081` |

O serviço lê variáveis do processo; não carrega automaticamente o `.env` do PDV. O Compose abaixo repassa as variáveis necessárias. Não altere a chave, o pepper ou o banco durante a extração: isso impediria a compatibilidade com usuários/sessões existentes.

No backend PDV, configure `AUTH_SERVICE_URL` com a URL do serviço (padrão `http://localhost:8081`). `AUTH_CONNECT_TIMEOUT` e `AUTH_READ_TIMEOUT` têm padrões `2s` e `5s`. Falhas do serviço retornam HTTP 503 com código `AUTH_UNAVAILABLE`; não há autenticação local de contingência.

## Compilar e executar

Na raiz do repositório, com as variáveis exportadas:

```powershell
.\mvnw.cmd -f auth-service/pom.xml verify
java -jar auth-service/target/auth-service-1.0.0.jar
```

Em Linux/macOS, use `./mvnw`. Para desenvolvimento com MongoDB e Redis locais:

```sh
docker compose -f docker-compose.yml -f docker-compose.auth.yml up -d mongodb redis auth-service
```

Nesse cenário, o PDV executado no host deve usar `MONGODB_URI=mongodb://localhost:27017`, `REDIS_URL=redis://localhost:6379` e `AUTH_SERVICE_URL=http://localhost:8081`, além da mesma chave e pepper. As contas são criadas pelos fluxos existentes do PDV; o serviço não cria um administrador padrão. O Compose local não copia dados de bancos externos.

A imagem de autenticação usa o contexto da raiz:

```sh
docker build -f auth-service/Dockerfile -t binitech-auth .
```

## Contrato HTTP

| Método e caminho | Entrada | Resposta |
| --- | --- | --- |
| `POST /api/auth/login` | JSON `username`, `password`, `tenantId` opcional | 200 com tokens e identidade |
| `POST /api/auth/refresh` | JSON `refreshToken` | 200 com novos tokens |
| `GET /api/auth/session` | `Authorization: Bearer <accessToken>` | 200 com `userId`, `username`, `role`, `tenantId` |
| `POST /api/auth/logout` | `Authorization: Bearer <accessToken>` | 204 |
| `GET /actuator/health` | Nenhuma | Saúde do serviço e dependências |

Login e refresh retornam o formato já utilizado pelo PDV:

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<token opaco>",
  "username": "operador",
  "role": "OPERATOR",
  "tenantId": "<id da loja>"
}
```

Sem `tenantId`, o login só autentica quando exatamente uma conta corresponde ao username/senha. Credenciais incorretas, usuários inativos e sessões expiradas/revogadas retornam 401 no serviço. O proxy do PDV traduz esse erro para seu contrato anterior de 400/`BUSINESS_ERROR`. JSON/campos inválidos retornam 400. Respostas de autenticação não devem ser armazenadas em cache.

Um login não invalida refresh tokens de outros logins. A renovação consome o token atomicamente e gera outro; duas requisições simultâneas com o mesmo refresh token não podem ter sucesso. Access tokens têm identificador único. O logout invalida o access token apresentado e remove os refresh tokens do usuário/tenant, preservando o alcance anterior do logout do PDV; outros access tokens já emitidos continuam válidos até expirar. Troca/redefinição de senha revoga todas as sessões por versão no Redis. Refresh tokens antigos sem versão são aceitos como versão zero; contas cuja versão já foi incrementada precisam fazer login novamente.

Na inicialização, o serviço garante os índices de unicidade do refresh token e expiração (`expiryDate`, TTL zero) na coleção `refresh_tokens`; o MongoDB remove registros expirados em segundo plano. O usuário de conexão precisa de permissão para criar esses índices. A expiração também é verificada na aplicação, sem depender do intervalo de limpeza do MongoDB.

## Reutilizar em outra aplicação

Adicione a origem do frontend a `AUTH_CORS_ALLOWED_ORIGINS`. O frontend pode chamar `/api/auth/login` diretamente e enviar o access token ao seu próprio backend. Esse backend consulta `/api/auth/session` para validar a assinatura, a expiração, a revogação e o estado atual do usuário. Nenhuma chave JWT ou credencial de banco precisa ser distribuída às aplicações consumidoras.

Exemplo em um backend JavaScript (URL fixa na configuração do servidor):

```js
const response = await fetch(`${process.env.AUTH_SERVICE_URL}/api/auth/session`, {
  headers: { Authorization: `Bearer ${accessToken}` },
  signal: AbortSignal.timeout(5000),
  redirect: 'error',
});
if (response.status === 401) throw new Error('Sessão inválida');
if (!response.ok) throw new Error('Autenticação indisponível');
const identity = await response.json();
// A aplicação aplica aqui suas permissões para identity.userId.
```

Use HTTPS fora do ambiente local e valide a sessão no backend antes de autorizar operações. `role` e `tenantId` são metadados atuais da identidade do PDV; cada aplicação deve definir suas permissões e quais usuários aceita. CORS não substitui essa autorização. Este contrato REST não implementa OAuth2/OIDC, consentimento ou permissões por aplicação.

## Implantação e compatibilidade

1. Publique primeiro o serviço de autenticação com acesso ao MongoDB/Redis atuais e os mesmos segredos.
2. Verifique `/actuator/health` e o login de uma conta existente.
3. Configure `AUTH_SERVICE_URL` e publique o backend PDV atualizado. O frontend atual continua usando as mesmas rotas do backend.
4. Configure as demais aplicações para consumir o serviço diretamente.

O PDV continua validando seus JWTs localmente e consultando usuários/revogações atuais. O serviço não precisa de URL de retorno ou chamada ao backend PDV. O cadastro do PDV salva a conta antes de solicitar o login ao serviço; se essa chamada falhar, a conta permanece criada e pode fazer login quando o serviço se recuperar.

### Railway

No projeto `steadfast-growth`, ambiente `production`, o serviço `BiniTech-Auth` usa `RAILWAY_DOCKERFILE_PATH=auth-service/Dockerfile` e `PORT=8081`, com contexto de build na raiz do repositório. As variáveis de MongoDB, Redis, JWT, pepper e CORS referenciam os valores correspondentes de `BiniTech-PDV` com a sintaxe `${{BiniTech-PDV.NOME_DA_VARIAVEL}}`. Não armazene seus valores no Git.

O backend usa `AUTH_SERVICE_URL=http://${{BiniTech-Auth.RAILWAY_PRIVATE_DOMAIN}}:8081`. O arquivo `railway.json` configura a verificação de saúde em `/actuator/health` antes de trocar os deployments.

Para publicar pela CLI a partir da raiz, sempre publique e valide primeiro a autenticação:

```sh
railway up --project 0fb63aa2-ccbd-4dcb-a451-6324960b0b22 --environment production --service BiniTech-Auth --detach
railway up --project 0fb63aa2-ccbd-4dcb-a451-6324960b0b22 --environment production --service BiniTech-PDV --detach
```

Os testes do serviço cobrem login, ambiguidade entre tenants, CORS, validação de sessão, usuários inativos, revogação, compatibilidade legada e rotação concorrente. Execute também `./mvnw verify` na raiz para validar o cliente e as regras do PDV.
