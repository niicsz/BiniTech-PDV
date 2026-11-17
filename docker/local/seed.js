const username = process.env.ADMIN_USERNAME;
const password = process.env.ADMIN_PASSWORD;
const authUrl = process.env.AUTH_URL;
const serviceKey = process.env.AUTH_SERVICE_KEY;
const identityId = "platform-admin";

if (!username || !password || !authUrl || !serviceKey) {
  throw new Error("ADMIN_USERNAME, ADMIN_PASSWORD, AUTH_URL e AUTH_SERVICE_KEY são obrigatórios");
}

const now = new Date();
db.users.updateOne(
  { username: username, tenantId: null },
  {
    $setOnInsert: {
      _id: identityId,
      username: username,
      name: "Administrador da plataforma",
      role: "SUPER_ADMIN",
      tenantId: null,
      active: true,
      createdAt: now,
      updatedAt: now,
    },
  },
  { upsert: true },
);

const admin = db.users.findOne({ username: username, tenantId: null });
let provisioned = false;
let lastProblem = "";

for (let attempt = 1; attempt <= 60 && !provisioned; attempt++) {
  try {
    const response = await fetch(authUrl + "/api/internal/identities/provision", {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Auth-Service-Key": serviceKey },
      body: JSON.stringify({ identityId: admin._id, username: username, password: password }),
    });
    if (response.ok || response.status === 409) {
      provisioned = true;
    } else if (response.status >= 500) {
      lastProblem = "Auth respondeu " + response.status;
      sleep(3000);
    } else {
      throw new Error("Auth recusou o provisionamento com status " + response.status);
    }
  } catch (error) {
    if (String(error.message).startsWith("Auth recusou")) {
      throw error;
    }
    lastProblem = "Auth ainda não respondeu";
    sleep(3000);
  }
}

if (!provisioned) {
  throw new Error("Não foi possível provisionar o administrador: " + lastProblem);
}

print("Administrador pronto: " + username);
