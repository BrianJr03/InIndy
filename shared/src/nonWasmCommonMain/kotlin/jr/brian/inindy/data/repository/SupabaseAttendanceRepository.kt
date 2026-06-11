package jr.brian.inindy.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.AttendanceRecord
import jr.brian.inindy.domain.repository.AttendanceRepository
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseAttendanceRepository(
    private val supabase: SupabaseClient,
    private val currentUserProvider: CurrentUserProvider
) : AttendanceRepository {

    override suspend fun getAttendanceHistory(limit: Int): Result<List<AttendanceRecord>> = runCatching {
        val userId = currentUserProvider.get().userId
            ?: throw IllegalStateException("No signed-in user")

        val rows = supabase.from(RSVPS_TABLE)
            .select(Columns.raw(HISTORY_COLUMNS)) {
                filter { eq("user_id", userId) }
                order("created_at", order = Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<RsvpHistoryDto>()

        rows.mapNotNull { row ->
            val post = row.post ?: return@mapNotNull null
            AttendanceRecord(
                postId = row.postId,
                postTitle = post.title,
                postImageUrl = post.images.minByOrNull { it.sortOrder }?.storageUrl,
                hostName = post.author?.fullName ?: "Unknown",
                attendedAt = parseIso8601Utc(post.createdAt)
            )
        }
    }

    override suspend fun getAttendanceRate(): Result<Float> = runCatching {
        val userId = currentUserProvider.get().userId
            ?: throw IllegalStateException("No signed-in user")

        val rsvps = supabase.from(RSVPS_TABLE)
            .select(Columns.list("created_at")) {
                filter { eq("user_id", userId) }
            }
            .decodeList<RsvpCreatedAtDto>()

        if (rsvps.isEmpty()) return@runCatching 1.0f

        val cutoffMs = currentTimeMillis() - THIRTY_DAYS_MS
        val recent = rsvps.count { parseIso8601Utc(it.createdAt) >= cutoffMs }
        recent.toFloat() / rsvps.size.toFloat()
    }

    // ── ISO 8601 helpers ─────────────────────────────────────────────────────
    // Supabase emits timestamptz as "YYYY-MM-DDTHH:MM:SS[.fff]+00:00" — UTC.
    // Avoid pulling in kotlinx-datetime for this single use case.

    private fun parseIso8601Utc(iso: String): Long {
        val year = iso.substring(0, 4).toInt()
        val month = iso.substring(5, 7).toInt()
        val day = iso.substring(8, 10).toInt()
        val hour = iso.substring(11, 13).toInt()
        val minute = iso.substring(14, 16).toInt()
        val second = iso.substring(17, 19).toInt()
        return civilToEpochDays(year, month, day) * 86_400_000L +
                hour * 3_600_000L +
                minute * 60_000L +
                second * 1_000L
    }

    // Howard Hinnant's civil-from-days inverse — converts (y, m, d) to epoch days.
    private fun civilToEpochDays(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = if (y >= 0) y / 400 else (y - 399) / 400
        val yoe = (y - era * 400).toLong()
        val mp = if (month > 2) month - 3 else month + 9
        val doy = (153L * mp + 2L) / 5L + day.toLong() - 1L
        val doe = yoe * 365L + yoe / 4L - yoe / 100L + doy
        return era * 146097L + doe - 719468L
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Serializable
    private data class RsvpHistoryDto(
        @SerialName("post_id") val postId: String,
        @SerialName("created_at") val rsvpCreatedAt: String,
        @SerialName("post") val post: PostJoinDto? = null
    )

    @Serializable
    private data class PostJoinDto(
        @SerialName("title") val title: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("author") val author: AuthorJoinDto? = null,
        @SerialName("images") val images: List<PostImageJoinDto> = emptyList()
    )

    @Serializable
    private data class AuthorJoinDto(
        @SerialName("full_name") val fullName: String? = null
    )

    @Serializable
    private data class PostImageJoinDto(
        @SerialName("storage_url") val storageUrl: String,
        @SerialName("sort_order") val sortOrder: Int = 0
    )

    @Serializable
    private data class RsvpCreatedAtDto(
        @SerialName("created_at") val createdAt: String
    )

    private companion object {
        const val RSVPS_TABLE = "rsvps"
        const val THIRTY_DAYS_MS = 30L * 86_400_000L
        const val HISTORY_COLUMNS =
            "post_id, created_at, post:posts(title, created_at, author:users(full_name), images:post_images(storage_url, sort_order))"
    }
}
