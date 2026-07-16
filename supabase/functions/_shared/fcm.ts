// supabase/functions/_shared/fcm.ts
// Firebase Cloud Messaging HTTP v1 sender for edge functions. Mints a
// short-lived OAuth2 access token from a Firebase service-account key
// (RS256 JWT -> Google token endpoint, cached in memory) and posts messages
// to the FCM v1 endpoint. No external deps — Web Crypto handles the signing.
//
// Env:
//   - FCM_SERVICE_ACCOUNT   full service-account JSON, set via
//     `supabase secrets set FCM_SERVICE_ACCOUNT="$(cat service-account.json)"`

interface ServiceAccount {
  client_email: string;
  private_key: string;
  project_id: string;
}

const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const TOKEN_URL = "https://oauth2.googleapis.com/token";

let account: ServiceAccount | null = null;
let cachedToken: { value: string; expiresAt: number } | null = null;

function serviceAccount(): ServiceAccount {
  if (account) return account;
  const raw = Deno.env.get("FCM_SERVICE_ACCOUNT");
  if (!raw) throw new Error("FCM_SERVICE_ACCOUNT not set");
  account = JSON.parse(raw) as ServiceAccount;
  return account;
}

export function fcmProjectId(): string {
  return serviceAccount().project_id;
}

function base64url(input: ArrayBuffer | string): string {
  let bin: string;
  if (typeof input === "string") {
    bin = input;
  } else {
    const bytes = new Uint8Array(input);
    bin = "";
    for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
  }
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToPkcs8(pem: string): ArrayBuffer {
  const body = pem
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s+/g, "");
  const bin = atob(body);
  const buf = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) buf[i] = bin.charCodeAt(i);
  return buf.buffer;
}

async function signJwt(sa: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const claims = {
    iss: sa.client_email,
    scope: FCM_SCOPE,
    aud: TOKEN_URL,
    iat: now,
    exp: now + 3600,
  };
  const unsigned =
    `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(claims))}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToPkcs8(sa.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned),
  );
  return `${unsigned}.${base64url(sig)}`;
}

export async function getAccessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.expiresAt - 60 > now) return cachedToken.value;

  const jwt = await signJwt(serviceAccount());
  const res = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  if (!res.ok) throw new Error(`token endpoint ${res.status}: ${await res.text()}`);

  const data = (await res.json()) as { access_token: string; expires_in: number };
  cachedToken = { value: data.access_token, expiresAt: now + data.expires_in };
  return data.access_token;
}

export type FcmSendResult =
  | { ok: true }
  | { ok: false; unregistered: boolean; status: number; body: string };

export async function sendToToken(
  accessToken: string,
  projectId: string,
  message: Record<string, unknown>,
): Promise<FcmSendResult> {
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ message }),
    },
  );
  if (res.ok) return { ok: true };

  const body = await res.text();
  // A dead token comes back as 404 / UNREGISTERED / NOT_FOUND — prune those.
  const unregistered = res.status === 404 || /UNREGISTERED|NOT_FOUND/.test(body);
  return { ok: false, unregistered, status: res.status, body };
}
