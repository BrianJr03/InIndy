// supabase/functions/moderate-message/index.ts
// Async redaction for group chat messages. Invoked by a database webhook on the
// `group_messages` table (INSERT only) with body `{ type, record }`.
//
// Pipeline (fail-OPEN — an OpenAI outage leaves messages visible; silently
// blanking real chat during an outage is worse UX than briefly missing a
// redaction):
//   1. Run checkText on record.body (shared helper covers profanity + OpenAI).
//   2. If flagged → service-role update `redacted = true` on this row.
//   3. If clean OR OpenAI errored → do nothing.
//
// The webhook is INSERT-only and redaction is an UPDATE, so there's no loop.
//
// Env:
//   - OPENAI_API_KEY               (set via `supabase secrets set`)
//   - SUPABASE_URL                 (auto-injected)
//   - SUPABASE_SERVICE_ROLE_KEY    (auto-injected)

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { checkText } from "../_shared/moderation.ts";

type WebhookPayload = {
  type: "INSERT" | "UPDATE" | "DELETE";
  record: {
    id: string;
    body: string | null;
  } | null;
};

Deno.serve(async (req: Request) => {
  try {
    const payload = (await req.json()) as WebhookPayload;
    const record = payload?.record;

    if (!record?.id) {
      return json({ error: "Missing record.id" }, 200);
    }

    const body = record.body ?? "";
    if (!body.trim()) {
      return json({ ok: true, decision: "empty" }, 200);
    }

    let textResult;
    try {
      textResult = await checkText(body);
    } catch (err) {
      // Fail-open: don't redact on OpenAI failure.
      console.error("moderate-message: checkText failed (fail-open)", err);
      return json({ ok: true, decision: "left_visible", reason: "openai_text_error" }, 200);
    }

    if (!textResult.rejected) {
      return json({ ok: true, decision: "clean" }, 200);
    }

    const admin = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
      { auth: { autoRefreshToken: false, persistSession: false } }
    );

    const { error } = await admin
      .from("group_messages")
      .update({ redacted: true })
      .eq("id", record.id);

    if (error) {
      console.error("moderate-message: redact write failed", { id: record.id, error });
      return json({ error: "redact_failed" }, 200);
    }

    return json({ ok: true, decision: "redacted", reason: textResult.reason }, 200);
  } catch (error) {
    console.error("moderate-message: unexpected error", error);
    return json({ error: "Internal server error" }, 500);
  }
});

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
