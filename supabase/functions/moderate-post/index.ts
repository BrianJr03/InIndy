// supabase/functions/moderate-post/index.ts
// Moderates post text and images. Invoked by a database webhook on the `posts`
// table (INSERT + UPDATE) with body `{ type, record, old_record }`.
//
// Pipeline (fail-closed — on any error the row stays `pending`, hidden by RLS):
//   1. Loop guard: skip if the row is not currently `pending` (our own write
//      would otherwise re-trigger the UPDATE webhook).
//   2. Profanity check on title + description via the `obscenity` library.
//   3. OpenAI `omni-moderation-latest` on title + description.
//   4. OpenAI `omni-moderation-latest` on each post image (in parallel).
//   5. Write the decision back to `posts.moderation_status` /
//      `posts.moderation_reason` using the service role.
//
// Env:
//   - OPENAI_API_KEY               (set via `supabase secrets set`)
//   - SUPABASE_URL                 (auto-injected)
//   - SUPABASE_SERVICE_ROLE_KEY    (auto-injected)

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import {
  RegExpMatcher,
  englishDataset,
  englishRecommendedTransformers,
} from "npm:obscenity";

// ── Tunables ───────────────────────────────────────────────────────────────
// Image category thresholds. OpenAI category scores are in [0, 1]; the API
// also returns its own `flagged` boolean for text, which we trust for step 3.
const IMAGE_SEXUAL_THRESHOLD = 0.5;      // nudity maps to `sexual`
const IMAGE_VIOLENCE_THRESHOLD = 0.5;
const IMAGE_SELF_HARM_THRESHOLD = 0.5;

const OPENAI_MODERATION_URL = "https://api.openai.com/v1/moderations";
const OPENAI_MODEL = "omni-moderation-latest";

const profanityMatcher = new RegExpMatcher({
  ...englishDataset.build(),
  ...englishRecommendedTransformers,
});

type WebhookPayload = {
  type: "INSERT" | "UPDATE" | "DELETE";
  record: {
    id: string;
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

    const openaiKey = Deno.env.get("OPENAI_API_KEY");
    if (!openaiKey) {
      console.error("moderate-post: OPENAI_API_KEY is not set — leaving pending");
      return json({ left_pending: true, reason: "missing_openai_key" }, 200);
    }

    const admin = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
      { auth: { autoRefreshToken: false, persistSession: false } }
    );

    const postId = record.id;
    const title = record.title ?? "";
    const description = record.description ?? "";
    const combinedText = `${title}\n${description}`.trim();

    // ── 2. Profanity check ──────────────────────────────────────────────
    if (combinedText && profanityMatcher.hasMatch(combinedText)) {
      await writeDecision(admin, postId, "rejected", "profanity");
      return json({ ok: true, decision: "rejected", reason: "profanity" }, 200);
    }

    // ── 3. Text moderation via OpenAI ───────────────────────────────────
    if (combinedText) {
      try {
        const textResult = await moderateText(openaiKey, combinedText);
        if (textResult.flagged) {
          await writeDecision(admin, postId, "rejected", textResult.reason);
          return json(
            { ok: true, decision: "rejected", reason: textResult.reason },
            200
          );
        }
      } catch (err) {
        console.error("moderate-post: text moderation failed", err);
        return json({ left_pending: true, reason: "openai_text_error" }, 200);
      }
    }

    // ── 4. Image moderation via OpenAI ──────────────────────────────────
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
          images.map((img) => moderateImage(openaiKey, img.storage_url))
        );
        const flagged = results.find((r) => r.flagged);
        if (flagged) {
          await writeDecision(admin, postId, "rejected", flagged.reason);
          return json(
            { ok: true, decision: "rejected", reason: flagged.reason },
            200
          );
        }
      } catch (err) {
        console.error("moderate-post: image moderation failed", err);
        return json({ left_pending: true, reason: "openai_image_error" }, 200);
      }
    }

    // ── 5. Approve ──────────────────────────────────────────────────────
    await writeDecision(admin, postId, "approved", null);
    return json({ ok: true, decision: "approved" }, 200);
  } catch (error) {
    console.error("moderate-post: unexpected error", error);
    return json({ error: "Internal server error" }, 500);
  }
});

// ── Helpers ────────────────────────────────────────────────────────────────

type ModerationOutcome = { flagged: boolean; reason: string };

async function moderateText(
  apiKey: string,
  text: string
): Promise<ModerationOutcome> {
  const res = await fetch(OPENAI_MODERATION_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: OPENAI_MODEL,
      input: [{ type: "text", text }],
    }),
  });

  if (!res.ok) {
    const detail = await safeReadText(res);
    throw new Error(`OpenAI text moderation ${res.status}: ${detail}`);
  }

  const json = await res.json();
  const result = json?.results?.[0];
  if (!result) {
    throw new Error("OpenAI text moderation: no result in response");
  }

  if (result.flagged) {
    return { flagged: true, reason: firstFlaggedCategory(result) ?? "flagged_text" };
  }
  return { flagged: false, reason: "" };
}

async function moderateImage(
  apiKey: string,
  url: string
): Promise<ModerationOutcome> {
  const res = await fetch(OPENAI_MODERATION_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: OPENAI_MODEL,
      input: [{ type: "image_url", image_url: { url } }],
    }),
  });

  if (!res.ok) {
    const detail = await safeReadText(res);
    throw new Error(`OpenAI image moderation ${res.status}: ${detail}`);
  }

  const json = await res.json();
  const result = json?.results?.[0];
  if (!result) {
    throw new Error("OpenAI image moderation: no result in response");
  }

  const scores = result.category_scores ?? {};
  const sexual = scores["sexual"] ?? 0;
  const violence = scores["violence"] ?? 0;
  const selfHarm = scores["self-harm"] ?? 0;

  if (sexual >= IMAGE_SEXUAL_THRESHOLD) return { flagged: true, reason: "sexual" };
  if (violence >= IMAGE_VIOLENCE_THRESHOLD) return { flagged: true, reason: "violence" };
  if (selfHarm >= IMAGE_SELF_HARM_THRESHOLD) return { flagged: true, reason: "self-harm" };

  return { flagged: false, reason: "" };
}

function firstFlaggedCategory(result: {
  categories?: Record<string, boolean>;
}): string | null {
  const categories = result.categories ?? {};
  for (const [name, flagged] of Object.entries(categories)) {
    if (flagged) return name;
  }
  return null;
}

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

async function safeReadText(res: Response): Promise<string> {
  try {
    return await res.text();
  } catch {
    return "<unreadable response body>";
  }
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
