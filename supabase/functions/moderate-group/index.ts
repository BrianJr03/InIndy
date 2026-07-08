// supabase/functions/moderate-group/index.ts
// Moderates group fields (name, description, cover_url).
// Invoked by a database webhook on `public.groups` (INSERT + UPDATE).
//
// Same sanitize-on-reject pattern as moderate-user:
//   - name is NOT NULL — revert to old value on reject, else placeholder.
//   - description → null on reject.
//   - cover_url   → null on reject.
//
// The change-guard doubles as a loop guard (see moderate-user for details).
//
// Env:
//   - OPENAI_API_KEY               (set via `supabase secrets set`)
//   - SUPABASE_URL                 (auto-injected)
//   - SUPABASE_SERVICE_ROLE_KEY    (auto-injected)

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { checkText, checkImage } from "../_shared/moderation.ts";

const NAME_PLACEHOLDER = "Group";

type GroupRow = {
  id: string;
  name: string | null;
  description: string | null;
  cover_url: string | null;
};

type WebhookPayload = {
  type: "INSERT" | "UPDATE" | "DELETE";
  record: GroupRow | null;
  old_record: GroupRow | null;
};

Deno.serve(async (req: Request) => {
  try {
    const payload = (await req.json()) as WebhookPayload;
    const record = payload?.record;
    const oldRecord = payload?.old_record ?? null;

    if (!record?.id) {
      return json({ error: "Missing record.id" }, 200);
    }

    const isInsert = payload.type === "INSERT" || oldRecord === null;

    // ── 1. Change guard / loop guard ────────────────────────────────────
    const changedName = isInsert
      ? !!record.name
      : record.name !== oldRecord!.name;
    const changedDescription = isInsert
      ? !!record.description
      : record.description !== oldRecord!.description;
    const changedCover = isInsert
      ? !!record.cover_url
      : record.cover_url !== oldRecord!.cover_url;

    if (!changedName && !changedDescription && !changedCover) {
      return json({ skipped: true, reason: "no_relevant_changes" }, 200);
    }

    // ── 2. Run checks (fail-closed: no writes, log, exit) ───────────────
    let rejectName: string | null = null;
    let rejectDescription: string | null = null;
    let rejectCover: string | null = null;

    try {
      if (changedName) {
        const r = await checkText(record.name);
        if (r.rejected) rejectName = r.reason ?? "flagged_text";
      }
      if (changedDescription) {
        const r = await checkText(record.description);
        if (r.rejected) rejectDescription = r.reason ?? "flagged_text";
      }
      if (changedCover) {
        const r = await checkImage(record.cover_url);
        if (r.rejected) rejectCover = r.reason ?? "flagged_image";
      }
    } catch (err) {
      console.error("moderate-group: moderation error, leaving fields unchecked", err);
      return json({ left_unchecked: true, reason: "openai_error" }, 200);
    }

    // ── 3. Sanitize on reject ───────────────────────────────────────────
    // groups.name is NOT NULL — always resolve to a non-null string.
    const updates: Partial<GroupRow> = {};
    if (rejectName) {
      updates.name = oldRecord?.name ?? NAME_PLACEHOLDER;
    }
    if (rejectDescription) {
      updates.description = null;
    }
    if (rejectCover) {
      updates.cover_url = null;
    }

    if (Object.keys(updates).length === 0) {
      return json({ ok: true, decision: "approved" }, 200);
    }

    const admin = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
      { auth: { autoRefreshToken: false, persistSession: false } }
    );

    const { error: writeError } = await admin
      .from("groups")
      .update(updates)
      .eq("id", record.id);

    if (writeError) {
      console.error("moderate-group: sanitize write failed", { id: record.id, writeError });
      return json({ error: "Sanitize write failed" }, 500);
    }

    return json(
      {
        ok: true,
        decision: "sanitized",
        fields: Object.keys(updates),
        reasons: {
          name: rejectName,
          description: rejectDescription,
          cover_url: rejectCover,
        },
      },
      200
    );
  } catch (error) {
    console.error("moderate-group: unexpected error", error);
    return json({ error: "Internal server error" }, 500);
  }
});

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
