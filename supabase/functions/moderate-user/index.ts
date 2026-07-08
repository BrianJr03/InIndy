// supabase/functions/moderate-user/index.ts
// Moderates user profile fields (full_name, display_name, avatar_url).
// Invoked by a database webhook on `public.users` (INSERT + UPDATE).
//
// There is no `moderation_status` on users — the row must stay usable. Instead
// we **sanitize on reject**: null a bad avatar (falls back to initials in the
// app) or revert a bad name to its previous value (or a placeholder on INSERT).
//
// The change-guard doubles as a loop guard: our own sanitize-write produces
// clean values, so the follow-up UPDATE webhook re-checks them, finds nothing
// to reject, and returns without another write.
//
// Env:
//   - OPENAI_API_KEY               (set via `supabase secrets set`)
//   - SUPABASE_URL                 (auto-injected)
//   - SUPABASE_SERVICE_ROLE_KEY    (auto-injected)

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { checkText, checkImage } from "../_shared/moderation.ts";

const NAME_PLACEHOLDER = "Member";

type UserRow = {
  id: string;
  full_name: string | null;
  display_name: string | null;
  avatar_url: string | null;
};

type WebhookPayload = {
  type: "INSERT" | "UPDATE" | "DELETE";
  record: UserRow | null;
  old_record: UserRow | null;
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
    // On INSERT: check any field the caller actually set (non-empty).
    // On UPDATE: check only the fields whose value moved.
    const changedFullName = isInsert
      ? !!record.full_name
      : record.full_name !== oldRecord!.full_name;
    const changedDisplayName = isInsert
      ? !!record.display_name
      : record.display_name !== oldRecord!.display_name;
    const changedAvatar = isInsert
      ? !!record.avatar_url
      : record.avatar_url !== oldRecord!.avatar_url;

    if (!changedFullName && !changedDisplayName && !changedAvatar) {
      return json({ skipped: true, reason: "no_relevant_changes" }, 200);
    }

    // ── 2. Run checks (fail-closed on error: no writes, log, exit) ──────
    let rejectFullName: string | null = null;
    let rejectDisplayName: string | null = null;
    let rejectAvatar: string | null = null;

    try {
      if (changedFullName) {
        const r = await checkText(record.full_name);
        if (r.rejected) rejectFullName = r.reason ?? "flagged_text";
      }
      if (changedDisplayName) {
        const r = await checkText(record.display_name);
        if (r.rejected) rejectDisplayName = r.reason ?? "flagged_text";
      }
      if (changedAvatar) {
        const r = await checkImage(record.avatar_url);
        if (r.rejected) rejectAvatar = r.reason ?? "flagged_image";
      }
    } catch (err) {
      // OpenAI down after retries. Leave the just-set values in place; log so
      // the operator can review. This is intentional: repeatedly resetting a
      // user's name/avatar during a moderation outage is worse than briefly
      // leaving one unchecked field.
      console.error("moderate-user: moderation error, leaving fields unchecked", err);
      return json({ left_unchecked: true, reason: "openai_error" }, 200);
    }

    // ── 3. Sanitize on reject ───────────────────────────────────────────
    const updates: Partial<UserRow> = {};
    if (rejectFullName) {
      updates.full_name = oldRecord?.full_name ?? NAME_PLACEHOLDER;
    }
    if (rejectDisplayName) {
      updates.display_name = oldRecord?.display_name ?? NAME_PLACEHOLDER;
    }
    if (rejectAvatar) {
      updates.avatar_url = null;
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
      .from("users")
      .update(updates)
      .eq("id", record.id);

    if (writeError) {
      console.error("moderate-user: sanitize write failed", { id: record.id, writeError });
      return json({ error: "Sanitize write failed" }, 500);
    }

    return json(
      {
        ok: true,
        decision: "sanitized",
        fields: Object.keys(updates),
        reasons: {
          full_name: rejectFullName,
          display_name: rejectDisplayName,
          avatar_url: rejectAvatar,
        },
      },
      200
    );
  } catch (error) {
    console.error("moderate-user: unexpected error", error);
    return json({ error: "Internal server error" }, 500);
  }
});

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
