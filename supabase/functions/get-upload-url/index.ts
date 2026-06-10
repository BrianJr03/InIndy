// supabase/functions/get-upload-url/index.ts
// Generates a signed Cloudflare R2 upload URL for InIndy media uploads.
// Called by the app before uploading any image — never exposes R2 credentials to the client.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { S3Client, PutObjectCommand } from "https://esm.sh/@aws-sdk/client-s3@3";
import { getSignedUrl } from "https://esm.sh/@aws-sdk/s3-request-presigner@3";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    // ── 1. Verify JWT ────────────────────────────────────────────────────
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: "Missing authorization header" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: authHeader } } }
    );

    const { data: { user }, error: authError } = await supabase.auth.getUser();
    if (authError || !user) {
      return new Response(
        JSON.stringify({ error: "Unauthorized" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // ── 2. Parse request body ────────────────────────────────────────────
    const { fileName, contentType, context } = await req.json();

    if (!fileName || !contentType) {
      return new Response(
        JSON.stringify({ error: "fileName and contentType are required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Validate content type — images only
    if (!contentType.startsWith("image/")) {
      return new Response(
        JSON.stringify({ error: "Only image uploads are supported" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // ── 3. Build R2 key based on context ────────────────────────────────
    // context: "post" | "avatar" | "group"
    const uuid = crypto.randomUUID();
    const ext = fileName.split(".").pop() ?? "jpg";

    const key = (() => {
      switch (context) {
        case "avatar":
          return `avatars/${user.id}.${ext}`;          // overwrites on update
        case "group":
          return `groups/${uuid}/cover.${ext}`;
        case "post":
        default:
          return `posts/${user.id}/${uuid}.${ext}`;
      }
    })();

    // ── 4. Create R2 S3 client ───────────────────────────────────────────
    const r2 = new S3Client({
      region: "auto",
      endpoint: `https://${Deno.env.get("CLOUDFLARE_ACCOUNT_ID")}.r2.cloudflarestorage.com`,
      credentials: {
        accessKeyId: Deno.env.get("CLOUDFLARE_R2_ACCESS_KEY")!,
        secretAccessKey: Deno.env.get("CLOUDFLARE_R2_SECRET_KEY")!,
      },
    });

    // ── 5. Generate signed upload URL (expires in 60 seconds) ───────────
    const command = new PutObjectCommand({
      Bucket: Deno.env.get("CLOUDFLARE_R2_BUCKET")!,
      Key: key,
      ContentType: contentType,
    });

    const uploadUrl = await getSignedUrl(r2, command, { expiresIn: 60 });

    // ── 6. Build permanent CDN URL ───────────────────────────────────────
    const cdnBase = Deno.env.get("CLOUDFLARE_CDN_BASE")!;
    const publicUrl = `${cdnBase}/${key}`;

    // ── 7. Return both URLs ──────────────────────────────────────────────
    return new Response(
      JSON.stringify({ uploadUrl, publicUrl, key }),
      {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      }
    );

  } catch (error) {
    console.error("get-upload-url error:", error);
    return new Response(
      JSON.stringify({ error: "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});