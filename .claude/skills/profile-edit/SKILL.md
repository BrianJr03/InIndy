---
name: profile-edit
description: Implement the profile edit bottom sheet for InIndy — edit name, avatar, neighborhood, and interests. Triggered by tapping the avatar or name in MeScreen. Use when building or modifying the profile editing experience.
---

# InIndy Profile Edit

Implement the profile edit bottom sheet for InIndy based on $ARGUMENTS.

## Flow overview

```
MeScreen — tap avatar or display name
    ↓
ProfileEditSheet (ModalBottomSheet, tall — 90% screen height)
    ↓
Edit: avatar + name + neighborhood + interests
    ↓
Tap "Save" → validate → upload avatar (if changed) → persist to UserPreferencesStore
    ↓
Sheet dismisses → MeScreen header updates instantly
```

---

## Phase 1 — Domain layer

### ProfileEditRepository interface
File: `shared/commonMain/domain/repository/ProfileEditRepository.kt`
```kotlin
interface ProfileEditRepository {
    suspend fun updateProfile(
        fullName: String,
        avatarUri: String?,         // null = no change, local URI if changed
        neighborhoodId: String,
        interests: List<Interest>
    ): Result<Unit>
    suspend fun getNeighborhoods(): Result<List<Neighborhood>>
}
```

### FakeProfileEditRepository
File: `shared/commonMain/data/repository/FakeProfileEditRepository.kt`
```kotlin
class FakeProfileEditRepository(
    private val userPreferencesStore: UserPreferencesStore,
    private val mediaRepository: MediaRepository
) : ProfileEditRepository {

    override suspend fun updateProfile(
        fullName: String,
        avatarUri: String?,
        neighborhoodId: String,
        interests: List<Interest>
    ): Result<Unit> {
        delay(800)
        // Upload new avatar if URI provided
        val avatarCdnUrl = if (avatarUri != null) {
            mediaRepository.uploadAvatar(avatarUri).getOrNull()
        } else null

        userPreferencesStore.saveProfile(fullName, avatarCdnUrl)
        userPreferencesStore.saveNeighborhood(
            neighborhoodId,
            fakeNeighborhoods.find { it.id == neighborhoodId }?.name ?: neighborhoodId
        )
        userPreferencesStore.saveInterests(interests)
        return Result.success(Unit)
    }

    override suspend fun getNeighborhoods(): Result<List<Neighborhood>> {
        delay(200)
        return Result.success(fakeNeighborhoods)
    }

    private val fakeNeighborhoods = listOf(
        Neighborhood("broad_ripple", "Broad Ripple"),
        Neighborhood("fountain_square", "Fountain Square"),
        Neighborhood("irvington", "Irvington"),
        Neighborhood("downtown", "Downtown"),
        Neighborhood("mass_ave", "Mass Ave"),
        Neighborhood("bates_hendricks", "Bates-Hendricks"),
        Neighborhood("cottage_home", "Cottage Home"),
        Neighborhood("herron_morton", "Herron-Morton Place"),
        Neighborhood("butler_tarkington", "Butler-Tarkington"),
        Neighborhood("meridian_kessler", "Meridian-Kessler")
    )
}
```

---

## Phase 2 — ViewModel

File: `shared/commonMain/presentation/profileedit/ProfileEditViewModel.kt`

### UiState
```kotlin
data class ProfileEditUiState(
    // Current values — pre-populated from UserPreferences on open
    val currentAvatarUrl: String? = null,       // existing CDN URL
    val newAvatarUri: String? = null,           // local URI if user picked a new photo
    val fullName: String = "",
    val neighborhoodId: String = "",
    val selectedInterests: Set<Interest> = emptySet(),
    val neighborhoods: List<Neighborhood> = emptyList(),

    // State
    val isLoading: Boolean = true,              // loading initial preferences
    val isSaving: Boolean = false,
    val saveError: String? = null,

    // Per-field errors
    val nameError: String? = null,

    // Save button — always enabled, validate on tap
)

// Computed
val ProfileEditUiState.hasChanges: Boolean
    get() = newAvatarUri != null
        || fullName != initialFullName      // compare against loaded values
        || neighborhoodId != initialNeighborhoodId
        || selectedInterests != initialInterests

val ProfileEditUiState.isValid: Boolean
    get() = fullName.length >= 2 && neighborhoodId.isNotBlank() && selectedInterests.isNotEmpty()
```

### Intents
```kotlin
sealed class ProfileEditIntent {
    object Load : ProfileEditIntent()
    data class AvatarSelected(val uri: String) : ProfileEditIntent()
    object RemoveAvatar : ProfileEditIntent()
    data class NameChanged(val text: String) : ProfileEditIntent()
    data class NeighborhoodSelected(val neighborhoodId: String) : ProfileEditIntent()
    data class ToggleInterest(val interest: Interest) : ProfileEditIntent()
    object Save : ProfileEditIntent()
    object Dismiss : ProfileEditIntent()
}
```

### Load logic
```kotlin
// On Load: read UserPreferences, pre-populate all fields
// Load neighborhoods list for picker
init {
    onIntent(ProfileEditIntent.Load)
}

private fun load() {
    viewModelScope.launch {
        val prefs = currentUserProvider.get()
        val neighborhoods = profileEditRepository.getNeighborhoods().getOrDefault(emptyList())
        _state.update { it.copy(
            currentAvatarUrl = prefs.avatarUrl,
            fullName = prefs.fullName ?: "",
            neighborhoodId = prefs.neighborhoodId ?: "",
            selectedInterests = prefs.interests
                .mapNotNull { runCatching { Interest.valueOf(it) }.getOrNull() }
                .toSet(),
            neighborhoods = neighborhoods,
            isLoading = false
        )}
        // Store initial values for hasChanges comparison
    }
}
```

### Save logic
```kotlin
// On Save:
// 1. Validate — if nameError, return
// 2. isSaving = true
// 3. Upload new avatar if newAvatarUri is set via MediaRepository
// 4. Call profileEditRepository.updateProfile(...)
// 5. On success → emit Dismiss event
// 6. On failure → saveError, isSaving = false
```

### Save button behavior
- Always enabled — validate on tap, show `nameError` if name < 2 chars
- "Save" label when not saving, loading indicator when `isSaving`
- Disabled appearance (not disabled functionally) if `!hasChanges` — use reduced opacity

---

## Phase 3 — Sheet layout

File: `shared/commonMain/ui/profile/ProfileEditSheet.kt`

```kotlin
@Composable
fun ProfileEditSheet(
    onDismiss: () -> Unit,
    viewModel: ProfileEditViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // Sheet content
    }
}
```

### Sheet content layout — scrollable column
```
┌─────────────────────────────┐
│  ▬▬▬  (drag handle)         │
│  Edit Profile               │
│  ─────────────────────────  │
│                             │
│       [Avatar 96dp]         │  ← tap to change
│       Change photo          │
│                             │
│  Name *                     │
│  [________________________] │
│  error if < 2 chars         │
│                             │
│  Neighborhood *             │
│  [Broad Ripple          ▾]  │  ← exposed dropdown
│                             │
│  What are you into? *       │
│  [Hike][Run][Picnic]...     │  ← same chip grid as onboarding
│                             │
│  ─────────────────────────  │
│       [ Save changes ]      │
│  (bottom padding for IME)   │
└─────────────────────────────┘
```

### Avatar section
```kotlin
Box(contentAlignment = Alignment.BottomEnd) {
    // Avatar — shows newAvatarUri preview if picked, else currentAvatarUrl
    AsyncImage(
        model = state.newAvatarUri ?: state.currentAvatarUrl?.let { "$it?width=200&height=200&fit=cover" },
        contentDescription = "Profile photo",
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .clickable { /* open ImagePicker */ }
    )
    // Edit badge bottom-right
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
// "Change photo" text below
Text(
    text = "Change photo",
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier
        .clickable { /* open ImagePicker */ }
        .padding(top = 8.dp)
)
```

### Name field
- Label: "Name" with `*`
- Single line, `KeyboardType.Text`, `ImeAction.Next`
- Error shown below if `nameError != null`
- Clear `nameError` on first keystroke

### Neighborhood picker — ExposedDropdownMenuBox
```kotlin
ExposedDropdownMenuBox(
    expanded = neighborhoodDropdownExpanded,
    onExpandedChange = { neighborhoodDropdownExpanded = it }
) {
    OutlinedTextField(
        value = state.neighborhoods.find { it.id == state.neighborhoodId }?.name ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text("Neighborhood") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = neighborhoodDropdownExpanded) },
        modifier = Modifier.menuAnchor().fillMaxWidth()
    )
    ExposedDropdownMenu(
        expanded = neighborhoodDropdownExpanded,
        onDismissRequest = { neighborhoodDropdownExpanded = false }
    ) {
        state.neighborhoods.forEach { neighborhood ->
            DropdownMenuItem(
                text = { Text(neighborhood.name) },
                onClick = {
                    viewModel.onIntent(ProfileEditIntent.NeighborhoodSelected(neighborhood.id))
                    neighborhoodDropdownExpanded = false
                },
                trailingIcon = {
                    if (neighborhood.id == state.neighborhoodId) {
                        Icon(Icons.Rounded.Check, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}
```

### Interests section
- Label: "What are you into?" with `*`
- Same wrapping chip grid as onboarding step 3 — reuse `InterestChipGrid` composable if it exists
- All `Interest` enum values displayed
- Multi-select, minimum 1 required
- Accent color when selected

### Save button
```kotlin
Button(
    onClick = { viewModel.onIntent(ProfileEditIntent.Save) },
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .alpha(if (state.hasChanges) 1f else 0.5f)  // visual hint — not actually disabled
) {
    if (state.isSaving) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp
        )
    } else {
        Text("Save changes")
    }
}
```

### IME padding
Add `Modifier.imePadding()` to the scrollable column so the save button stays above the keyboard when the name field is focused.

---

## Phase 4 — MeScreen hookup

### Trigger
In `MeScreen` header — wrap avatar + name in a clickable:
```kotlin
Row(
    modifier = Modifier
        .clickable { showProfileEditSheet = true }
        .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    AsyncImage(/* avatar */)
    Spacer(Modifier.width(12.dp))
    Column {
        Text(state.user?.fullName ?: "")
        Text(state.user?.neighborhoodName ?: "")
    }
}
```

### Sheet display
```kotlin
var showProfileEditSheet by remember { mutableStateOf(false) }

if (showProfileEditSheet) {
    ProfileEditSheet(
        onDismiss = { showProfileEditSheet = false }
    )
}
```

### Refresh after save
`MeScreen` observes `UserPreferencesStore` via `MeViewModel` — since `UserPreferencesStore` exposes a `Flow<UserPreferences>`, the header updates automatically when save completes. No manual refresh needed.

---

## Phase 5 — Koin wiring

Add to `postModule` or a new `profileModule`:
```kotlin
single<ProfileEditRepository> { FakeProfileEditRepository(get(), get()) }
viewModel { ProfileEditViewModel(get(), get()) }  // CurrentUserProvider, ProfileEditRepository
```

---

## Navigation

No new navigation routes needed — `ProfileEditSheet` is a `ModalBottomSheet` presented inline in `MeScreen`, not a navigation destination.

---

## Implementation order
1. `ProfileEditRepository` interface + `FakeProfileEditRepository`
2. Koin wiring
3. `ProfileEditViewModel` — load, save, all intents
4. `ProfileEditSheet` — avatar, name, neighborhood dropdown, interests chips, save button
5. `MeScreen` hookup — clickable header, sheet toggle, auto-refresh via Flow

## What NOT to do
- Don't navigate to a new screen — this is a `ModalBottomSheet` only
- Don't upload avatar eagerly on pick — upload as part of save flow only
- Don't disable the Save button — always enabled, validate on tap
- Don't show Save button as fully disabled when no changes — use reduced opacity only
- Don't forget `imePadding()` — keyboard will cover the save button without it
- Don't manually refresh `MeScreen` after save — `UserPreferencesStore` Flow handles it automatically
- Don't allow zero interests — minimum 1 required, show error if user deselects all
- Don't hardcode neighborhoods — always load from `ProfileEditRepository.getNeighborhoods()`
  EOF