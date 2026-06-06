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
            ?: feedPosts.firstOrNull { it.id == postId }
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

    override suspend fun getNeighborhoodFeed(neighborhoodId: String): Result<List<Post>> {
        delay(FEED_DELAY_MS)
        return Result.success(feedPosts.sortedByDescending { it.createdAt })
    }

    override suspend fun getNeighborhoodOnlyFeed(neighborhoodId: String): Result<List<Post>> {
        delay(FEED_DELAY_MS)
        return Result.success(
            feedPosts
                .filter { it.neighborhoodId == neighborhoodId && it.groupId == null }
                .sortedByDescending { it.createdAt }
        )
    }

    override suspend fun getGroupFeed(groupId: String): Result<List<Post>> {
        delay(FEED_DELAY_MS)
        return Result.success(
            feedPosts
                .filter { it.groupId == groupId }
                .sortedByDescending { it.createdAt }
        )
    }

    private fun User.toAuthor(): User = User(
        id = id,
        fullName = fullName,
        avatarUrl = avatarUrl
    )

    private val feedPosts: List<Post> = buildFeedPosts()

    private fun buildFeedPosts(): List<Post> {
        val now = currentTimeMillis()
        val hour = 3_600_000L
        val day = 86_400_000L
        return listOf(
            Post(
                id = "p1",
                userId = "u1",
                title = "Morning Monon run",
                description = "Morning trail run along the Monon, easy pace welcome!",
                latitude = 39.8676,
                longitude = -86.1431,
                address = "Monon Trail, Broad Ripple, Indianapolis",
                startsAt = now + 2 * hour,
                endsAt = now + 3 * hour,
                createdAt = now - 20 * 60_000L,
                tags = listOf(PostTag.RUN),
                images = listOf("https://picsum.photos/seed/run1/800/600"),
                videos = emptyList(),
                rsvpCount = 3,
                author = User("u1", "Michelle W.", "https://i.pravatar.cc/200?img=47"),
                neighborhoodId = "downtown",
                groupId = null
            ),
            Post(
                id = "p2",
                userId = "u2",
                title = "Garfield Park picnic",
                description = "Sunday picnic at Garfield Park, bring a dish to share",
                latitude = 39.7447,
                longitude = -86.1358,
                address = "Garfield Park, Indianapolis",
                startsAt = now + day,
                endsAt = now + day + 3 * hour,
                createdAt = now - hour,
                tags = listOf(PostTag.PICNIC),
                images = listOf("https://picsum.photos/seed/picnic1/800/600"),
                videos = emptyList(),
                rsvpCount = 7,
                author = User("u2", "Marcus T.", "https://i.pravatar.cc/200?img=12"),
                neighborhoodId = "fountain_square",
                groupId = null
            ),
            Post(
                id = "p3",
                userId = "u3",
                title = "White River group ride",
                description = "Group ride along White River, 15 miles at a chill pace",
                latitude = 39.8512,
                longitude = -86.1674,
                address = "White River State Park, Indianapolis",
                startsAt = now + 3 * hour,
                endsAt = null,
                createdAt = now - 45 * 60_000L,
                tags = listOf(PostTag.EXPLORE),
                images = listOf("https://picsum.photos/seed/bike1/800/600"),
                videos = emptyList(),
                rsvpCount = 2,
                author = User("u3", "Priya K.", "https://i.pravatar.cc/200?img=32"),
                neighborhoodId = "broad_ripple",
                groupId = "g1"
            ),
            Post(
                id = "p4",
                userId = "u4",
                title = "Pickup basketball",
                description = "Pickup basketball at Irving Circle Park, all skill levels",
                latitude = 39.7689,
                longitude = -86.0531,
                address = "Irving Circle Park, Irvington, Indianapolis",
                startsAt = now + 4 * hour,
                endsAt = now + 6 * hour,
                createdAt = now - 30 * 60_000L,
                tags = listOf(PostTag.SPORT),
                images = listOf("https://picsum.photos/seed/bball1/800/600"),
                videos = emptyList(),
                rsvpCount = 5,
                author = User("u4", "Jordan O.", "https://i.pravatar.cc/200?img=15"),
                neighborhoodId = "irvington",
                groupId = "g2"
            ),
            Post(
                id = "p5",
                userId = "u5",
                title = "Sunset yoga",
                description = "Sunset yoga at Holliday Park, bring a mat",
                latitude = 39.8723,
                longitude = -86.1612,
                address = "Holliday Park, Indianapolis",
                startsAt = now + 5 * hour,
                endsAt = now + 6 * hour,
                createdAt = now - 2 * hour,
                tags = listOf(PostTag.WALK),
                images = listOf("https://picsum.photos/seed/yoga1/800/600"),
                videos = emptyList(),
                rsvpCount = 9,
                author = User("u5", "Sam R.", "https://i.pravatar.cc/200?img=51"),
                neighborhoodId = "broad_ripple",
                groupId = null
            ),
            Post(
                id = "p6",
                userId = "u6",
                title = "Way Street long run",
                description = "Way Street Runners weekly long run — 8 miles this week",
                latitude = 39.8676,
                longitude = -86.1431,
                address = "Monon Trail at Broad Ripple Ave, Indianapolis",
                startsAt = now + day + 7 * hour,
                endsAt = null,
                createdAt = now - 3 * hour,
                tags = listOf(PostTag.RUN),
                images = listOf("https://picsum.photos/seed/run2/800/600"),
                videos = emptyList(),
                rsvpCount = 6,
                author = User("u6", "Lena H.", "https://i.pravatar.cc/200?img=44"),
                neighborhoodId = "broad_ripple",
                groupId = "g1"
            ),
            Post(
                id = "p7",
                userId = "u7",
                title = "Ellenberger dog walk",
                description = "Dog walk around Ellenberger Park, all breeds welcome",
                latitude = 39.7712,
                longitude = -86.0612,
                address = "Ellenberger Park, Irvington, Indianapolis",
                startsAt = now + 6 * hour,
                endsAt = null,
                createdAt = now - (1.5 * hour).toLong(),
                tags = listOf(PostTag.WALK),
                images = listOf("https://picsum.photos/seed/dogwalk1/800/600"),
                videos = emptyList(),
                rsvpCount = 4,
                author = User("u7", "Diego C.", "https://i.pravatar.cc/200?img=11"),
                neighborhoodId = "irvington",
                groupId = null
            )
        )
    }

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
        const val FEED_DELAY_MS = 400L
    }
}

@Suppress("unused")
private fun PostAudience.tagAudience(): String = when (this) {
    is PostAudience.Neighborhood -> "neighborhood"
    is PostAudience.GroupAudience -> "group:$groupId"
}
