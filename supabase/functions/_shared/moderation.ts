// supabase/functions/_shared/moderation.ts
// Shared moderation primitives — profanity (obscenity) + OpenAI
// `omni-moderation-latest` for text and images. Used by moderate-post,
// moderate-user, and moderate-group.
//
// Both helpers THROW on final failure so callers can fail-closed. Callers
// decide what "fail-closed" means for their table (leave post pending,
// leave user field unchecked, etc).

import {
  RegExpMatcher,
  englishDataset,
  englishRecommendedTransformers,
} from "npm:obscenity";

// Image category thresholds. OpenAI category scores are in [0, 1]; nudity
// maps to `sexual`.
const IMAGE_SEXUAL_THRESHOLD = 0.5;
const IMAGE_VIOLENCE_THRESHOLD = 0.5;
const IMAGE_SELF_HARM_THRESHOLD = 0.5;

const OPENAI_MODERATION_URL = "https://api.openai.com/v1/moderations";
const OPENAI_MODEL = "omni-moderation-latest";

// Retry: 3 attempts, backoff ~500ms / 1s / 2s + jitter.
const RETRY_ATTEMPTS = 3;
const RETRY_BASE_MS = 500;

const profanityMatcher = new RegExpMatcher({
  ...englishDataset.build(),
  ...englishRecommendedTransformers,
});

export type ModerationOutcome = { rejected: boolean; reason?: string };

export async function checkText(
  text: string | null | undefined
): Promise<ModerationOutcome> {
  const trimmed = (text ?? "").trim();
  if (!trimmed) return { rejected: false };

  // Cheap local profanity check first.
  if (profanityMatcher.hasMatch(trimmed)) {
    return { rejected: true, reason: "profanity" };
  }

  const apiKey = requireApiKey();
  const json = await callOpenAI(apiKey, {
    model: OPENAI_MODEL,
    input: [{ type: "text", text: trimmed }],
  });

  const result = json?.results?.[0];
  if (!result) {
    throw new Error("OpenAI text moderation: no result in response");
  }

  if (result.flagged) {
    return {
      rejected: true,
      reason: firstFlaggedCategory(result) ?? "flagged_text",
    };
  }
  return { rejected: false };
}

export async function checkImage(
  url: string | null | undefined
): Promise<ModerationOutcome> {
  const trimmed = (url ?? "").trim();
  if (!trimmed) return { rejected: false };

  const apiKey = requireApiKey();
  const json = await callOpenAI(apiKey, {
    model: OPENAI_MODEL,
    input: [{ type: "image_url", image_url: { url: trimmed } }],
  });

  const result = json?.results?.[0];
  if (!result) {
    throw new Error("OpenAI image moderation: no result in response");
  }

  const scores = result.category_scores ?? {};
  const sexual = Number(scores["sexual"] ?? 0);
  const violence = Number(scores["violence"] ?? 0);
  const selfHarm = Number(scores["self-harm"] ?? 0);

  if (sexual >= IMAGE_SEXUAL_THRESHOLD) return { rejected: true, reason: "sexual" };
  if (violence >= IMAGE_VIOLENCE_THRESHOLD) return { rejected: true, reason: "violence" };
  if (selfHarm >= IMAGE_SELF_HARM_THRESHOLD) return { rejected: true, reason: "self-harm" };
  return { rejected: false };
}

function requireApiKey(): string {
  const key = Deno.env.get("OPENAI_API_KEY");
  if (!key) throw new Error("OPENAI_API_KEY is not set");
  return key;
}

// deno-lint-ignore no-explicit-any
async function callOpenAI(apiKey: string, body: unknown): Promise<any> {
  let lastError: unknown;
  for (let attempt = 0; attempt < RETRY_ATTEMPTS; attempt++) {
    let res: Response;
    try {
      res = await fetch(OPENAI_MODERATION_URL, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${apiKey}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      });
    } catch (err) {
      // Network error — retriable.
      lastError = err;
      await backoff(attempt);
      continue;
    }

    if (res.ok) {
      return await res.json();
    }

    const detail = await safeReadText(res);
    // 429 (rate limit) and 5xx (server error) are retriable.
    if (res.status === 429 || (res.status >= 500 && res.status < 600)) {
      lastError = new Error(`OpenAI moderation ${res.status}: ${detail}`);
      await backoff(attempt);
      continue;
    }
    // Non-retriable (4xx other than 429) — bad key, malformed request, etc.
    throw new Error(`OpenAI moderation ${res.status}: ${detail}`);
  }

  throw lastError instanceof Error
    ? lastError
    : new Error(`OpenAI moderation failed after ${RETRY_ATTEMPTS} attempts`);
}

async function backoff(attempt: number): Promise<void> {
  if (attempt >= RETRY_ATTEMPTS - 1) return;
  const base = RETRY_BASE_MS * Math.pow(2, attempt);
  const jitter = Math.random() * (RETRY_BASE_MS / 2);
  await sleep(base + jitter);
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

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function safeReadText(res: Response): Promise<string> {
  try {
    return await res.text();
  } catch {
    return "<unreadable response body>";
  }
}
