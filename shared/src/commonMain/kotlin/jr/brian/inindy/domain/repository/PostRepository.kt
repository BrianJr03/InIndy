package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.CreatePostRequest
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun observeUserPosts(): Flow<List<Post>>
    fun observeNeighborhoodOnlyFeed(neighborhoodId: String): Flow<Result<List<Post>>>
    fun observeGroupFeed(groupId: String): Flow<Result<List<Post>>>
    fun observePost(postId: String): Flow<Result<Post>>
    suspend fun getUserPosts(): Result<List<Post>>
    suspend fun getPostById(postId: String): Result<Post>
    suspend fun createPost(request: CreatePostRequest): Result<Post>
    suspend fun updatePost(postId: String, request: CreatePostRequest): Result<Post>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun getNeighborhoodFeed(neighborhoodId: String): Result<List<Post>>
    suspend fun getNeighborhoodOnlyFeed(neighborhoodId: String): Result<List<Post>>
    suspend fun getGroupFeed(groupId: String): Result<List<Post>>
    suspend fun getPostAttendees(postId: String): Result<List<User>>
}

class PostDeletedException : Exception("Post deleted")
