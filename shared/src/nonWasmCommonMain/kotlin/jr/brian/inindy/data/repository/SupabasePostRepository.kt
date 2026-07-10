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
import jr.brian.inindy.data.remote.post.toUpdateDto
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.CreatePostRequest
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.PostDeletedException
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.util.appLog
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
    private val log = appLog("SupabasePostRepository")

    private val sharedScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sharedFlows = mutableMapOf<String, SharedFlow<List<Post>>>()
    private val sharedFlowsMutex = Mutex()

    override fun observeUserPosts(): Flow<List<Post>> = flow {
        val userId = currentUserProvider.get().userId
            ?: run {
                log.d { "observeUserPosts — no signed-in user, emitting empty list" }
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
        log.d { "observeUserPosts — subscribing for userId: $userId" }

        suspend fun emitLatest() {
            val result = fetchUserPosts(userId)
            log.d { "observeUserPosts — emitting ${result.getOrElse { emptyList() }.size} posts" }
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
                        log.d { "observeUserPosts — INSERT, re-fetching" }
                        emitLatest()
                    }
                    is PostgresAction.Update -> {
                        log.d { "observeUserPosts — UPDATE, re-fetching" }
                        emitLatest()
                    }
                    is PostgresAction.Delete -> {
                        log.d { "observeUserPosts — DELETE, re-fetching" }
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
        log.d { "getUserPosts — fetching for userId: $userId" }
        val posts = fetchUserPosts(userId).getOrThrow()
        log.d { "getUserPosts — fetched ${posts.size} posts" }
        posts
    }

    override suspend fun getPostById(postId: String): Result<Post> = runCatching {
        log.d { "getPostById — postId: $postId" }
        val post = supabase.from(POSTS_TABLE).select(JOINED_COLUMNS) {
            filter { eq("id", postId) }
            order("created_at", order = Order.ASCENDING, referencedTable = RSVPS_TABLE)
            limit(count = ATTENDEE_PREVIEW_LIMIT, referencedTable = RSVPS_TABLE)
        }.decodeSingle<PostDto>().toDomain()
        log.d { "getPostById — found post: ${post.id} with ${post.images.size} images" }
        post
    }

    override suspend fun createPost(request: CreatePostRequest): Result<Post> = runCatching {
        val prefs = currentUserProvider.get()
        val userId = prefs.userId ?: error("No signed-in user")
        val neighborhoodId = prefs.neighborhoodId ?: error("No neighborhood selected")

        log.i { "createPost START — userId: $userId, neighborhoodId: $neighborhoodId" }
        log.d { "createPost — description: ${request.description.take(50)}" }
        log.d { "createPost — images: ${request.imageUris.size}, tags: ${request.tags.size}" }

        val cdnUrls = request.imageUris

        // ── Insert post ──────────────────────────────────────────────────
        val dto = request.toDto(userId = userId, neighborhoodId = neighborhoodId)
        log.d { "createPost — inserting post row into Supabase" }

        val inserted = supabase.from(POSTS_TABLE)
            .insert(dto) { select(Columns.list("id")) }
            .decodeSingle<InsertedPostId>()

        log.i { "createPost — post inserted with id: ${inserted.id}" }

        // ── Insert post_images ───────────────────────────────────────────
        if (cdnUrls.isNotEmpty()) {
            val imageRows = cdnUrls.mapIndexed { index, url ->
                PostImageDto(postId = inserted.id, storageUrl = url, sortOrder = index)
            }
            log.d { "createPost — inserting ${imageRows.size} post_images rows" }
            supabase.from(POST_IMAGES_TABLE).insert(imageRows)
            log.d { "createPost — post_images inserted for post ${inserted.id}" }
        } else {
            log.d { "createPost — no images to insert" }
        }

        // ── Insert post_tags ─────────────────────────────────────────────
        if (request.tags.isNotEmpty()) {
            val tagRows = request.tags.map { interest ->
                PostTagDto(postId = inserted.id, tag = interest.name)
            }
            log.d { "createPost — inserting ${tagRows.size} post_tags rows: ${request.tags.map { it.name }}" }
            supabase.from(POST_TAGS_TABLE).insert(tagRows)
            log.d { "createPost — post_tags inserted for post ${inserted.id}" }
        } else {
            log.d { "createPost — no tags to insert" }
        }

        // ── Re-fetch with joins ──────────────────────────────────────────
        log.d { "createPost — re-fetching post with joins" }
        val finalPost = supabase.from(POSTS_TABLE).select(JOINED_COLUMNS) {
            filter { eq("id", inserted.id) }
            order("created_at", order = Order.ASCENDING, referencedTable = RSVPS_TABLE)
            limit(count = ATTENDEE_PREVIEW_LIMIT, referencedTable = RSVPS_TABLE)
        }.decodeSingle<PostDto>().toDomain()

        log.i { "createPost COMPLETE — id: ${finalPost.id}, images: ${finalPost.images.size}, tags: ${finalPost.tags.size}" }
        finalPost
    }.onFailure { e ->
        log.e(e) { "createPost FAILED" }
    }

    override suspend fun updatePost(
        postId: String,
        request: CreatePostRequest
    ): Result<Post> = runCatching {
        log.i { "updatePost START — postId: $postId" }
        log.d { "updatePost — description: ${request.description.take(50)}" }
        log.d { "updatePost — images: ${request.imageUris.size}, tags: ${request.tags.size}" }

        val cdnUrls = request.imageUris

        // ── Update posts row ────────────────────────────────────────────
        val updateDto = request.toUpdateDto()
        log.d { "updatePost — updating posts row" }
        supabase.from(POSTS_TABLE).update(updateDto) {
            filter { eq("id", postId) }
        }
        log.d { "updatePost — posts row updated" }

        // ── Reconcile post_images: delete-then-insert ───────────────────
        // Max 3 images per post, so wiping and re-inserting is simpler and
        // correct compared to diffing. Fails here bubble up via runCatching;
        // if RLS blocks the DELETE the error will name post_images and the
        // ViewModel surfaces submitError to the user.
        log.d { "updatePost — clearing existing post_images" }
        supabase.from(POST_IMAGES_TABLE).delete {
            filter { eq("post_id", postId) }
        }
        if (cdnUrls.isNotEmpty()) {
            val imageRows = cdnUrls.mapIndexed { index, url ->
                PostImageDto(postId = postId, storageUrl = url, sortOrder = index)
            }
            log.d { "updatePost — inserting ${imageRows.size} post_images rows" }
            supabase.from(POST_IMAGES_TABLE).insert(imageRows)
        }

        // ── Reconcile post_tags: delete-then-insert ─────────────────────
        log.d { "updatePost — clearing existing post_tags" }
        supabase.from(POST_TAGS_TABLE).delete {
            filter { eq("post_id", postId) }
        }
        if (request.tags.isNotEmpty()) {
            val tagRows = request.tags.map { interest ->
                PostTagDto(postId = postId, tag = interest.name)
            }
            log.d { "updatePost — inserting ${tagRows.size} post_tags rows: ${request.tags.map { it.name }}" }
            supabase.from(POST_TAGS_TABLE).insert(tagRows)
        }

        // ── Re-fetch with joins ─────────────────────────────────────────
        log.d { "updatePost — re-fetching post with joins" }
        val finalPost = supabase.from(POSTS_TABLE).select(JOINED_COLUMNS) {
            filter { eq("id", postId) }
            order("created_at", order = Order.ASCENDING, referencedTable = RSVPS_TABLE)
            limit(count = ATTENDEE_PREVIEW_LIMIT, referencedTable = RSVPS_TABLE)
        }.decodeSingle<PostDto>().toDomain()

        log.i { "updatePost COMPLETE — id: ${finalPost.id}, images: ${finalPost.images.size}, tags: ${finalPost.tags.size}" }
        finalPost
    }.onFailure { e ->
        log.e(e) { "updatePost FAILED" }
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        log.d { "deletePost — postId: $postId" }
        supabase.from(POSTS_TABLE).delete {
            filter { eq("id", postId) }
        }
        log.i { "deletePost — success for postId: $postId" }
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
            log.d { "observeNeighborhoodOnlyFeed — subscribing for neighborhoodId: $neighborhoodId" }

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
                            log.d { "observeNeighborhoodOnlyFeed — INSERT, re-fetching" }
                            emitLatest()
                        }
                        is PostgresAction.Update -> {
                            log.d { "observeNeighborhoodOnlyFeed — UPDATE, re-fetching" }
                            emitLatest()
                        }
                        is PostgresAction.Delete -> {
                            log.d { "observeNeighborhoodOnlyFeed — DELETE, re-fetching" }
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

    override fun observePost(postId: String): Flow<Result<Post>> = channelFlow {
        log.d { "observePost — subscribing for postId: $postId" }

        suspend fun emitLatest() {
            send(getPostById(postId))
        }

        emitLatest()

        val channel = supabase.channel("posts-detail-$postId-${Uuid.random()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = POSTS_TABLE
            filter("id", FilterOperator.EQ, postId)
        }
        launch {
            changes.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        log.d { "observePost — INSERT, re-fetching" }
                        emitLatest()
                    }
                    is PostgresAction.Update -> {
                        log.d { "observePost — UPDATE, re-fetching" }
                        emitLatest()
                    }
                    is PostgresAction.Delete -> {
                        log.d { "observePost — DELETE, emitting failure" }
                        send(Result.failure(PostDeletedException()))
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
        log.d { "observeGroupFeed — subscribing for groupId: $groupId" }

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
                        log.d { "observeGroupFeed — INSERT, re-fetching" }
                        emitLatest()
                    }
                    is PostgresAction.Update -> {
                        log.d { "observeGroupFeed — UPDATE, re-fetching" }
                        emitLatest()
                    }
                    is PostgresAction.Delete -> {
                        log.d { "observeGroupFeed — DELETE, re-fetching" }
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
            log.d { "getNeighborhoodFeed — neighborhoodId: $neighborhoodId" }
            val posts = supabase.from(POSTS_TABLE).select(JOINED_COLUMNS) {
                filter { eq("neighborhood_id", neighborhoodId) }
                order("created_at", order = Order.DESCENDING)
                limit(FEED_LIMIT)
                order("created_at", order = Order.ASCENDING, referencedTable = RSVPS_TABLE)
                limit(count = ATTENDEE_PREVIEW_LIMIT, referencedTable = RSVPS_TABLE)
            }.decodeList<PostDto>().map { it.toDomain() }
            log.d { "getNeighborhoodFeed — loaded ${posts.size} posts" }
            posts
        }

    override suspend fun getNeighborhoodOnlyFeed(neighborhoodId: String): Result<List<Post>> =
        runCatching {
            log.d { "getNeighborhoodOnlyFeed — neighborhoodId: $neighborhoodId" }
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
            log.d { "getNeighborhoodOnlyFeed — loaded ${posts.size} posts" }
            posts
        }

    override suspend fun getGroupFeed(groupId: String): Result<List<Post>> = runCatching {
        log.d { "getGroupFeed — groupId: $groupId" }
        val posts = supabase.from(POSTS_TABLE).select(JOINED_COLUMNS) {
            filter { eq("group_id", groupId) }
            order("created_at", order = Order.DESCENDING)
            order("created_at", order = Order.ASCENDING, referencedTable = RSVPS_TABLE)
            limit(count = ATTENDEE_PREVIEW_LIMIT, referencedTable = RSVPS_TABLE)
        }.decodeList<PostDto>().map { it.toDomain() }
        log.d { "getGroupFeed — loaded ${posts.size} posts" }
        posts
    }

    override suspend fun getPostAttendees(postId: String): Result<List<User>> = runCatching {
        log.d { "getPostAttendees — postId: $postId" }
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
        log.d { "getPostAttendees — loaded ${attendees.size} attendees" }
        attendees
    }.onFailure { e ->
        log.e(e) { "getPostAttendees FAILED — postId: $postId" }
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