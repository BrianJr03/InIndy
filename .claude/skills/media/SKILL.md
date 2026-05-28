---
name: media
description: Implement photo picking, compression, upload to Cloudflare R2, and Coil image loading for InIndy. Use when building any feature that involves user photo uploads, post images, or avatar handling.
---

# InIndy Media Upload

Implement the complete media pipeline for InIndy based on $ARGUMENTS.

## Overview
Photos are uploaded directly from the device to Cloudflare R2 via a signed URL.
The backend (Supabase Edge Function) only issues the signed URL — it never handles image bytes.
All images are served via Cloudflare CDN with on-the-fly resizing.
No video support — photos only at MVP.

---

## Phase 1 — Image picker expect/actual

### Interface in commonMain
File: `shared/commonMain/data/media/ImagePicker.kt`
```kotlin
data class PickedImage(
    val uri: String,          // platform-specific URI string
    val mimeType: String      // always "image/jpeg" after compression
)

expect class ImagePicker {
    suspend fun pickSingle(): PickedImage?
    suspend fun pickMultiple(max: Int): List<PickedImage>
}
```

### Android actual
File: `shared/androidMain/data/media/ImagePicker.android.kt`
- Use `ActivityResultContracts.PickVisualMedia` (Photo Picker API — no permission required on Android 13+)
- For Android 12 and below fall back to `READ_EXTERNAL_STORAGE` permission check before launching
- Return device URI as string

### iOS actual
File: `shared/iosMain/data/media/ImagePicker.ios.kt`
- Use `PHPickerViewController` — no permission required for photo library access
- Return file URL as string

---

## Phase 2 — Image compression expect/actual

### Interface in commonMain
File: `shared/commonMain/data/media/ImageCompressor.kt`
```kotlin
expect class ImageCompressor {
    suspend fun compress(uri: String): ByteArray
}
```

### Compression targets
- Format: JPEG always (never PNG for photos)
- Quality: 80%
- Max dimension: 1200px on longest side — scale down proportionally, never upscale
- Target output size: under 1MB per image
- Always compress before upload — never upload raw camera bytes

### Android actual
File: `shared/androidMain/data/media/ImageCompressor.android.kt`
```kotlin
// Pseudocode — implement fully
val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
val scaled = scaleBitmap(bitmap, maxDimension = 1200)
val outputStream = ByteArrayOutputStream()
scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
return outputStream.toByteArray()
```

### iOS actual
File: `shared/iosMain/data/media/ImageCompressor.ios.kt`
```swift
// Pseudocode — implement fully
let image = UIImage(contentsOfFile: uri)
let scaled = image.scaleToFit(maxDimension: 1200)
return scaled.jpegData(compressionQuality: 0.8)
```

---

## Phase 3 — Signed URL (Supabase Edge Function)

### Edge function
File: `supabase/functions/get-upload-url/index.ts`

The function receives `{ fileName, contentType }` and returns a signed R2 upload URL.
It must:
- Verify the user is authenticated (check Supabase JWT)
- Generate a unique key: `posts/{userId}/{uuid}.jpg`
- Return `{ uploadUrl, publicUrl }` — uploadUrl is temporary (expires 60s), publicUrl is the permanent CDN URL

```typescript
// Key generation pattern
const key = `posts/${userId}/${crypto.randomUUID()}.jpg`
const uploadUrl = await r2.getSignedUrl(key, { expiresIn: 60 })
const publicUrl = `${CLOUDFLARE_CDN_BASE}/${key}`
return { uploadUrl, publicUrl }
```

### Repository call in commonMain
File: `shared/commonMain/data/remote/media/MediaRemoteDataSource.kt`
```kotlin
data class UploadUrlResponse(
    val uploadUrl: String,
    val publicUrl: String
)

interface MediaRemoteDataSource {
    suspend fun getUploadUrl(fileName: String): Result<UploadUrlResponse>
    suspend fun uploadImage(uploadUrl: String, bytes: ByteArray): Result<Unit>
}
```

Upload to the signed URL via a raw PUT request — no auth header, no JSON:
```kotlin
client.put(uploadUrl) {
    setBody(bytes)
    contentType(ContentType.Image.JPEG)
}
```

---

## Phase 4 — Media repository

File: `shared/commonMain/data/repository/MediaRepositoryImpl.kt`

```kotlin
interface MediaRepository {
    // Returns the permanent Cloudflare CDN URL
    suspend fun uploadPostImage(uri: String): Result<String>
    suspend fun uploadAvatar(uri: String): Result<String>
    suspend fun deleteImage(publicUrl: String): Result<Unit>
}
```

### Full upload pipeline (implement in MediaRepositoryImpl)
```
1. compress(uri) → ByteArray
2. getUploadUrl(fileName) → { uploadUrl, publicUrl }
3. uploadImage(uploadUrl, bytes) → Unit
4. return publicUrl
```

All three steps in a single `runCatching` block — if any step fails, surface a clean error.

### Avatar vs post image
- Post images: key prefix `posts/{userId}/{uuid}.jpg`
- Avatars: key prefix `avatars/{userId}.jpg` — overwrite on update (same key, no versioning needed)

---

## Phase 5 — ViewModel integration

### Upload state
Add to any ViewModel that handles photo uploads:
```kotlin
sealed class MediaUploadState {
    object Idle : MediaUploadState()
    data class Uploading(val progress: Float) : MediaUploadState()  // 0.0–1.0
    data class Success(val cdnUrl: String) : MediaUploadState()
    data class Error(val message: String) : MediaUploadState()
}
```

### Post creation ViewModel pattern
```kotlin
// Upload all images first, collect CDN URLs, then submit post
val uploadResults = imageUris.map { uri ->
    mediaRepository.uploadPostImage(uri)
}
if (uploadResults.any { it.isFailure }) {
    // surface error — do not submit partial post
    return
}
val cdnUrls = uploadResults.map { it.getOrThrow() }
// proceed to create post with cdnUrls
```
Never submit a post if any image upload failed — partial posts with missing images are worse than a failed submission.

---

## Phase 6 — Coil image loading

### Always use size-appropriate Cloudflare variants
```kotlin
// Feed thumbnail (in PostCard)
AsyncImage(
    model = "${cdnUrl}?width=600&fit=cover",
    contentDescription = ...,
    contentScale = ContentScale.Crop,
    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
)

// Detail / full view
AsyncImage(
    model = "${cdnUrl}?width=1200&fit=scale-down",
    contentDescription = ...,
    contentScale = ContentScale.Fit
)

// Avatar
AsyncImage(
    model = "${cdnUrl}?width=200&height=200&fit=cover",
    contentDescription = ...,
    modifier = Modifier.size(40.dp).clip(CircleShape)
)
```

### Shimmer placeholder
Always use shimmer while loading — never the default gray box:
```kotlin
AsyncImage(
    model = url,
    placeholder = ShimmerPainter(),  // custom shimmer painter
    error = painterResource(Res.drawable.ic_image_error),
    ...
)
```

---

## Phase 7 — Post image limits

- Maximum 3 photos per post — enforce in UI (disable picker after 3) and in the repository
- Minimum 1 photo per post — "Post" CTA disabled until at least 1 image is uploaded
- Show upload progress per image — don't block the whole form while uploading
- Allow removal of a selected image before submitting — tap X on thumbnail to remove

---

## Fake implementation (for use before Cloudflare/R2 is configured)

File: `shared/commonMain/data/repository/FakeMediaRepository.kt`
```kotlin
class FakeMediaRepository : MediaRepository {
    override suspend fun uploadPostImage(uri: String): Result<String> {
        delay(1500) // simulate upload time
        return Result.success("https://picsum.photos/seed/${uuid}/1200/800")
    }
    override suspend fun uploadAvatar(uri: String): Result<String> {
        delay(1000)
        return Result.success("https://picsum.photos/seed/${uuid}/200/200")
    }
}
```
Swap in Koin → one line change when R2 is configured.

---

## Koin wiring
Add to `shared/commonMain/di/MediaModule.kt`:
```kotlin
val mediaModule = module {
    single<MediaRepository> { FakeMediaRepository() }  // swap for MediaRepositoryImpl when R2 ready
    single<MediaRemoteDataSource> { MediaRemoteDataSourceImpl(get()) }
    single { ImageCompressor() }
    single { ImagePicker() }
}
```

---

## Implementation order
1. `ImagePicker` expect/actual
2. `ImageCompressor` expect/actual
3. `FakeMediaRepository` + Koin wiring
4. UI — photo picker in post creation + avatar upload in onboarding
5. Supabase Edge Function (`get-upload-url`)
6. `MediaRemoteDataSourceImpl` with real R2 upload
7. Swap `FakeMediaRepository` → `MediaRepositoryImpl` in Koin

## What NOT to do
- Don't upload through Supabase Storage — R2 only
- Don't upload uncompressed images — always compress first
- Don't store device URIs in the database — only permanent CDN URLs
- Don't block form submission while images upload — upload in background, enable CTA when done
- Don't load full-size images in feeds — always use Cloudflare resize params
- Don't implement video — deferred to v2
- Don't upload avatar to a versioned key — always overwrite `avatars/{userId}.jpg`
  EOF