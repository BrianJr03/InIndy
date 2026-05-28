package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface ExploreRepository {
    fun getPosts(): Flow<Result<List<Post>>>
    suspend fun rsvp(postId: String): Result<Post>
    suspend fun unRsvp(postId: String): Result<Post>
    fun isRsvpd(postId: String): Boolean
}
