package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.CreatePostRequest
import jr.brian.inindy.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun observeUserPosts(): Flow<List<Post>>
    suspend fun getUserPosts(): Result<List<Post>>
    suspend fun getPostById(postId: String): Result<Post>
    suspend fun createPost(request: CreatePostRequest): Result<Post>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun getNeighborhoodFeed(neighborhoodId: String): Result<List<Post>>
    suspend fun getNeighborhoodOnlyFeed(neighborhoodId: String): Result<List<Post>>
    suspend fun getGroupFeed(groupId: String): Result<List<Post>>
}
