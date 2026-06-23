package jr.brian.inindy.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import jr.brian.inindy.data.remote.post.PostDto
import jr.brian.inindy.data.remote.post.PostImageDto
import jr.brian.inindy.data.remote.post.PostTagDto
import jr.brian.inindy.data.remote.post.RsvpWithUserDto
import jr.brian.inindy.data.remote.post.toDomain
import jr.brian.inindy.data.remote.post.toDto
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.CreatePostRequest
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.PostRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@OptIn(ExperimentalUuidApi::class)
class SupabasePostRepository(
    private val supabase: SupabaseClient,
    private val currentUserProvider: CurrentUserProvider
) : PostRepository {

    private val sharedScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sharedFlows = mutableMapOf<String, SharedFlow<List<Post>>>()
    private val sharedFlowsMutex = Mutex()

    override fun observeUserPosts(): Flow<List<Post>> = flow {
        val userId = currentUserProvider.get().userId
            ?: run {
                println("[InIndy] observeUserPosts — no signed-in user, emitting empty list")
                emit(emptyList())
                return@flow
            }
        emitAll(sharedUserPostsFlow(userId))
    }

    private suspend fun sharedUserPostsFlow(userId: String): SharedFlow<List<Post>> =
        sharedFlowsMutex.withLock {
            sharedFlows.getOrPut(userId) {
                buildUserPostsFlow(userId).shareIn(
                    scope = sharedScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    replay = 1
                )
            }
        }

    // IMPORTANT: posts table must have Realtime enabled in Supabase dashboard
    // Database → Replication → enable posts
    // Without this the flow never updates and the UI only refreshes on app restart
    private fun buildUserPostsFlow(userId: String): Flow<List<Post>> = channelFlow {
        println("[InIndy] observeUserPosts — subscribing for userId: $userId")

        suspend fun emitLatest() {
            val result = fetchUserPosts(userId)
            println("[InIndy] observeUserPosts — emitting ${result.getOrElse { emptyList() }.size} posts")
            send(result.getOrElse { emptyList() })
        }

        emitLatest()

        val channel = supabase.channel("posts-user-$userId-${Uuid.random()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = POSTS_TABLE
            filter("user_id", FilterOperator.EQ, userId)
        }
        launch {
            changes.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        println("[InIndy] observeUserPosts — INSERT, re-fetching")
                        emitLatest()
                    }
                    is PostgresAction.Update -> {
                        println("[InIndy] observeUserPosts — UPDATE, re-fetching")
                        emitLatest()
                    }
                    is PostgresAction.Delete -> {
                        println("[InIndy] observeUserPosts — DELETE, re-fetching")
                        emitLatest()
                    }
                    else -> {}
                }
            }
        }
        channel.subscribe()

        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                supabase.realtime.removeChannel(channel)
            }
        }
    }

    override suspend fun getUserPosts(): Result<List<Post>> = runCatching {
        val userId = currentUserProvider.get().userId
            ?: error("No signed-in user")
        println("[InIndy] getUserPosts — fetching for userId: $userId")
        val posts = fetchUserPosts(userId).getOrThrow()
        println("[InIndy] getUserPosts — fetched ${posts.size} posts")
        posts
    }

    override suspend fun getPostById(postId: String): Result<Post> = runCatching {
        println("[InIndy] getPostById — postId: $postId")
        val post = supabase.from(POSTS_TABLE).select(JOINED_COLUMNS) {
            filter { eq("id", postId) }
            order("created_at", order = Order.ASCENDING, referencedTable = RSVPS_TABLE)
            limit(count = ATTENDEE_PREVIEW_LIMIT, referencedTable = RSVPS_TABLE)
        }.decodeSingle<PostDto>().toDomain()
        println("[InIndy] getPostById — found post: ${post.id} with ${post.images.size} images")
        post
    }

    override suspend fun createPost(request: CreatePostRequest): Result<Post> = runCatching {
        val prefs = currentUserProvider.get()
        val userId = prefs.userId ?: error("No signed-in user")
        val neighborhoodId = prefs.neighborhoodId ?: error("No neighborhood selected")

        println("[InIndy] createPost START — userId: $userId, neighborhoodId: $neighborhoodId")
        println("[InIndy] createPost — description: ${request.description.take(50)}")
        println("[InIndy] createPost — images: ${request.imageUris.size}, tags: ${request.tags.size}")

        val cdnUrls = request.imageUris

        // ── Insert post ──────────────────────────────────────────────────
        val dto = request.toDto(userId = userId, neighborhoodId = neighborhoodId)
        println("[InIndy] createPost — inserting post row into Supabase")

        val inserted = supabase.from(POSTS_TABLE)
            .insert(dto) { select(Columns.list("id")) }
            .decodeSingle<InsertedPostId>()

        println("[InIndy] createPost — post inserted with id: ${inserted.id}")

        // ── Insert post_images ───────────────────────────────────────────
        if (cdnUrls.isNotEmpty()) {
            val imageRows = cdnUrls.mapIndexed { index, url ->
                PostImageDto(postId = inserted.id, storageUrl = url, sortOrder = index)
            }
            println("[InIndy] createPost — inserting ${imageRows.size} post_images rows")
            supabase.from(POST_IMAGES_TABLE).insert(imageRows)
            println("[InIndy] createPost — post_images inserted for post ${inserted.id}")
        } else {
            println("[InIndy] createPost — no images to insert")
        }

        // ── Insert post_tags ─────────────────────────────────────────────
        if (request.tags.isNotEmpty()) {
            val tagRows = request.tags.map { interest ->
                PostTagDto(postId = inserted.id, tag = interest.name)
            }
            println("[InIndy] createPost — inserting ${tagRows.size} post_tags rows: ${request.tags.map { it.name }}")
            supabase.from(POST_TAGS_TABLE).insert(tagRows)
            println("[InIndy] createPost — post_tags inserted for post ${inserted.id}")
        } else {
            println("[InIndy] createPost — no tags to insert")
        }

        // ── Re-fetch with joins ──────────────────────────────────────────
        println("[InIndy] createPost — re-fetching post with joins")
        val finalPost = supabase.from(POSTS_TABLE).select(JOINED_COLUMNS) {
            filter { eq("id", inserted.id) }
            order("created_at", order = Order.ASCENDING, referencedTable = RSVPS_TABLE)
            limit(count = ATTENDEE_PREVIEW_LIMIT, referencedTable = RSVPS_TABLE)
        }.decodeSingle<PostDto>().toDomain()

        println("[InIndy] createPost COMPLETE — id: ${finalPost.id}, images: ${finalPost.images.size}, tags: ${finalPost.tags.size}")
        finalPost
    }.onFailure { e ->
        println("[InIndy] createPost FAILED: ${e::class.simpleName}: ${e.message}")
        e.printStackTrace()
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        println("[InIndy] deletePost — postId: $postId")
        supabase.from(POSTS_TABLE).delete {
            filter { eq("id", postId) }
        }
        println("[InIndy] deletePost — success for postId: $postId")
    }

    // ── Realtime feed observers ──────────────────────────────────────────────
    // These flows emit the latest feed on subscribe and re-emit whenever the
    // posts table changes for the matching scope. The rsvp_count column lives
    // on posts, so any RSVP from any device triggers an UPDATE here and the
    // feed re-fetch picks up the new count automatically.
    //
    // NOTE: requires Supabase Realtime to be enabled for the `posts` table.
    // Supabase dashboard → Database → Replication → enable `posts`. Without it
    // the postgresChangeFlow never fires and counts won't sync across devices.

    override fun observeNeighborhoodOnlyFeed(neighborhoodId: String): Flow<Result<List<Post>>> =
        channelFlow {
            println("[InIndy] observeNeighborhoodOnlyFeed — subscribing for neighborhoodId: $neighborhoodId")

            suspend fun emitLatest() {
                send(getNeighborhoodOnlyFeed(neighborhoodId))
            }

            emitLatest()

            val channel = supabase.channel("posts-nh-$neighborhoodId-${Uuid.random()}")
            val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = POSTS_TABLE
                filter("neighborhood_id", FilterOperator.EQ, neighborhoodId)
            }
            launch {
                changes.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            println("[InIndy] observeNeighborhoodOnlyFeed — INSERT, re-fetching")
                            emitLatest()
                        }
                        is PostgresAction.Update -> {
                            println("[InIndy] observeNeighborhoodOnlyFeed — UPDATE, re-fetching")
                            emitLatest()
                        }
                        is PostgresAction.Delete -> {
                            println("[InIndy] observeNeighborhoodOnlyFeed — DELETE, re-fetching")
                            emitLatest()
                        }
                        else -> {}
                    }
                }
            }
            channel.subscribe()

            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    supabase.realtime.removeChannel(channel)
                }
            }
        }

    override fun observeGroupFeed(groupId: String): Flow<Result<List<Post>>> = channelFlow {
        println("[InIndy] observeGroupFeed — subscribing for groupId: $groupId")

        suspend fun emitLatest() {
            send(getGroupFeed(groupId))
        }

        emitLatest()

        val channel = supabase.channel("posts-group-$groupId-${Uuid.random()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = POSTS_TABLE
            filter("group_id", FilterOperator.EQ, groupId)
        }
        launch {
            changes.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        println("[InIndy] observeGroupFeed — INSERT, re-fetching")
                        emitLatest()
                    }
                    is PostgresAction.Update -> {
                        println("[InIndy] observeGroupFeed — UPDATE, re-fetching")
                        emitLatest()
                    }
                    is PostgresAction.Delete -> {
                        println("[InIndy] observeGroupFeed — DELETE, re-fetching")
                        emitLatest()
                    }
                    else -> {}
                }
            }
        }
        channel.subscribe()

        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                supabase.realtime.removeChannel(channel)
            }
        }
    }

    override suspend fun getNeighborhoodFeed(neighborhoodId: String): Result<List<Post>> =
        runCatching {
            println("[InIndy] getNeighborhoodFeed — neighborhoodId: $neighborhoodId")
            val posts = supabase.from(POSTS_TABLE).select(JOINED_COLUMNS) {
                filter { eq("neighborhood_id", neighborhoodId) }
                order("created_at", order = Order.DESCENDING)
                limit(FEED_LIMIT)
                order("created_at", order = Order.ASCENDING, referencedTable = RSVPS_TABLE)
                limit(count = ATTENDEE_PREVIEW_LIMIT, referencedTable = RSVPS_TABLE)
            }.decodeList<PostDto>().map { it.toDomain() }
            println("[InIndy] getNeighborhoodFeed — loaded ${posts.size} posts")
            posts
        }

    override suspend fun getNeighborhoodOnlyFeed(neighborhoodId: String): Result<List<Post>> =
        runCatching {
            println("[InIndy] getNeighborhoodOnlyFeed — neighborhoodId: $neighborhoodId")
            val posts = supabase.from(POSTS_TABLE).select(JOINED_COLUMNS) {
                filter {
                    eq("neighborhood_id", neighborhoodId)
                    filter("group_id", FilterOperator.IS, null)
                }
                order("created_at", order = Order.DESCENDING)
                limit(FEED_LIMIT)
                order("created_at", order = Order.ASCENDING, referencedTable = RSVPS_TABLE)
                limit(count = ATTENDEE_PREVIEW_LIMIT, referencedTable = RSVPS_TABLE)
            }.decodeList<PostDto>().map { it.toDomain() }
            println("[InIndy] getNeighborhoodOnlyFeed — loaded ${posts.size} posts")
            posts
        }

    override suspend fun getGroupFeed(groupId: String): Result<List<Post>> = runCatching {
        println("[InIndy] getGroupFeed — groupId: $groupId")
        val posts = supabase.from(POSTS_TABLE).select(JOINED_COLUMNS) {
            filter { eq("group_id", groupId) }
            order("created_at", order = Order.DESCENDING)
            order("created_at", order = Order.ASCENDING, referencedTable = RSVPS_TABLE)
            limit(count = ATTENDEE_PREVIEW_LIMIT, referencedTable = RSVPS_TABLE)
        }.decodeList<PostDto>().map { it.toDomain() }
        println("[InIndy] getGroupFeed — loaded ${posts.size} posts")
        posts
    }

    override suspend fun getPostAttendees(postId: String): Result<List<User>> = runCatching {
        println("[InIndy] getPostAttendees — postId: $postId")
        val attendees = supabase.from(RSVPS_TABLE)
            .select(ATTENDEES_COLUMNS) {
                filter {
                    eq("post_id", postId)
                    eq("status", "confirmed")
                }
                order("created_at", order = Order.ASCENDING)
            }
            .decodeList<RsvpWithUserDto>()
            .map { it.user.toDomain() }
        println("[InIndy] getPostAttendees — loaded ${attendees.size} attendees")
        attendees
    }.onFailure { e ->
        println("[InIndy] getPostAttendees FAILED — postId: $postId, error: ${e::class.simpleName}: ${e.message}")
    }

    private suspend fun fetchUserPosts(userId: String): Result<List<Post>> = runCatching {
        supabase.from(POSTS_TABLE).select(JOINED_COLUMNS) {
            filter { eq("user_id", userId) }
            order("created_at", order = Order.DESCENDING)
            order("created_at", order = Order.ASCENDING, referencedTable = RSVPS_TABLE)
            limit(count = ATTENDEE_PREVIEW_LIMIT, referencedTable = RSVPS_TABLE)
        }.decodeList<PostDto>().map { it.toDomain() }
    }

    @Serializable
    private data class InsertedPostId(val id: String)

    private companion object {
        const val POSTS_TABLE = "posts"
        const val POST_IMAGES_TABLE = "post_images"
        const val POST_TAGS_TABLE = "post_tags"
        const val RSVPS_TABLE = "rsvps"
        const val FEED_LIMIT = 50L
        const val STOP_TIMEOUT_MS = 5_000L
        const val ATTENDEE_PREVIEW_LIMIT = 5L
        val JOINED_COLUMNS = Columns.raw(
            "*, author:users(id, full_name, avatar_url), neighborhood:neighborhoods(name), images:post_images(*), tags:post_tags(*), rsvps(user_id, user:users(id, full_name, avatar_url))"
        )
        val ATTENDEES_COLUMNS = Columns.raw(
            "user_id, user:users(id, full_name, avatar_url)"
        )
    }
}