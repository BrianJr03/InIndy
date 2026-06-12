package jr.brian.inindy.data.repository

import jr.brian.inindy.domain.repository.RsvpRepository

class FakeRsvpRepository : RsvpRepository {
    private val cache = mutableSetOf<String>()

    override suspend fun getRsvpdPostIds(userId: String): Result<Set<String>> =
        Result.success(cache.toSet())

    override suspend fun rsvp(postId: String): Result<Unit> {
        cache += postId
        return Result.success(Unit)
    }

    override suspend fun unRsvp(postId: String): Result<Unit> {
        cache -= postId
        return Result.success(Unit)
    }

    override fun isRsvpd(postId: String): Boolean = postId in cache
}
