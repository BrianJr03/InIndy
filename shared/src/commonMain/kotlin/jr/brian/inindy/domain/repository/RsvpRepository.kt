package jr.brian.inindy.domain.repository

interface RsvpRepository {
    suspend fun getRsvpdPostIds(userId: String): Result<Set<String>>
    suspend fun rsvp(postId: String): Result<Unit>
    suspend fun unRsvp(postId: String): Result<Unit>
    fun isRsvpd(postId: String): Boolean
}
