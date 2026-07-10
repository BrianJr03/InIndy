package jr.brian.inindy.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.RsvpRepository
import jr.brian.inindy.util.appLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseRsvpRepository(
    private val supabase: SupabaseClient,
    private val currentUserProvider: CurrentUserProvider
) : RsvpRepository {
    private val log = appLog("SupabaseRsvpRepository")
    private val cache = mutableSetOf<String>()
    private val cacheMutex = Mutex()

    override suspend fun getRsvpdPostIds(userId: String): Result<Set<String>> = runCatching {
        log.d { "getRsvpdPostIds — userId: $userId" }
        val rows = supabase.from(RSVPS_TABLE)
            .select(Columns.list("post_id")) {
                filter {
                    eq("user_id", userId)
                    eq("status", "confirmed")
                }
            }
            .decodeList<RsvpRow>()
        val ids = rows.map { it.postId }.toSet()
        cacheMutex.withLock {
            cache.clear()
            cache.addAll(ids)
        }
        log.d { "getRsvpdPostIds — cached ${ids.size} ids" }
        ids
    }.onFailure { e ->
        log.e(e) { "getRsvpdPostIds FAILED" }
    }

    override suspend fun rsvp(postId: String): Result<Unit> {
        if (isRsvpd(postId)) {
            log.d { "rsvp — already RSVP'd for $postId, skipping" }
            return Result.success(Unit)
        }
        return runCatching {
            val userId = currentUserProvider.get().userId ?: error("No signed-in user")
            log.d { "rsvp — postId: $postId, userId: $userId" }

            // Cache-first guard above keeps the common case from racing the
            // posts.rsvp_count update. A unique violation from a stale-cache
            // multi-device race surfaces as Result.failure here; the caller
            // can refresh and retry.
            supabase.from(RSVPS_TABLE).insert(
                RsvpInsert(postId = postId, userId = userId, status = "confirmed")
            )

            supabase.postgrest.rpc(
                function = "increment_rsvp_count",
                parameters = buildJsonObject { put("post_id", postId) }
            )

            cacheMutex.withLock { cache.add(postId) }
            log.i { "rsvp — success for $postId" }
        }.onFailure { e ->
            log.e(e) { "rsvp FAILED — postId: $postId" }
        }
    }

    override suspend fun unRsvp(postId: String): Result<Unit> {
        if (!isRsvpd(postId)) {
            log.d { "unRsvp — not RSVP'd for $postId, skipping" }
            return Result.success(Unit)
        }
        return runCatching {
            val userId = currentUserProvider.get().userId ?: error("No signed-in user")
            log.d { "unRsvp — postId: $postId, userId: $userId" }

            supabase.from(RSVPS_TABLE).delete {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }

            supabase.postgrest.rpc(
                function = "decrement_rsvp_count",
                parameters = buildJsonObject { put("post_id", postId) }
            )

            cacheMutex.withLock { cache.remove(postId) }
            log.i { "unRsvp — success for $postId" }
        }.onFailure { e ->
            log.e(e) { "unRsvp FAILED — postId: $postId" }
        }
    }

    override fun isRsvpd(postId: String): Boolean = postId in cache

    @Serializable
    private data class RsvpRow(@SerialName("post_id") val postId: String)

    @Serializable
    private data class RsvpInsert(
        @SerialName("post_id") val postId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("status") val status: String
    )

    private companion object {
        const val RSVPS_TABLE = "rsvps"
    }
}
