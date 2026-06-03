---
name: photo-picker
description: Implement the reusable photo picker bottom sheet for InIndy — camera and gallery sources, used across create post, create group cover, and profile avatar. Use when building or modifying any photo selection experience in the app.
---

# InIndy Photo Picker

Implement the reusable photo picker bottom sheet for InIndy based on $ARGUMENTS.
Used in three contexts: post images (multi), group cover (single), profile avatar (single).

## Flow overview

```
User taps photo add/edit trigger
    ↓
PhotoPickerSheet (ModalBottomSheet)
    ├── 📷 Camera  → native camera → compress → return URI
    └── 🖼 Gallery → system photo picker → compress → return URI
    ↓
Caller receives URI via callback — handles preview + upload
```

---

## Phase 1 — Camera expect/actual

The `ImagePicker` in the `/media` skill covers gallery only.
Camera capture requires an additional `expect/actual`.

### Interface
File: `shared/commonMain/data/media/CameraCapture.kt`
```kotlin
expect class CameraCapture {
    suspend fun capturePhoto(): String?   // returns local URI or null if cancelled
}
```

### Android actual
File: `shared/androidMain/data/media/CameraCapture.android.kt`
- Use `ActivityResultContracts.TakePicture` with a `FileProvider` URI
- Save to app-specific cache directory — `context.cacheDir/photos/{uuid}.jpg`
- Request `CAMERA` permission before launching
- Return the file URI as a string on success, null on cancel or permission denied

### iOS actual
File: `shared/iosMain/data/media/CameraCapture.ios.kt`
- Use `UIImagePickerController` with `sourceType = .camera`
- Request `NSCameraUsageDescription` — ensure it's in `Info.plist`
- Save captured image to temp directory
- Return file URL as string on success, null on cancel or permission denied

---

## Phase 2 — Updated ImagePicker expect/actual

Update existing `ImagePicker` to support both single and multiple picks clearly.

File: `shared/commonMain/data/media/ImagePicker.kt`
```kotlin
expect class ImagePicker {
    suspend fun pickSingle(): String?           // returns local URI or null if cancelled
    suspend fun pickMultiple(max: Int): List<String>  // returns list of URIs
}
```

### Android actual updates
- `pickSingle`: use `PickVisualMedia(ImageOnly)` — single selection mode
- `pickMultiple`: use `PickMultipleVisualMedia(maxItems = max)`
- Both return device URI strings

### iOS actual updates
- `pickSingle`: `PHPickerConfiguration` with `selectionLimit = 1`
- `pickMultiple`: `PHPickerConfiguration` with `selectionLimit = max`
- Return file URL strings

---

## Phase 3 — PhotoPickerSheet composable

File: `shared/commonMain/ui/components/PhotoPickerSheet.kt`

```kotlin
@Composable
fun PhotoPickerSheet(
    mode: PhotoPickerMode,
    onPhotoSelected: (String) -> Unit,        // single URI callback
    onPhotosSelected: (List<String>) -> Unit, // multi URI callback (post only)
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)

sealed class PhotoPickerMode {
    object Single : PhotoPickerMode()                    // avatar, group cover
    data class Multiple(val max: Int) : PhotoPickerMode() // post images
}
```

### Sheet layout
```
┌─────────────────────────────┐
│  ▬▬▬  (drag handle)         │
│                             │
│  📷  Take a photo           │
│  ─────────────────────────  │
│  🖼  Choose from gallery    │
│                             │
└─────────────────────────────┘
```

```kotlin
ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    Column(modifier = Modifier.navigationBarsPadding()) {

        // Camera option
        ListItem(
            headlineContent = { Text("Take a photo") },
            leadingContent = {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
            },
            modifier = Modifier.clickable {
                onDismiss()
                // Launch camera after sheet dismisses
                launchCamera()
            }
        )

        HorizontalDivider()

        // Gallery option
        ListItem(
            headlineContent = {
                Text(
                    when (mode) {
                        is PhotoPickerMode.Single -> "Choose from gallery"
                        is PhotoPickerMode.Multiple -> "Choose from gallery (up to ${mode.max})"
                    }
                )
            },
            leadingContent = {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
            },
            modifier = Modifier.clickable {
                onDismiss()
                // Launch gallery after sheet dismisses
                launchGallery()
            }
        )

        Spacer(Modifier.height(8.dp))
    }
}
```

### Sheet behavior
- `skipPartiallyExpanded = true` — opens fully, no half-expanded state
- Dismiss sheet before launching camera/gallery — prevents sheet + system picker overlap
- Use `LaunchedEffect` with a flag to launch picker after sheet animation completes
- Camera and gallery results both run through `ImageCompressor` before calling back
- Return compressed URI — caller never sees uncompressed bytes

### Compression on result
```kotlin
// After camera capture or gallery pick — compress before calling back
val compressedUri = withContext(Dispatchers.IO) {
    imageCompressor.compress(rawUri)
    // Write compressed bytes to temp file, return new URI
}
onPhotoSelected(compressedUri)
```

---

## Phase 4 — AvatarPickerSection composable

Reusable composable for single-photo avatar contexts (profile edit, onboarding profile step).

File: `shared/commonMain/ui/components/AvatarPickerSection.kt`
```kotlin
@Composable
fun AvatarPickerSection(
    currentImageUrl: String?,       // existing CDN URL
    newImageUri: String?,           // local URI if newly picked
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPickerSheet by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = newImageUri ?: currentImageUrl?.let { "$it?width=200&height=200&fit=cover" },
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .clickable { showPickerSheet = true }
            )
            // Camera badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Rounded.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (newImageUri != null || currentImageUrl != null) "Change photo" else "Add photo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { showPickerSheet = true }
        )
    }

    if (showPickerSheet) {
        PhotoPickerSheet(
            mode = PhotoPickerMode.Single,
            onPhotoSelected = {
                onImageSelected(it)
                showPickerSheet = false
            },
            onPhotosSelected = {},
            onDismiss = { showPickerSheet = false }
        )
    }
}
```

---

## Phase 5 — CoverPhotoPickerSection composable

Reusable composable for single-photo cover contexts (create group, edit group).

File: `shared/commonMain/ui/components/CoverPhotoPickerSection.kt`
```kotlin
@Composable
fun CoverPhotoPickerSection(
    currentImageUrl: String?,
    newImageUri: String?,
    onImageSelected: (String) -> Unit,
    onImageRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPickerSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { showPickerSheet = true }
    ) {
        if (newImageUri != null || currentImageUrl != null) {
            // Show photo preview
            AsyncImage(
                model = newImageUri ?: currentImageUrl?.let { "$it?width=800&fit=cover" },
                contentDescription = "Cover photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Remove button top-right
            IconButton(
                onClick = onImageRemoved,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove photo",
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            // Edit overlay bottom-left
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
            ) {
                Text(
                    "Change cover photo",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        } else {
            // Empty state
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.AddAPhoto,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add a cover photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showPickerSheet) {
        PhotoPickerSheet(
            mode = PhotoPickerMode.Single,
            onPhotoSelected = {
                onImageSelected(it)
                showPickerSheet = false
            },
            onPhotosSelected = {},
            onDismiss = { showPickerSheet = false }
        )
    }
}
```

---

## Phase 6 — PostImagePickerRow composable

Reusable composable for multi-photo post contexts (create post).

File: `shared/commonMain/ui/components/PostImagePickerRow.kt`
```kotlin
@Composable
fun PostImagePickerRow(
    images: List<String>,           // local URIs or CDN URLs
    maxImages: Int = 3,
    onImagesAdded: (List<String>) -> Unit,
    onImageRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPickerSheet by remember { mutableStateOf(false) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        // Add button — hidden once max reached
        if (images.size < maxImages) {
            item {
                AddImageButton(onClick = { showPickerSheet = true })
            }
        }

        // Existing image thumbnails
        items(images) { uri ->
            ImageThumbnail(
                uri = uri,
                onRemove = { onImageRemoved(uri) }
            )
        }
    }

    if (showPickerSheet) {
        val remaining = maxImages - images.size
        PhotoPickerSheet(
            mode = PhotoPickerMode.Multiple(max = remaining),
            onPhotoSelected = { onImagesAdded(listOf(it)) },
            onPhotosSelected = { onImagesAdded(it) },
            onDismiss = { showPickerSheet = false }
        )
    }
}

@Composable
private fun AddImageButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.AddAPhoto,
            contentDescription = "Add photo",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ImageThumbnail(uri: String, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(80.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
        )
        // Remove × top-right
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
        ) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f)) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.padding(3.dp)
                )
            }
        }
    }
}
```

---

## Phase 7 — Permissions handling

### Android
- Camera: `CAMERA` permission — request before launching `CameraCapture`
- Gallery (Android 13+): `READ_MEDIA_IMAGES` — Photo Picker API doesn't require it, but camera does
- Use `rememberPermissionState` from Accompanist or built-in CMP permission handling
- If permission denied: show `Snackbar` — "Camera permission is required to take photos"
- If permanently denied: show dialog with "Open Settings" action

### iOS
- Camera: `NSCameraUsageDescription` in `Info.plist` — required
- Gallery: `PHPickerViewController` requires no permission on iOS 14+
- If camera permission denied: show alert with "Open Settings" deep link

### Permission flow in PhotoPickerSheet
```kotlin
// On "Take a photo" tap:
// 1. Check camera permission
// 2. If granted → dismiss sheet → launch CameraCapture
// 3. If denied → show permission rationale → do not dismiss sheet
```

---

## Phase 8 — Integration points

### Replace in CreatePostScreen
Replace existing photo section with:
```kotlin
PostImagePickerRow(
    images = uiState.images,
    maxImages = 3,
    onImagesAdded = { uris -> uris.forEach { vm.onIntent(CreatePostIntent.AddImage(it)) } },
    onImageRemoved = { vm.onIntent(CreatePostIntent.RemoveImage(it)) }
)
```

### Replace in CreateGroupScreen
Replace existing cover photo area with:
```kotlin
CoverPhotoPickerSection(
    currentImageUrl = null,
    newImageUri = uiState.coverImageUri,
    onImageSelected = { vm.onIntent(CreateGroupIntent.CoverImageSelected(it)) },
    onImageRemoved = { vm.onIntent(CreateGroupIntent.RemoveCoverImage) }
)
```

### Replace in ProfileEditSheet
Replace existing avatar section with:
```kotlin
AvatarPickerSection(
    currentImageUrl = uiState.currentAvatarUrl,
    newImageUri = uiState.newAvatarUri,
    onImageSelected = { vm.onIntent(ProfileEditIntent.AvatarSelected(it)) }
)
```

### Replace in OnboardingProfileScreen
Replace existing avatar picker with:
```kotlin
AvatarPickerSection(
    currentImageUrl = null,
    newImageUri = uiState.avatarUri,
    onImageSelected = { vm.onIntent(OnboardingIntent.AvatarSelected(it)) }
)
```

---

## Koin wiring

Add to `CoreModule` or `MediaModule`:
```kotlin
single { CameraCapture() }
// ImagePicker already registered in MediaModule
```

Inject `CameraCapture` and `ImagePicker` into `PhotoPickerSheet` via Koin or pass as parameters.

---

## Implementation order
1. `CameraCapture` expect/actual — Android (`FileProvider`) + iOS (`UIImagePickerController`)
2. Update `ImagePicker` expect/actual — ensure `pickSingle()` and `pickMultiple(max)` are clean
3. Add `CameraCapture` to Koin
4. `PhotoPickerSheet` composable — sheet with camera + gallery options, compress on result
5. `AvatarPickerSection` composable
6. `CoverPhotoPickerSection` composable
7. `PostImagePickerRow` composable — with `AddImageButton` and `ImageThumbnail`
8. Permissions handling — Android camera permission, iOS `Info.plist`
9. Replace photo sections in `CreatePostScreen`, `CreateGroupScreen`, `ProfileEditSheet`, `OnboardingProfileScreen`

## What NOT to do
- Don't launch camera or gallery while sheet is still animating — dismiss sheet first, use `LaunchedEffect`
- Don't pass raw uncompressed URIs to callbacks — always compress first
- Don't skip camera permission check on Android — crash guaranteed without it
- Don't reimplement picker logic per screen — always use the three reusable composables
- Don't allow more than `max` images in `PostImagePickerRow` — hide add button when limit reached
- Don't store CDN URLs in `newImageUri` — that field is for local device URIs only
- Don't upload on pick — callers handle upload timing (on save / on submit)
  EOF