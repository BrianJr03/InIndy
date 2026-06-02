---
name: create-group
description: Implement the create group screen for InIndy. Use when building or modifying the group creation experience, navigated to from the Me tab Your Groups section.
---

# InIndy Create Group Screen

Implement the create group screen for InIndy based on $ARGUMENTS.
Build against fake repositories — Supabase does not need to exist yet.

## Flow overview

```
Me tab → Your Groups → "＋ Create a group"
    ↓
CreateGroupScreen (full screen, pushed from Me tab)
    ↓
Fill: cover photo (optional) + name (required) + description (optional)
    ↓
Tap "Create" → validate → upload cover photo (if set) → create group
    ↓
Navigate to GroupManagementScreen (replaces CreateGroupScreen in back stack)
Back from GroupManagementScreen → Me tab
```

---

## Phase 1 — Domain layer

### Group domain model
File: `shared/commonMain/domain/model/Group.kt`
```kotlin
data class Group(
    val id: String,
    val name: String,
    val description: String?,
    val coverUrl: String?,      // Cloudflare CDN URL — null if no cover set
    val createdBy: String,      // user id
    val isOpen: Boolean,        // false = invite-only (default)
    val memberCount: Int,
    val createdAt: Instant
)
```

### GroupMember domain model
File: `shared/commonMain/domain/model/GroupMember.kt`
```kotlin
data class GroupMember(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: GroupRole,
    val joinedAt: Instant
)

enum class GroupRole { ADMIN, MEMBER }
```

### Create group request
File: `shared/commonMain/domain/model/CreateGroupRequest.kt`
```kotlin
data class CreateGroupRequest(
    val name: String,
    val description: String?,
    val coverImageUri: String?   // local device URI — converted to CDN URL before submit
)
```

### GroupRepository — ensure these are present
File: `shared/commonMain/domain/repository/GroupRepository.kt`
```kotlin
interface GroupRepository {
    suspend fun getUserGroups(): Result<List<Group>>
    suspend fun createGroup(request: CreateGroupRequest): Result<Group>
    suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>>
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
    suspend fun generateInviteLink(groupId: String): Result<String>
    suspend fun deleteGroup(groupId: String): Result<Unit>
}
```

### FakeGroupRepository — update createGroup
```kotlin
override suspend fun createGroup(request: CreateGroupRequest): Result<Group> {
    delay(1000)
    return Result.success(
        Group(
            id = "group_${uuid}",
            name = request.name,
            description = request.description,
            coverUrl = request.coverImageUri?.let { "https://picsum.photos/seed/${uuid}/800/400" },
            createdBy = "current_user_id",
            isOpen = false,
            memberCount = 1,
            createdAt = Clock.System.now()
        )
    )
}
```

---

## Phase 2 — ViewModel

File: `shared/commonMain/presentation/creategroup/CreateGroupViewModel.kt`

### UiState
```kotlin
data class CreateGroupUiState(
    val coverImageUri: String? = null,       // local device URI for preview
    val coverUploadUrl: String? = null,      // CDN URL after upload
    val name: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    // Per-field errors — null means no error shown
    val nameError: String? = null
)

// Computed — never stored in state
val CreateGroupUiState.isValid: Boolean
    get() = name.length >= 3 && !isSubmitting
```

### Intents
```kotlin
sealed class CreateGroupIntent {
    data class CoverImageSelected(val uri: String) : CreateGroupIntent()
    object RemoveCoverImage : CreateGroupIntent()
    data class NameChanged(val text: String) : CreateGroupIntent()
    data class DescriptionChanged(val text: String) : CreateGroupIntent()
    object Submit : CreateGroupIntent()
}
```

### Submit logic
```kotlin
// On Submit:
// 1. Validate name — if invalid, set nameError and return
// 2. isSubmitting = true
// 3. If coverImageUri is set → compress + upload via MediaRepository
//    → store CDN URL, use bucket prefix groups/{groupId}/cover.jpg
//    → if upload fails → set submitError, isSubmitting = false, return
// 4. createGroup(CreateGroupRequest(name, description, coverCdnUrl))
// 5. On success → emit navigation event to GroupManagementScreen
//    → use popUpTo(create_group, inclusive = true) so back stack lands on Me tab
```

### Create button behavior
- Create button is ALWAYS enabled — never disabled
- On tap: validate name, show `nameError` if invalid, only proceed if `isValid` is true
- Clear `nameError` as soon as user edits the name field

### Field error messages
- `nameError`: "Group name must be at least 3 characters"

---

## Phase 3 — Screen

File: `shared/commonMain/ui/creategroup/CreateGroupScreen.kt`

Apply the `/design` skill. Warm, community feel — creating a group should feel exciting, not administrative.

### System insets
Outer Box must include `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`

### Top bar
- Left: ✕ close button → pop back to Me tab (no discard dialog — fields are simple)
- Center: "Create Group" title
- Right: "Create" filled accent button — always enabled

### Cover photo section
```
┌─────────────────────────────┐
│                             │
│   [Camera icon]             │
│   Add a cover photo         │  ← tap anywhere to open ImagePicker
│                             │
└─────────────────────────────┘
```
- Full-width rectangular area, `200dp` height, `RoundedCornerShape(16.dp)`
- Dashed border + camera icon + "Add a cover photo" label when empty
- Background: subtle accent color tint when empty
- Once photo selected: show compressed preview filling the area
- Small ✕ remove button top-right corner of preview — clears the photo
- Tap on existing preview → opens picker to replace

### Name field
- Label: "Group name"
- Required indicator: `*` after label
- Single line text field
- Placeholder: "Trail runners, park crew, weekend warriors…"
- Character counter bottom-right: "12 / 50" (max 50 chars)
- Error message in red below field when `nameError` is not null
- Clear `nameError` on first keystroke after error is shown

### Description field
- Label: "Description"
- Optional — no `*`
- Multiline, min 3 lines, max 5 lines
- Placeholder: "What's this group about? Who should join?"
- Max 200 chars, character counter bottom-right

### Bottom spacing
- Extra `WindowInsets.navigationBars` padding so description field clears home indicator

---

## Phase 4 — Cover photo upload

Reuse `MediaRepository` from the `/media` skill — no new upload logic needed.

### Key difference from post images
- Post images: `posts/{userId}/{uuid}.jpg`
- Group cover: `groups/{tempId}/cover.jpg` where `tempId` is a client-generated UUID
- When group is created, the cover URL is passed in `CreateGroupRequest.coverImageUri` as the already-uploaded CDN URL

### Upload timing
- Upload cover photo as part of the submit flow, not eagerly on pick
- Show full-screen loading state (not just button loading) during upload + creation since both happen sequentially

---

## Phase 5 — Navigation

Add to `shared/commonMain/navigation/NavGraph.kt`:

```
main_graph
    ├── me (tab 0)
    │     ├── create_group (full-screen, pushed from Your Groups + Create a group)
    │     │     └── on success → group_management/{groupId}
    │     │         (popUpTo create_group inclusive=true)
    │     ├── group_management/{groupId}
    │     ├── create_post (full-screen)
    │     └── post_detail/{postId}
    ├── explore (tab 1)
    └── events (tab 2)
```

### Back stack after group creation
```
Before creation:   Me → CreateGroup
After creation:    Me → GroupManagement   (CreateGroup removed from stack)
Back pressed:      Me tab
```

Use `navController.navigate("group_management/${group.id}") { popUpTo("create_group") { inclusive = true } }`

---

## Phase 6 — Koin wiring

Add `CreateGroupViewModel` to `postModule` (or create a separate `groupModule`):
```kotlin
viewModel { CreateGroupViewModel(get(), get()) }  // GroupRepository, MediaRepository
```

---

## Phase 7 — Me tab hookup

In `MeScreen.kt` — Your Groups section:
- "＋ Create a group" button navigates to `create_group` route
- After returning from `GroupManagementScreen`, refresh the groups list in `MeViewModel`
- Use `LaunchedEffect(lifecycle)` or result handling to trigger `MeIntent.Load` on resume

---

## Implementation order
1. `Group`, `GroupMember`, `GroupRole`, `CreateGroupRequest` domain models (if not already present)
2. Update `GroupRepository` interface + `FakeGroupRepository`
3. `CreateGroupViewModel`
4. Koin wiring
5. `CreateGroupScreen` — cover photo section, name field, description field
6. Navigation — `create_group` route + back stack fix
7. Me tab hookup — wire "＋ Create a group" button + refresh on return

## What NOT to do
- Don't add a privacy toggle (invite-only vs open) — always default to invite-only, configurable in group settings v2
- Don't upload cover photo eagerly on pick — upload as part of submit flow only
- Don't disable the Create button — always enabled, show nameError on tap if invalid
- Don't navigate back to CreateGroupScreen after group is created — remove it from back stack
- Don't reuse the QuickCreateGroupSheet from create-post — this is a dedicated full screen with cover photo
- Don't show a discard confirmation dialog — fields are simple enough that accidental dismissal is not a concern
- Don't add member invitation to this screen — invite flow lives in GroupManagementScreen
EOF