// supabase/functions/delete-account/index.ts
// Permanently deletes the calling user's account.
//
// Two-step delete (public.users.id has no FK to auth.users):
//   1. Reassign groups.created_by to the oldest remaining admin per group.
//      Groups with no other admin are left as-is and cascade-delete in step 2.
//   2. DELETE FROM public.users WHERE id = <caller>. This cascades posts,
//      rsvps, group_members, group_invites, follows, user_interests,
//      user_stats, and any groups whose created_by was still the caller.
//   3. auth.admin.deleteUser(<caller>) — needs the service role key.
//
// The caller's user id is resolved server-side from the JWT — never trusted
// from the request body — so one user cannot delete another.
//
// Required secrets (set via `supabase secrets set` before deploy):
//   - SUPABASE_URL
//   - SUPABASE_ANON_KEY
//   - SUPABASE_SERVICE_ROLE_KEY

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    // ── 1. Verify JWT and resolve caller ─────────────────────────────────
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return jsonResponse({ error: "Missing authorization header" }, 401);
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const anonClient = createClient(
      supabaseUrl,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: authHeader } } }
    );

    const { data: { user }, error: authError } = await anonClient.auth.getUser();
    if (authError || !user) {
      return jsonResponse({ error: "Unauthorized" }, 401);
    }

    const userId = user.id;

    // ── 2. Service-role client for privileged writes ─────────────────────
    const admin = createClient(
      supabaseUrl,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
      { auth: { autoRefreshToken: false, persistSession: false } }
    );

    // ── 3. Reassign groups founded by the caller ─────────────────────────
    // groups.created_by is NOT NULL, so we can't null it out. For each group
    // the caller founded, pick the oldest remaining admin (excluding the
    // caller) and hand ownership to them. Groups with no other admin fall
    // through and cascade-delete in step 4.
    const { data: ownedGroups, error: ownedGroupsError } = await admin
      .from("groups")
      .select("id")
      .eq("created_by", userId);

    if (ownedGroupsError) {
      console.error("delete-account: fetch owned groups failed", ownedGroupsError);
      return jsonResponse({ error: "Failed to enumerate owned groups" }, 500);
    }

    for (const group of ownedGroups ?? []) {
      const { data: nextAdmin, error: nextAdminError } = await admin
        .from("group_members")
        .select("user_id, joined_at")
        .eq("group_id", group.id)
        .eq("role", "admin")
        .neq("user_id", userId)
        .order("joined_at", { ascending: true })
        .limit(1)
        .maybeSingle();

      if (nextAdminError) {
        console.error(
          `delete-account: find next admin failed for group ${group.id}`,
          nextAdminError
        );
        return jsonResponse({ error: "Failed to reassign group ownership" }, 500);
      }

      if (nextAdmin) {
        const { error: reassignError } = await admin
          .from("groups")
          .update({ created_by: nextAdmin.user_id })
          .eq("id", group.id);

        if (reassignError) {
          console.error(
            `delete-account: reassign failed for group ${group.id}`,
            reassignError
          );
          return jsonResponse({ error: "Failed to reassign group ownership" }, 500);
        }
      }
      // If nextAdmin is null the group cascade-deletes in the next step.
    }

    // ── 4. Delete public.users — cascades app data + orphan groups ───────
    const { error: publicDeleteError } = await admin
      .from("users")
      .delete()
      .eq("id", userId);

    if (publicDeleteError) {
      console.error("delete-account: public.users delete failed", publicDeleteError);
      return jsonResponse({ error: "Failed to delete profile" }, 500);
    }

    // ── 5. Delete auth.users ─────────────────────────────────────────────
    const { error: authDeleteError } = await admin.auth.admin.deleteUser(userId);
    if (authDeleteError) {
      console.error("delete-account: auth.admin.deleteUser failed", authDeleteError);
      // public.users is already gone. Surface an error so the app can retry
      // or the operator can clean up the orphan auth row.
      return jsonResponse(
        { error: "Profile deleted but auth deletion failed. Contact support." },
        500
      );
    }

    return jsonResponse({ ok: true }, 200);
  } catch (error) {
    console.error("delete-account error:", error);
    return jsonResponse({ error: "Internal server error" }, 500);
  }
});

function jsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
