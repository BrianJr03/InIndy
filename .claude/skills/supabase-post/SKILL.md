---
name: supabase-post
description: Wire the create post flow to the real Supabase backend — image upload via R2 Edge Function, post insertion, post_images and post_tags rows, and feed refresh after creation. Use after SupabasePostRepository is swapped in and the get-upload-url Edge Function is deployed.
---

# InIndy Supabase Create Post Wiring

Wire the create post flow to the real Supabase backend for InIndy based on $ARGUMENTS.

## Prerequisites
- `SupabasePostRepository` swapped in Koin (from `/supabase-post` skill)
- `get-upload-url` Edge Function deployed and secrets set
- R2 bucket `inindy-media` created with CORS policy applied
- Migrations applied — `posts`, `post_images`, `post_tags` tables exist

---

## What changes vs fake implementation

| Layer | Fake | Real |
|---|---|---|
| Image upload | returns picsum URL immediately | compress → call Edge Function → PUT to R2 → get CDN URL |
| Post insert | adds to in-memory list | inserts to `posts` table via postgrest |
| post_images | not stored | inserted to `post_images` table after post created |
| post_tags | not stored | inserted to `post_tags` table after post created |
| Feed refresh | reads from memory | re-fetches from Supabase after creation |

---

## Phase 1 — MediaRemoteDataSource (real implementation)

File: `shared/commonMain/data/remote/media/MediaRemoteDataSourceImpl.kt`

```kotlin
class MediaRemoteDataSourceImpl(
    private val supabase: SupabaseClient,
    private val httpClient: HttpClient    // plain Ktor client for R2 PUT
) : MediaRemoteDataSource {

    override suspend fun getUploadUrl(
        fileName: String,
        contentType: String,
        context: String     // "post" | "avatar" | "group"
    ): Result<UploadUrlResponse> = runCatching {
        val response = supabase.functions.invoke(
            function = "get-upload-url",
            body = mapOf(
                "fileName" to fileName,
                "contentType" to contentType,
                "context" to context
            )
        )
        Json.decodeFromString<UploadUrlResponse>(response.data)
    }

    override suspend fun uploadImage(
        uploadUrl: String,
        bytes: ByteArray,
        contentType: String = "image/jpeg"
    ): Result<Unit> = runCatching {
        // PUT directly to R2 signed URL — no auth header needed
        httpClient.put(uploadUrl) {
            setBody(bytes)
            contentType(ContentType.parse(contentType))
        }
        Unit
    }
}

@Serializable
data class UploadUrlResponse(
    val uploadUrl: String,
    val publicUrl: String,
    val key: String
)
```

---

## Phase 2 — MediaRepositoryImpl (real implementation)

File: `shared/commonMain/data/repository/MediaRepositoryImpl.kt`

```kotlin
class MediaRepositoryImpl(
    private val remoteDataSource: MediaRemoteDataSource,
    private val imageCompressor: ImageCompressor
) : MediaRepository {

    override suspend fun uploadPostImage(uri: String): Result<String> = runCatching {
        // 1. Compress
        val bytes = imageCompressor.compress(uri)

        // 2. Get signed upload URL
        val fileName = "post_${System.currentTimeMillis()}.jpg"
        val uploadResponse = remoteDataSource
            .getUploadUrl(fileName, "image/jpeg", "post")
            .getOrThrow()

        // 3. PUT compressed bytes to R2
        remoteDataSource.uploadImage(uploadResponse.uploadUrl, bytes).getOrThrow()

        // 4. Return permanent CDN URL
        uploadResponse.publicUrl
    }

    override suspend fun uploadAvatar(uri: String): Result<String> = runCatching {
        val bytes = imageCompressor.compress(uri)
        val uploadResponse = remoteDataSource
            .getUploadUrl("avatar.jpg", "image/jpeg", "avatar")
            .getOrThrow()
        remoteDataSource.uploadImage(uploadResponse.uploadUrl, bytes).getOrThrow()
        uploadResponse.publicUrl
    }

    override suspend fun uploadGroupCover(uri: String): Result<String> = runCatching {
        val bytes = imageCompressor.compress(uri)
        val uploadResponse = remoteDataSource
            .getUploadUrl("cover.jpg", "image/jpeg", "group")
            .getOrThrow()
        remoteDataSource.uploadImage(uploadResponse.uploadUrl, bytes).getOrThrow()
        uploadResponse.publicUrl
    }
}
```

---

## Phase 3 — CreatePostViewModel submit flow (real)

The ViewModel submit logic stays the same — it calls `MediaRepository.uploadPostImage()` per image then `PostRepository.createPost()`. No ViewModel changes needed since both repositories implement the same interfaces.

The only thing to verify in `CreatePostViewModel`:

```kotlin
// On Submit — ensure images upload in parallel not sequentially
private fun submit() {
    viewModelScope.launch {
        _state.update { it.copy(isSubmitting = true, submitError = null) }

        // Upload all images in parallel
        val uploadResults = state.value.images.map { uri ->
            async { mediaRepository.uploadPostImage(uri) }
        }.awaitAll()

        // Fail fast if any upload failed
        val failedUpload = uploadResults.firstOrNull { it.isFailure }
        if (failedUpload != null) {
            _state.update {
                it.copy(
                    isSubmitting = false,
                    submitError = "Image upload failed — check your connection and try again"
                )
            }
            return@launch
        }

        val cdnUrls = uploadResults.map { it.getOrThrow() }

        // Create post with CDN URLs
        val request = buildCreatePostRequest(cdnUrls)
        postRepository.createPost(request)
            .onSuccess {
                _navigationEvent.emit(NavigationEvent.PopBack)
            }
            .onFailure { error ->
                _state.update {
                    it.copy(isSubmitting = false, submitError = error.message)
                }
            }
    }
}
```

---

## Phase 4 — Koin wiring

Update `shared/commonMain/di/MediaModule.kt`:
```kotlin
val mediaModule = module {
    // Before:
    // single<MediaRepository> { FakeMediaRepository() }
    // single<MediaRemoteDataSource> { FakeMediaRemoteDataSource() }

    // After:
    single<MediaRemoteDataSource> {
        MediaRemoteDataSourceImpl(
            supabase = SupabaseClientProvider.client,
            httpClient = get()          // plain Ktor HttpClient — no auth headers
        )
    }
    single<MediaRepository> {
        MediaRepositoryImpl(
            remoteDataSource = get(),
            imageCompressor = get()
        )
    }

    // These stay the same
    single { ImageCompressor() }
    single { ImagePicker() }
    single { CameraCapture() }
}
```

Add a plain Ktor `HttpClient` to `CoreModule` for R2 uploads — separate from the Supabase client:
```kotlin
// In CoreModule
single {
    HttpClient {
        install(ContentNegotiation) {
            json()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000   // 30s for image uploads
        }
    }
}
```

---

## Phase 5 — Feed refresh after post creation

After `createPost` succeeds, the Explore feed needs to reflect the new post.

### Option A — Re-fetch on resume (simplest)
In `ExploreViewModel` use `LaunchedEffect` tied to lifecycle:
```kotlin
// Re-fetch feed every time Explore tab comes into focus
LaunchedEffect(lifecycleOwner.lifecycle) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        viewModel.onIntent(ExploreIntent.Refresh)
    }
}
```

### Option B — Real-time subscription (more complex)
Use Supabase real-time to observe the `posts` table and push new posts into the feed automatically. Implement this in v2 — Option A is sufficient for MVP.

---

## Phase 6 — Error states to handle

These are real errors that won't happen with fake repos — handle them gracefully:

```kotlin
// In CreatePostViewModel — map upload errors to user-friendly messages
fun Throwable.toUploadError(): String = when {
    message?.contains("timeout", ignoreCase = true) == true ->
        "Upload timed out — check your connection"
    message?.contains("403", ignoreCase = true) == true ->
        "Upload permission denied — try signing out and back in"
    message?.contains("413", ignoreCase = true) == true ->
        "Image is too large — try a smaller photo"
    else -> "Upload failed — please try again"
}
```

Also handle the case where the Edge Function is not deployed yet:
```kotlin
message?.contains("404", ignoreCase = true) == true ->
    "Upload service unavailable — contact support"
```

---

## Phase 7 — End to end test checklist

Run through this manually after wiring:

- [ ] Pick 1 photo → tap Post → photo appears in Your Posts feed
- [ ] Pick 3 photos → tap Post → all 3 appear in post detail
- [ ] Post appears in Explore neighborhood feed
- [ ] Post appears in Supabase dashboard → Table Editor → `posts` table
- [ ] Images appear in `post_images` table with correct `post_id`
- [ ] Tags appear in `post_tags` table with correct Interest enum names
- [ ] R2 bucket `inindy-media` → Objects tab shows uploaded files under `posts/{userId}/`
- [ ] CDN URL loads in browser: `https://your-cdn-url.r2.dev/posts/{userId}/{uuid}.jpg`
- [ ] Delete post → row removed from `posts` table → `post_images` + `post_tags` cascade deleted

---

## Implementation order
1. `UploadUrlResponse` DTO
2. `MediaRemoteDataSourceImpl` — Edge Function call + R2 PUT
3. `MediaRepositoryImpl` — compress → upload → return CDN URL
4. Plain `HttpClient` in `CoreModule`
5. Swap `MediaModule` bindings — `FakeMediaRepository` → `MediaRepositoryImpl`
6. Verify `CreatePostViewModel.submit()` uses `async/awaitAll` for parallel uploads
7. Add `ExploreIntent.Refresh` + lifecycle-based re-fetch in `ExploreScreen`
8. Run end to end test checklist

## What NOT to do
- Don't add auth headers to the R2 PUT request — the signed URL handles auth, adding a header will break it
- Don't upload images sequentially — use `async/awaitAll` for parallel uploads
- Don't store local device URIs in the database — only CDN URLs from R2
- Don't skip the end to end test checklist — silent failures are common with signed URLs
- Don't implement real-time feed subscription yet — lifecycle-based refresh is sufficient for MVP
- Don't forget the 30s timeout on the Ktor client — image uploads will silently hang without it
  EOF