/*
 * Migração única: converte a loja legada (single-tenant, sem tenantId) em um Tenant de verdade.
 *
 * Contexto: antes do multi-tenant todos os usuários/produtos/vendas eram criados SEM tenantId.
 * Este script cria um Tenant, atribui o tenantId a todos esses documentos órfãos e (opcional)
 * promove um usuário a TENANT_ADMIN. Pensado para a loja do plano cortesia "Banca Oz Bini".
 *
 * PRESSUPOSTO IMPORTANTE: todos os documentos com tenantId nulo pertencem a UMA ÚNICA loja.
 * Se houver mais de uma loja misturada no "espaço nulo", NÃO rode isto sem segmentar antes.
 *
 * Como rodar (exemplos):
 *   mongosh "<SUA_MONGODB_URI>" scripts/migrate-legacy-tenant.js
 *   # ou dentro do mongosh:  load('scripts/migrate-legacy-tenant.js')
 *
 * Faça BACKUP antes (mongodump). Comece com DRY_RUN = true para conferir as contagens.
 */

// ====================== CONFIG (edite aqui) ======================
const CONFIG = {
  dbName: 'binitech_pdv', // mesmo de spring.data.mongodb.database
  dryRun: true, // true = só mostra o que faria; mude para false para aplicar

  tenant: {
    name: 'Banca Oz Bini',
    slug: 'banca-oz-bini', // será o identificador "Loja" no login
    planId: 'free', // plano cortesia: grátis, ilimitado, sem cobrança
    billingEmail: 'PREENCHA_O_EMAIL@exemplo.com', // usado p/ reset de senha; OBRIGATÓRIO
  },

  // Promove este usuário (match por username) a TENANT_ADMIN após a migração.
  // Deixe null para não promover ninguém.
  promoteUsernameToTenantAdmin: 'Banca Oz Bini',

  // Cria também um registro de assinatura ACTIVE (para a tela /billing exibir "Cortesia").
  createSubscription: true,

  // Remove refresh tokens órfãos para forçar re-login limpo (agora com o slug da loja).
  clearOrphanRefreshTokens: true,
};
// ================================================================

const db = db.getSiblingDB(CONFIG.dbName);
const now = new Date();
const ORPHAN = { $or: [{ tenantId: null }, { tenantId: { $exists: false } }] };

function log(msg) {
  print(`[migrate] ${msg}`);
}

// -------- Validações --------
if (!CONFIG.tenant.billingEmail || CONFIG.tenant.billingEmail.startsWith('PREENCHA')) {
  throw new Error('Defina CONFIG.tenant.billingEmail antes de rodar.');
}
if (db.tenants.findOne({ slug: CONFIG.tenant.slug })) {
  throw new Error(`Já existe um tenant com slug "${CONFIG.tenant.slug}". Abortando para não duplicar.`);
}

// -------- Diagnóstico (sempre roda) --------
const orphanUsers = db.users.countDocuments({ $and: [ORPHAN, { role: { $ne: 'SUPER_ADMIN' } }] });
const superAdmins = db.users.countDocuments({ role: 'SUPER_ADMIN' });
const orphanProducts = db.products.countDocuments(ORPHAN);
const orphanSales = db.sales.countDocuments(ORPHAN);

log(`Banco: ${CONFIG.dbName}`);
log(`Usuários órfãos (não super admin): ${orphanUsers}  | super admins preservados: ${superAdmins}`);
log(`Produtos órfãos: ${orphanProducts}`);
log(`Vendas órfãs: ${orphanSales}`);

if (CONFIG.dryRun) {
  log('DRY_RUN = true → nada foi gravado. Revise as contagens e rode de novo com dryRun=false.');
  quit(0);
}

// -------- 1) Cria o Tenant --------
const tenantId = new ObjectId();
const tenantIdStr = tenantId.toString(); // os campos tenantId (String) usam o hex do _id

db.tenants.insertOne({
  _id: tenantId,
  name: CONFIG.tenant.name,
  slug: CONFIG.tenant.slug,
  status: 'ACTIVE',
  planId: CONFIG.tenant.planId,
  billingEmail: CONFIG.tenant.billingEmail,
  trialEndsAt: null,
  blockedAt: null,
  blockReason: null,
  createdAt: now,
  updatedAt: now,
  _class: 'com.binitech.pdv.adapters.outbound.persistence.document.TenantDocument',
});
log(`Tenant criado: _id=${tenantIdStr} slug=${CONFIG.tenant.slug} plano=${CONFIG.tenant.planId}`);

// -------- 2) Backfill de tenantId --------
const usersRes = db.users.updateMany(
  { $and: [ORPHAN, { role: { $ne: 'SUPER_ADMIN' } }] },
  { $set: { tenantId: tenantIdStr } }
);
log(`Usuários atualizados: ${usersRes.modifiedCount}`);

const productsRes = db.products.updateMany(ORPHAN, { $set: { tenantId: tenantIdStr } });
log(`Produtos atualizados: ${productsRes.modifiedCount}`);

const salesRes = db.sales.updateMany(ORPHAN, { $set: { tenantId: tenantIdStr } });
log(`Vendas atualizadas: ${salesRes.modifiedCount}`);

// -------- 3) Promove usuário a TENANT_ADMIN (opcional) --------
if (CONFIG.promoteUsernameToTenantAdmin) {
  const promoteRes = db.users.updateOne(
    { username: CONFIG.promoteUsernameToTenantAdmin, tenantId: tenantIdStr },
    { $set: { role: 'TENANT_ADMIN' } }
  );
  log(
    `Promoção a TENANT_ADMIN (${CONFIG.promoteUsernameToTenantAdmin}): ` +
      `${promoteRes.matchedCount} encontrado(s), ${promoteRes.modifiedCount} alterado(s)`
  );
}

// -------- 4) Assinatura cortesia ACTIVE (opcional) --------
if (CONFIG.createSubscription) {
  db.subscriptions.insertOne({
    _id: new ObjectId(),
    tenantId: tenantIdStr,
    stripeSubscriptionId: null,
    stripeCustomerId: null,
    stripePriceId: null, // cortesia não tem plano no Stripe
    planTier: CONFIG.tenant.planId,
    status: 'ACTIVE',
    currentPeriodStart: now,
    currentPeriodEnd: null,
    nextBillingDate: null,
    lastPaymentDate: null,
    failedPaymentCount: 0,
    cancelledAt: null,
    createdAt: now,
    updatedAt: now,
    _class: 'com.binitech.pdv.adapters.outbound.persistence.document.SubscriptionDocument',
  });
  log('Assinatura cortesia ACTIVE criada.');
}

// -------- 5) Limpa refresh tokens órfãos (opcional) --------
if (CONFIG.clearOrphanRefreshTokens) {
  const rtRes = db.refresh_tokens.deleteMany(ORPHAN);
  log(`Refresh tokens órfãos removidos: ${rtRes.deletedCount} (usuários farão login novamente).`);
}

// -------- Verificação final --------
const remainingOrphans =
  db.users.countDocuments({ $and: [ORPHAN, { role: { $ne: 'SUPER_ADMIN' } }] }) +
  db.products.countDocuments(ORPHAN) +
  db.sales.countDocuments(ORPHAN);

log(`Órfãos restantes (esperado 0): ${remainingOrphans}`);
log('Concluído. A loja agora loga com:');
log(`   Loja: ${CONFIG.tenant.slug}   |   Usuário: <username>   |   senha: (a mesma)`);

/*
 * ROLLBACK (se precisar desfazer):
 *   const t = db.tenants.findOne({ slug: 'banca-oz-bini' });
 *   const id = t._id.toString();
 *   db.users.updateMany({ tenantId: id }, { $set: { tenantId: null } });
 *   db.products.updateMany({ tenantId: id }, { $set: { tenantId: null } });
 *   db.sales.updateMany({ tenantId: id }, { $set: { tenantId: null } });
 *   db.subscriptions.deleteMany({ tenantId: id });
 *   db.tenants.deleteOne({ _id: t._id });
 *   // (a promoção a TENANT_ADMIN não é revertida automaticamente)
 */
