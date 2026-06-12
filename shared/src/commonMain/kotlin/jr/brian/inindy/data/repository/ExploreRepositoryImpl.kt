package jr.brian.inindy.data.repository

import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.ExploreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class ExploreRepositoryImpl : ExploreRepository {
    private val rsvpdPostIds = mutableSetOf<String>()
    private val postsState = MutableStateFlow(listOf<Post>())

    override fun getPosts(): Flow<Result<List<Post>>> =
        postsState.asStateFlow().map { Result.success(it) }

    override suspend fun rsvp(postId: String): Result<Unit> {
        if (postId in rsvpdPostIds) return Result.success(Unit)
        rsvpdPostIds += postId
        postsState.value = postsState.value.map { post ->
            if (post.id == postId) post.copy(rsvpCount = post.rsvpCount + 1) else post
        }
        return Result.success(Unit)
    }

    override suspend fun unRsvp(postId: String): Result<Unit> {
        if (postId !in rsvpdPostIds) return Result.success(Unit)
        rsvpdPostIds -= postId
        postsState.value = postsState.value.map { post ->
            if (post.id == postId) {
                post.copy(rsvpCount = (post.rsvpCount - 1).coerceAtLeast(0))
            } else post
        }
        return Result.success(Unit)
    }

    override fun isRsvpd(postId: String): Boolean = postId in rsvpdPostIds

    override suspend fun getAttendees(postId: String): Result<List<User>> {
        val rsvpCount = postsState.value.firstOrNull { it.id == postId }?.rsvpCount
            ?: defaultAttendeeCount(postId)
        val pool = emptyList<User>() // TODO: Get actual pool
        val seed = postId.hashCode()
        val rotated = pool.indices.map { i -> pool[((i + seed) % pool.size + pool.size) % pool.size] }
        val list = rotated.take(rsvpCount.coerceAtMost(pool.size))
        return Result.success(list)
    }

    private fun defaultAttendeeCount(postId: String): Int =
        (postId.hashCode().let { if (it < 0) -it else it } % 8) + 2
}
