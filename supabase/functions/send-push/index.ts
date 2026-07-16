// supabase/functions/send-push/index.ts
// OS-level push delivery. Invoked by a database webhook on the `notifications`
// table (INSERT only) with body `{ type, record }`. For each new notification
// row it looks up the recipient's device tokens, sends one FCM v1 message per
// token, and prunes any token FCM reports as unregistered.
//
// Muting is handled upstream: muted members never get a notifications row, so
// they never reach this function. New notification types need no change here
// beyond an optional case in buildMessage().
//
// Env:
//   - FCM_SERVICE_ACCOUNT          (set via `supabase secrets set`)
//   - SUPABASE_URL                 (auto-injected)
//   - SUPABASE_SERVICE_ROLE_KEY    (auto-injected)

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { fcmProjectId, getAccessToken, sendToToken } from "../_shared/fcm.ts";

type NotificationRecord = {
  id: string;
  user_id: string;
  type: string;
  actor_id: string | null;
  group_id: string | null;
  post_id: string | null;
};

type WebhookPayload = {
  type: "INSERT" | "UPDATE" | "DELETE";
  record: NotificationRecord | null;
};

Deno.serve(async (req: Request) => {
  try {
    const payload = (await req.json()) as WebhookPayload;
    if (payload.type !== "INSERT" || !payload.record?.id) {
      return json({ skipped: true }, 200);
    }
    const n = payload.record;

    const admin = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
      { auth: { autoRefreshToken: false, persistSession: false } },
    );

    const { data: tokens, error: tokensError } = await admin
      .from("device_tokens")
      .select("token")
      .eq("user_id", n.user_id);

    if (tokensError) {
      console.error("send-push: fetch tokens failed", tokensError);
      return json({ error: "tokens_query_error" }, 200);
    }
    if (!tokens || tokens.length === 0) {
      return json({ ok: true, sent: 0, reason: "no_tokens" }, 200);
    }

    const { title, body } = await buildMessage(admin, n);

    const data: Record<string, string> = { type: n.type, notification_id: n.id };
    if (n.post_id) data.post_id = n.post_id;
    if (n.group_id) data.group_id = n.group_id;

    const accessToken = await getAccessToken();
    const projectId = fcmProjectId();

    const dead: string[] = [];
    let sent = 0;

    await Promise.all(
      tokens.map(async ({ token }) => {
        const result = await sendToToken(accessToken, projectId, {
          token,
          notification: { title, body },
          data,
          android: {
            priority: "high",
            notification: { channel_id: "inindy_default" },
          },
        });
        if (result.ok) sent++;
        else if (result.unregistered) dead.push(token);
        else console.error("send-push: FCM error", result.status, result.body);
      }),
    );

    if (dead.length > 0) {
      const { error: pruneError } = await admin
        .from("device_tokens")
        .delete()
        .in("token", dead);
      if (pruneError) console.error("send-push: prune failed", pruneError);
    }

    return json({ ok: true, sent, pruned: dead.length }, 200);
  } catch (error) {
    console.error("send-push: unexpected error", error);
    return json({ error: "Internal server error" }, 500);
  }
});

async function buildMessage(
  admin: ReturnType<typeof createClient>,
  n: NotificationRecord,
): Promise<{ title: string; body: string }> {
  switch (n.type) {
    case "group_post": {
      const [groupName, actorName] = await Promise.all([
        n.group_id ? fetchGroupName(admin, n.group_id) : Promise.resolve(null),
        n.actor_id ? fetchActorName(admin, n.actor_id) : Promise.resolve(null),
      ]);
      return {
        title: groupName ?? "InIndy",
        body: `${actorName ?? "Someone"} shared a new post`,
      };
    }
    default:
      return { title: "InIndy", body: "You have a new notification" };
  }
}

async function fetchGroupName(
  admin: ReturnType<typeof createClient>,
  groupId: string,
): Promise<string | null> {
  const { data } = await admin.from("groups").select("name").eq("id", groupId).maybeSingle();
  return (data as { name?: string } | null)?.name ?? null;
}

async function fetchActorName(
  admin: ReturnType<typeof createClient>,
  actorId: string,
): Promise<string | null> {
  const { data } = await admin.from("users").select("full_name").eq("id", actorId).maybeSingle();
  return (data as { full_name?: string } | null)?.full_name ?? null;
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
