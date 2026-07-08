// supabase/functions/moderate-post/index.ts
// Moderates post text and images. Invoked by a database webhook on the `posts`
// table (INSERT + UPDATE) with body `{ type, record, old_record }`.
//
// Pipeline (fail-closed — on any moderation error the row stays `pending`,
// hidden by RLS):
//   1. Loop guard: skip if the row is not currently `pending` (our own write
//      would otherwise re-trigger the UPDATE webhook).
//   2. Text moderation on title + description (profanity + OpenAI).
//   3. Image moderation on each linked post_image (parallel).
//   4. Write `approved` / `rejected` back via the service role.
//
// Env:
//   - OPENAI_API_KEY               (set via `supabase secrets set`)
//   - SUPABASE_URL                 (auto-injected)
//   - SUPABASE_SERVICE_ROLE_KEY    (auto-injected)

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { checkText, checkImage } from "../_shared/moderation.ts";

type WebhookPayload = {
  type: "INSERT" | "UPDATE" | "DELETE";
  record: {
    id: string;
    user_id: string | null;
    group_id: string | null;
    title: string | null;
    description: string | null;
    moderation_status: string | null;
  } | null;
  old_record: unknown;
};

Deno.serve(async (req: Request) => {
  try {
    const payload = (await req.json()) as WebhookPayload;
    const record = payload?.record;

    if (!record?.id) {
      return json({ error: "Missing record.id" }, 200);
    }

    // ── 1. Loop guard ───────────────────────────────────────────────────
    if (record.moderation_status !== "pending") {
      return json({ skipped: true, reason: "not_pending" }, 200);
    }

    const admin = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
      { auth: { autoRefreshToken: false, persistSession: false } }
    );

    const postId = record.id;
    const combinedText = `${record.title ?? ""}\n${record.description ?? ""}`;

    // ── 2. Text moderation ──────────────────────────────────────────────
    try {
      const textResult = await checkText(combinedText);
      if (textResult.rejected) {
        await writeDecision(admin, postId, "rejected", textResult.reason ?? "flagged_text");
        return json({ ok: true, decision: "rejected", reason: textResult.reason }, 200);
      }
    } catch (err) {
      console.error("moderate-post: text moderation failed", err);
      return json({ left_pending: true, reason: "openai_text_error" }, 200);
    }

    // ── 3. Image moderation ─────────────────────────────────────────────
    const { data: images, error: imagesError } = await admin
      .from("post_images")
      .select("storage_url")
      .eq("post_id", postId);

    if (imagesError) {
      console.error("moderate-post: fetch post_images failed", imagesError);
      return json({ left_pending: true, reason: "images_query_error" }, 200);
    }

    if (images && images.length > 0) {
      try {
        const results = await Promise.all(
          images.map((img) => checkImage(img.storage_url))
        );
        const flagged = results.find((r) => r.rejected);
        if (flagged) {
          await writeDecision(admin, postId, "rejected", flagged.reason ?? "flagged_image");
          return json({ ok: true, decision: "rejected", reason: flagged.reason }, 200);
        }
      } catch (err) {
        console.error("moderate-post: image moderation failed", err);
        return json({ left_pending: true, reason: "openai_image_error" }, 200);
      }
    }

    // ── 4. Approve ──────────────────────────────────────────────────────
    await writeDecision(admin, postId, "approved", null);

    // ── 5. Fan out group notifications ─────────────────────────────────
    // Only for group posts (group_id non-null). The Postgres function is
    // idempotent and handles author-exclusion + mute filtering, so we just
    // fire and log — failures here don't roll back the approval.
    if (record.group_id && record.user_id) {
      const { error: notifyError } = await admin.rpc("notify_group_post", {
        p_post_id: postId,
        p_group_id: record.group_id,
        p_actor_id: record.user_id,
      });
      if (notifyError) {
        console.error("moderate-post: notify_group_post failed", notifyError);
      }
    }

    return json({ ok: true, decision: "approved" }, 200);
  } catch (error) {
    console.error("moderate-post: unexpected error", error);
    return json({ error: "Internal server error" }, 500);
  }
});

async function writeDecision(
  admin: ReturnType<typeof createClient>,
  postId: string,
  status: "approved" | "rejected",
  reason: string | null
): Promise<void> {
  const { error } = await admin
    .from("posts")
    .update({ moderation_status: status, moderation_reason: reason })
    .eq("id", postId);

  if (error) {
    console.error("moderate-post: write decision failed", { postId, status, error });
    throw error;
  }
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
