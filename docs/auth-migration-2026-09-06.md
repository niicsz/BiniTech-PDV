# Migração de autenticação — 06/09/2026

## Resultado em produção

O Auth foi publicado no projeto Railway `steadfast-growth`, com repositório
[BiniTech-Auth](https://github.com/niicsz/BiniTech-Auth) independente e arquitetura
hexagonal (domínio, casos de uso, portas e adaptadores). O PDV consome sua API;
não acessa diretamente o banco de identidades.

| Responsabilidade | Serviço / armazenamento |
| --- | --- |
| Identidades, hashes, recuperação, sessões e revogações | Auth / MongoDB dedicado, `binitech_auth` |
| Vínculos de usuários, perfis, status no PDV e dados de negócio | PDV / banco original `binitech_pdv` |
| Backup da migração | MongoDB dedicado / `binitech_auth_migration_backup_20260906`, acesso administrativo |

Foram copiadas e verificadas 10 identidades, preservando IDs e hashes existentes.
Os 10 vínculos do PDV foram mantidos, inclusive seus perfis e os 3 vínculos inativos.
O status de uma identidade no Auth não substitui a autorização local de cada aplicação.

Após validar o novo fluxo, foram removidos do banco original 10 campos de senha,
4 tokens de renovação e 1 token de recuperação. Auditoria posterior confirmou
10 vínculos, zero campos `password` e zero tokens nessas coleções legadas.
As demais coleções de negócio não foram migradas nem removidas.

## Isolamento e segurança

- Auth utiliza usuário MongoDB com `readWrite` somente em `binitech_auth`.
  Tentativas de acessar o banco do PDV e o backup com essa credencial foram negadas.
- Assinatura JWT independente; pepper original preservado para manter as senhas.
- API interna exige credencial de serviço; chamadas anônimas e com token de usuário foram rejeitadas.
- PDV sem `JWT_SECRET`, `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION`,
  `SECURITY_PEPPER`, `SECURITY_DUMMY_PASSWORD_HASH` e `ADMIN_PASSWORD`.
  A ausência foi confirmada também no container após republicação.
- Redis legado retirado do Auth; Redis do PDV permanece para suas funções de negócio.
- Proxy TCP público temporário do MongoDB excluído, chave SSH temporária revogada
  e seus arquivos locais removidos. Comunicação de produção com MongoDB usa rede privada.

Sessões anteriores e links antigos de recuperação foram invalidados.
Os usuários precisam entrar novamente, mantendo suas senhas existentes.

## Publicações e validação

| Serviço | Código publicado | Deployment final |
| --- | --- | --- |
| Auth | `7bf1d3d0be03e3f42a692d5b0bf5820cbef6c1a1` / `main` | `fa253a49-68a9-4535-b6e9-1a0c7bd8e4ab` |
| PDV | `e141ead598fac1ffcc8e3d6444b4d4dcc2463acc` / `feat/shared-authentication` | `bd10c7c7-2db1-4940-8c58-ed5c3f75f222` |

PDV publicado via Railway CLI a partir de exportação Git do commit indicado,
sem arquivos locais não rastreados. Ambos os deployments concluíram com sucesso.

- PDV: `mvn clean verify`, 159 testes aprovados; build/testes e Docker também aprovados no CI.
- Auth: 41 testes unitários/HTTP/arquitetura e 3 testes de adaptadores com MongoDB real.
  [CI aprovado](https://github.com/niicsz/BiniTech-Auth/actions/runs/34063176383).
- Testes em produção repetidos após limpar os dados e republicar as variáveis:
  saúde dos serviços, login de conta migrada pelo PDV, autorização SUPER_ADMIN,
  rejeição de identidade sem vínculo no PDV, provisionamento idempotente,
  troca de senha, revogação de sessões, renovação e rejeição de reutilização,
  recuperação com uso único, logout e rejeição de tokens antigos.
- A identidade sintética usada nos testes e seus refresh tokens foram removidos.

## Backup e operação

O backup preserva os documentos originais de usuários e tokens para recuperação
administrativa. O snapshot local de configuração foi protegido com Windows DPAPI
em `.auth-work/migration/runtime-secrets.dpapi`, fora do Git. Não publicar esse
arquivo nem incluir segredos em chamados ou logs.

Revisar a retenção do backup após 30 dias; nenhuma exclusão automática foi criada.
Uma reversão exige planejamento e reconciliação com as alterações posteriores no
Auth: não republicar simplesmente o backend antigo nem restaurar hashes antigos
sobre senhas alteradas após a migração. O procedimento e os scripts estão em
[database-isolation.md](https://github.com/niicsz/BiniTech-Auth/blob/main/docs/database-isolation.md).
Os acessos temporários usados pelos scripts estão encerrados; uma nova intervenção
administrativa exige acesso autorizado e endpoint atualizado.

## Pendências explícitas

- [PR 80 do PDV](https://github.com/niicsz/BiniTech-PDV/pull/80) permanece aberta;
  as alterações estão no GitHub e em produção, mas ainda não foram integradas em `main`.
  Antes de publicar novamente a partir de `main`, integrar a migração para não
  recolocar em produção código dependente das credenciais removidas.
- [OWASP Dependency Check do PDV](https://github.com/niicsz/BiniTech-PDV/actions/runs/34063180765/job/101567254701)
  falhou por dependências sinalizadas com CVSS >= 8, incluindo Spring, Tomcat,
  Netty e Jackson. Esse gate não foi desativado nem suprimido; sua triagem e
  atualização permanecem pendentes. Não confundir testes funcionais aprovados
  com aprovação integral da segurança das dependências.
- O namespace configurado é `pdv`. Novas aplicações devem definir suas políticas
  de integração e autorização; o serviço não é um provedor OAuth/OIDC completo.
