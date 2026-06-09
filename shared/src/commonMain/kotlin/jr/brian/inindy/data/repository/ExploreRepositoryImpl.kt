package jr.brian.inindy.data.repository

import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.model.VideoMedia
import jr.brian.inindy.domain.repository.ExploreRepository
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class ExploreRepositoryImpl : ExploreRepository {

    private val rsvpdPostIds = mutableSetOf<String>()
    private val postsState = MutableStateFlow(buildInitialPosts())

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
        val pool = attendeePool
        val seed = postId.hashCode()
        val rotated = pool.indices.map { i -> pool[((i + seed) % pool.size + pool.size) % pool.size] }
        val list = rotated.take(rsvpCount.coerceAtMost(pool.size))
        return Result.success(list)
    }

    private fun defaultAttendeeCount(postId: String): Int =
        (postId.hashCode().let { if (it < 0) -it else it } % 8) + 2

    private fun buildInitialPosts(): List<Post> {
        val fifteenMinutesAgo = currentTimeMillis() - 15 * 60_000L
        return samplePosts.mapIndexed { index, post ->
            if (index == 0) post.copy(createdAt = fifteenMinutesAgo) else post
        }
    }

    private companion object {
        // May 27, 2026 00:00 UTC
        private const val NOW_MS = 1_779_840_000_000L

        val attendeePool = listOf(
            User("a1", "Michelle W.", "https://i.pravatar.cc/200?img=47"),
            User("a2", "Marcus T.", "https://i.pravatar.cc/200?img=12"),
            User("a3", "Priya K.", "https://i.pravatar.cc/200?img=32"),
            User("a4", "Jordan O.", "https://i.pravatar.cc/200?img=15"),
            User("a5", "Sam R.", "https://i.pravatar.cc/200?img=51"),
            User("a6", "Lena H.", "https://i.pravatar.cc/200?img=44"),
            User("a7", "Diego C.", "https://i.pravatar.cc/200?img=11"),
            User("a8", "Maya P.", "https://i.pravatar.cc/200?img=20"),
            User("a9", "Theo B.", "https://i.pravatar.cc/200?img=33"),
            User("a10", "Ash N.", null),
            User("a11", "Riley J.", "https://i.pravatar.cc/200?img=26"),
            User("a12", "Casey D.", null)
        )

        val samplePosts = listOf(
            Post(
                id = "1",
                userId = "u1",
                title = "Morning hike at Eagle Creek",
                description = "Join us for a 5-mile loop through Eagle Creek Park. All skill levels welcome. Bring water and snacks!",
                latitude = 39.8283,
                longitude = -86.2779,
                address = "Eagle Creek Park, Indianapolis",
                startsAt = NOW_MS + 86_400_000L + 9 * 3_600_000L,
                endsAt = NOW_MS + 86_400_000L + 12 * 3_600_000L,
                createdAt = NOW_MS - 3_600_000L,
                tags = listOf(Interest.HIKING, Interest.WALKING),
                images = listOf(
                    "https://scontent-lhr8-2.xx.fbcdn.net/v/t39.30808-6/707981420_10107786398053153_8410068118367704544_n.jpg?stp=cp6_dst-jpegr_tt6&_nc_cat=101&ccb=1-7&_nc_sid=aa7b47&_nc_ohc=y-bslXyy1cEQ7kNvwFNjk03&_nc_oc=AdobgX-R3Z-8HanTE6DmeK2H2LaZQwhiq7c2rn4hAm9tnlHZ5hN2P0LWh6c5tInYA_U&_nc_zt=23&se=-1&_nc_ht=scontent-lhr8-2.xx&_nc_gid=CZNz2T0fU6XDFq5YftLrYA&_nc_ss=7b2a8&oh=00_Af6m8fNv-xMttAXiTA_hml41yBQ6pOOz0ueMee24YFfBtA&oe=6A1CF5E0",
                    "https://scontent-lhr8-2.xx.fbcdn.net/v/t39.30808-6/707406804_10107786395912443_7289282741261028850_n.jpg?_nc_cat=103&ccb=1-7&_nc_sid=aa7b47&_nc_ohc=ow66hQadPugQ7kNvwHL2ZFU&_nc_oc=AdoGoKt7icnzPjiUC3-BEal_WSyH308Zkv3pMni4Y1leK4cx_9XQqnOgVoC47H1E1C8&_nc_zt=23&_nc_ht=scontent-lhr8-2.xx&_nc_gid=ns86D6HTF8xagHJWeJTF6g&_nc_ss=7b2a8&oh=00_Af4-yKe9PPlfbRQq37gw-clH4VBd6oH8agXnxKqUIQE3-g&oe=6A1CF9D1",
                    "https://scontent-lhr6-1.xx.fbcdn.net/v/t39.30808-6/707188305_10107786412060083_7223946904420155065_n.jpg?stp=cp6_dst-jpegr_tt6&_nc_cat=110&ccb=1-7&_nc_sid=aa7b47&_nc_ohc=oRiMqJVgfrMQ7kNvwFSMfuE&_nc_oc=AdrKv4kPRRDFV1E3SQ1kf6pmdI5XTw-LEW4oNdG0YmRMLBPEyMLhTtffXipTRhm4Q_g&_nc_zt=23&se=-1&_nc_ht=scontent-lhr6-1.xx&_nc_gid=bDARivlp_CrNula5vr-_0w&_nc_ss=7b2a8&oh=00_Af778BS2bHn38KHOLnf8k_wPr6bgPFJctU570MNbbYCVwA&oe=6A1CD4F1"
                ),
                videos = emptyList(),
                rsvpCount = 12,
                author = User("u1", "Michelle W.", "https://scontent-lhr8-1.cdninstagram.com/v/t51.82787-19/522715287_18510421714020632_8147388195996693606_n.jpg?efg=eyJ2ZW5jb2RlX3RhZyI6InByb2ZpbGVfcGljLmRqYW5nby4xMDU2LmMyIn0&_nc_ht=scontent-lhr8-1.cdninstagram.com&_nc_cat=108&_nc_oc=Q6cZ2gHwqiU1pqp7w4C7ZUW644dmOUyF_VrYvB83c03Av56BnbOJuX-65JPi5f_yiRnt_3Y&_nc_ohc=eK5SHrYgn0cQ7kNvwGwz-kK&_nc_gid=pSn6TDr-Nzy5UVWrdHiJUA&edm=AA5fTDYBAAAA&ccb=7-5&oh=00_Af4peqSBIbGyZW4MqaJaBFuxbBwjqGZKsxFv4qEpeEq-1w&oe=6A1D1DAD&_nc_sid=7edfe2")
            ),
            Post(
                id = "2",
                userId = "u2",
                title = "Indy 5K @ Monon Trail",
                description = "Casual Saturday run along the Monon Trail. We'll meet at the 10th Street trailhead and head north at an easy pace.",
                latitude = 39.7817,
                longitude = -86.1567,
                address = "Monon Trail @ 10th St, Indianapolis",
                startsAt = NOW_MS + 2 * 86_400_000L + 7 * 3_600_000L,
                endsAt = null,
                createdAt = NOW_MS - 7_200_000L,
                tags = listOf(Interest.RUNNING),
                images = listOf(
                    "https://www.visitindy.com/imager/files_idss_com/C516/DMS_image_3410_e7b4e5d5-5056-854c-b6c0e14aadaa42c5_e45adf5f6bc0c5c2a30a39868f44eab6.jpg",
                    "https://www.railstotrails.org/nitropack_static/pVKvLDLqSrRUaEyiNwEcSJukRyhzZaDI/assets/images/optimized/rev-958f862/www.railstotrails.org/wp-content/uploads/2024/12/Indianas-Monon-Trail_IMG_8344_Photo-by-Robert-Annis.jpg"
                ),
                videos = emptyList(),
                rsvpCount = 8,
                author = User("u2", "Marcus T.", "https://www.adobe.com/creativecloud/photography/discover/media_131179edca5f92db203e2b78cb8a308605afbc958.png?width=750&format=png&optimize=medium")
            ),
            Post(
                id = "3",
                userId = "u3",
                title = "Last Second Picnic!",
                description = "We're throwing a picnic. Kids, dogs, and good vibes welcome!",
                latitude = 39.7365,
                longitude = -86.1425,
                address = "Garfield Park, Indianapolis",
                startsAt = NOW_MS + 3 * 86_400_000L + 12 * 3_600_000L,
                endsAt = NOW_MS + 3 * 86_400_000L + 17 * 3_600_000L,
                createdAt = NOW_MS - 1_800_000L,
                tags = listOf(Interest.PICNICS, Interest.EXPLORING),
                images = listOf(
                    "https://www.blackfoodie.co/wp-content/uploads/2020/08/Copy-of-random-for-reference.png",
                    "https://static01.nyt.com/images/2022/07/20/t-magazine/20tmag-mayfield-slide-RNIG/20tmag-mayfield-slide-RNIG-articleLarge.jpg?quality=75&auto=webp&disable=upscale",

                ),
                videos = emptyList(),
                rsvpCount = 24,
                author = User("u3", "Priya K.", "https://qodeinteractive.com/magazine/wp-content/uploads/2019/08/Featured-Stock-1240x623.jpg")
            ),
            Post(
                id = "4",
                userId = "u4",
                title = "Electric Skateboarding",
                description = "I'll be hitting the trails today! B&O trail is next!",
                latitude = 39.7691,
                longitude = -86.1599,
                address = "B&O Trail on 71st",
                startsAt = NOW_MS + 4 * 86_400_000L + 8 * 3_600_000L,
                endsAt = null,
                createdAt = NOW_MS - 90_000L,
                tags = listOf(Interest.SPORTS),
                images = listOf(
                    "https://www.ridepropel.com/wp-content/uploads/2023/08/Remoteless-Electric-Skateboard1.jpg"
                ),
                videos = listOf(
                    VideoMedia(
                        url = "https://packaged-media.redd.it/nrnyhmszq0tg1/pb/m2-res_1080p.mp4?m=DASHPlaylist.mpd&var=sgpssan&v=1&e=1779930000&s=cad89802891024b675418b67014c8befac3a097d",
                        thumbnailUrl = "https://preview.redd.it/gaming-in-a-pinch-v0-vklpmssa1l2h1.jpeg?width=1080&crop=smart&auto=webp&s=a8bb942074243a5a62f3f0762b20b677f9899b3f"
                    )
                ),
                rsvpCount = 2,
                author = User("u4", "Brian", "https://scontent-lhr6-2.cdninstagram.com/v/t51.2885-19/431616479_700615628948538_2034760349014514351_n.jpg?_nc_cat=105&ccb=7-5&_nc_sid=bf7eb4&efg=eyJ2ZW5jb2RlX3RhZyI6InByb2ZpbGVfcGljLnd3dy45NjAuQzMifQ%3D%3D&_nc_ohc=JTPUx10jMygQ7kNvwHUrfeH&_nc_oc=AdoWNV3YfvvmX00aCYkXrZK050gZH8ddpm2USKakb1TurGEYy2Z44NidXT3Ujx-QsFY&_nc_zt=24&_nc_ht=scontent-lhr6-2.cdninstagram.com&_nc_ss=7b6a8&oh=00_Af4b9zkCxbX1anrHpwmLFqY1Cy7IqOm7NW1ZrqEOXzHciQ&oe=6A1D3B42")
            ),
            Post(
                id = "5",
                userId = "u5",
                title = "Wine and Walk",
                description = "Drunkenly walk through Broad Ripple village. About 3 miles total.",
                latitude = 39.8672,
                longitude = -86.1414,
                address = "Broad Ripple Ave, Indianapolis",
                startsAt = NOW_MS + 5 * 86_400_000L + 10 * 3_600_000L,
                endsAt = NOW_MS + 5 * 86_400_000L + 13 * 3_600_000L,
                createdAt = NOW_MS - 5_400_000L,
                tags = listOf(Interest.WALKING, Interest.EXPLORING),
                images = listOf(
                    "https://wineandwalk.hr/wp-content/uploads/2024/09/wine_walk-00636.jpg"
                ),
                videos = emptyList(),
                rsvpCount = 15,
                author = User("u5", "Jordan O.", null)
            )
        )
    }
}
