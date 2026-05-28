package jr.brian.inindy.data.repository

import jr.brian.inindy.domain.model.CreatePostRequest
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.PostAudience
import jr.brian.inindy.domain.model.PostTag
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePostRepository : PostRepository {

    private val currentUser = User(
        id = "me",
        fullName = "Brian",
        avatarUrl = null,
        phoneVerified = true,
        neighborhoodId = "broad-ripple"
    )

    private val state = MutableStateFlow(buildSeedPosts())

    override fun observeUserPosts(): Flow<List<Post>> = state.asStateFlow()

    override suspend fun getUserPosts(): Result<List<Post>> {
        delay(SHORT_DELAY_MS)
        return Result.success(state.value)
    }

    override suspend fun getPostById(postId: String): Result<Post> {
        delay(SHORT_DELAY_MS)
        val match = state.value.firstOrNull { it.id == postId }
            ?: return Result.failure(NoSuchElementException("Post $postId not found"))
        return Result.success(match)
    }

    override suspend fun createPost(request: CreatePostRequest): Result<Post> {
        delay(NETWORK_DELAY_MS)
        val now = currentTimeMillis()
        val newPost = Post(
            id = "me-${now}",
            userId = currentUser.id,
            title = request.title,
            description = request.description,
            latitude = request.latitude,
            longitude = request.longitude,
            address = request.address,
            startsAt = request.startsAt,
            endsAt = request.endsAt,
            createdAt = now,
            tags = request.tags,
            images = request.imageUris,
            videos = emptyList(),
            rsvpCount = 0,
            author = currentUser.toAuthor()
        )
        state.value = listOf(newPost) + state.value
        return Result.success(newPost)
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        delay(SHORT_DELAY_MS)
        state.value = state.value.filterNot { it.id == postId }
        return Result.success(Unit)
    }

    private fun User.toAuthor(): User = User(
        id = id,
        fullName = fullName,
        avatarUrl = avatarUrl
    )

    private fun buildSeedPosts(): List<Post> {
        val now = currentTimeMillis()
        val twoDays = 2 * 86_400_000L
        val fiveDays = 5 * 86_400_000L
        val oneWeekAgo = 7 * 86_400_000L
        return listOf(
            Post(
                id = "me-1",
                userId = currentUser.id,
                title = "Sunday morning Monon walk",
                description = "Easy 4 miler from 49th down to Broad Ripple Park. Coffee after.",
                latitude = 39.8672,
                longitude = -86.1414,
                address = "Monon Trail @ 49th St, Indianapolis",
                startsAt = now + twoDays + 9 * 3_600_000L,
                endsAt = now + twoDays + 11 * 3_600_000L,
                createdAt = now - 86_400_000L,
                tags = listOf(PostTag.WALK),
                images = listOf(
                    "https://www.railstotrails.org/nitropack_static/pVKvLDLqSrRUaEyiNwEcSJukRyhzZaDI/assets/images/optimized/rev-958f862/www.railstotrails.org/wp-content/uploads/2024/12/Indianas-Monon-Trail_IMG_8344_Photo-by-Robert-Annis.jpg"
                ),
                videos = emptyList(),
                rsvpCount = 4,
                author = currentUser.toAuthor()
            ),
            Post(
                id = "me-2",
                userId = currentUser.id,
                title = "Picnic in Garfield Park",
                description = "Bring a blanket and snacks. Dogs welcome.",
                latitude = 39.7365,
                longitude = -86.1425,
                address = "Garfield Park, Indianapolis",
                startsAt = now + fiveDays + 13 * 3_600_000L,
                endsAt = now + fiveDays + 16 * 3_600_000L,
                createdAt = now - 3 * 86_400_000L,
                tags = listOf(PostTag.PICNIC),
                images = listOf(
                    "https://www.blackfoodie.co/wp-content/uploads/2020/08/Copy-of-random-for-reference.png"
                ),
                videos = emptyList(),
                rsvpCount = 9,
                author = currentUser.toAuthor()
            ),
            Post(
                id = "me-3",
                userId = currentUser.id,
                title = "Eagle Creek loop",
                description = "Past event — went great, ~12 of us made it out.",
                latitude = 39.8283,
                longitude = -86.2779,
                address = "Eagle Creek Park, Indianapolis",
                startsAt = now - oneWeekAgo + 9 * 3_600_000L,
                endsAt = now - oneWeekAgo + 11 * 3_600_000L,
                createdAt = now - 2 * oneWeekAgo,
                tags = listOf(PostTag.HIKE),
                images = listOf(
                    "https://www.visitindy.com/imager/files_idss_com/C516/DMS_image_3410_e7b4e5d5-5056-854c-b6c0e14aadaa42c5_e45adf5f6bc0c5c2a30a39868f44eab6.jpg"
                ),
                videos = emptyList(),
                rsvpCount = 12,
                author = currentUser.toAuthor()
            )
        )
    }

    private companion object {
        const val SHORT_DELAY_MS = 200L
        const val NETWORK_DELAY_MS = 800L
    }
}

@Suppress("unused")
private fun PostAudience.tagAudience(): String = when (this) {
    is PostAudience.Neighborhood -> "neighborhood"
    is PostAudience.GroupAudience -> "group:$groupId"
}
