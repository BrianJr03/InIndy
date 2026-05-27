package jr.brian.inindy.data.repository

import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.PostTag
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.ExploreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ExploreRepositoryImpl : ExploreRepository {

    override fun getPosts(): Flow<Result<List<Post>>> = flow {
        emit(Result.success(samplePosts))
    }

    private companion object {
        // May 27, 2026 00:00 UTC
        private const val NOW_MS = 1_779_840_000_000L

        val samplePosts = listOf(
            Post(
                id = "1",
                userId = "u1",
                title = "Morning hike at Eagle Creek",
                description = "Join us for a 5-mile loop through Eagle Creek Park. All skill levels welcome — bring water and snacks!",
                latitude = 39.8283,
                longitude = -86.2779,
                address = "Eagle Creek Park, Indianapolis",
                startsAt = NOW_MS + 86_400_000L + 9 * 3_600_000L,
                endsAt = NOW_MS + 86_400_000L + 12 * 3_600_000L,
                createdAt = NOW_MS - 3_600_000L,
                tags = listOf(PostTag.HIKE, PostTag.WALK),
                images = emptyList(),
                rsvpCount = 12,
                author = User("u1", "Sarah M.", null)
            ),
            Post(
                id = "2",
                userId = "u2",
                title = "Indy 5K — Monon Trail",
                description = "Casual Saturday run along the Monon Trail. We'll meet at the 10th Street trailhead and head north at an easy pace.",
                latitude = 39.7817,
                longitude = -86.1567,
                address = "Monon Trail @ 10th St, Indianapolis",
                startsAt = NOW_MS + 2 * 86_400_000L + 7 * 3_600_000L,
                endsAt = null,
                createdAt = NOW_MS - 7_200_000L,
                tags = listOf(PostTag.RUN),
                images = emptyList(),
                rsvpCount = 8,
                author = User("u2", "Marcus T.", null)
            ),
            Post(
                id = "3",
                userId = "u3",
                title = "Community Picnic — Garfield Park",
                description = "Bring a blanket and your favorite dish for a community potluck. Kids, dogs, and good vibes welcome!",
                latitude = 39.7365,
                longitude = -86.1425,
                address = "Garfield Park, Indianapolis",
                startsAt = NOW_MS + 3 * 86_400_000L + 12 * 3_600_000L,
                endsAt = NOW_MS + 3 * 86_400_000L + 17 * 3_600_000L,
                createdAt = NOW_MS - 1_800_000L,
                tags = listOf(PostTag.PICNIC, PostTag.EXPLORE),
                images = emptyList(),
                rsvpCount = 24,
                author = User("u3", "Priya K.", null)
            ),
            Post(
                id = "4",
                userId = "u4",
                title = "Pickup Basketball — Pan Am Plaza",
                description = "Open run every Sunday morning at Pan Am. Show up and ball. Mixed skill levels, all welcome.",
                latitude = 39.7691,
                longitude = -86.1599,
                address = "Pan Am Plaza, Indianapolis",
                startsAt = NOW_MS + 4 * 86_400_000L + 8 * 3_600_000L,
                endsAt = null,
                createdAt = NOW_MS - 900_000L,
                tags = listOf(PostTag.SPORT),
                images = emptyList(),
                rsvpCount = 6,
                author = User("u4", "DeShawn W.", null)
            ),
            Post(
                id = "5",
                userId = "u5",
                title = "Neighborhood Walk — Broad Ripple",
                description = "Leisurely walk through Broad Ripple village. We'll stop for coffee and check out local murals. About 3 miles total.",
                latitude = 39.8672,
                longitude = -86.1414,
                address = "Broad Ripple Ave, Indianapolis",
                startsAt = NOW_MS + 5 * 86_400_000L + 10 * 3_600_000L,
                endsAt = NOW_MS + 5 * 86_400_000L + 13 * 3_600_000L,
                createdAt = NOW_MS - 5_400_000L,
                tags = listOf(PostTag.WALK, PostTag.EXPLORE),
                images = emptyList(),
                rsvpCount = 15,
                author = User("u5", "Jordan L.", null)
            )
        )
    }
}
