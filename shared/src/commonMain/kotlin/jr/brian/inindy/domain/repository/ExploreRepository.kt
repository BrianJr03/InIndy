package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ExploreRepository {
    fun getPosts(): Flow<Result<List<Post>>>
    suspend fun rsvp(postId: String): Result<Unit>
    suspend fun unRsvp(postId: String): Result<Unit>
    fun isRsvpd(postId: String): Boolean
    suspend fun getAttendees(postId: String): Result<List<User>>
}
