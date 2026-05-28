---
name: create-post
description: Implement the Me tab (personal activity hub), create post screen, group management, attendance history, bottom nav, audience picker, location picker, and post submission flow for InIndy. Use when building or modifying the post creation experience, Me tab, or group management.
---

# InIndy Create Post + Me Tab

Implement the complete create post flow and Me tab for InIndy based on $ARGUMENTS.
Build against fake repositories — Supabase does not need to exist yet.

## Flow overview

```
User taps Me tab in bottom nav
    ↓
Me screen (personal activity hub)
    ├── [ + Create a post ] CTA → pushes Create Post full-screen
    ├── Your Posts — active + past, edit/delete/see RSVPs
    ├── Your Groups — manage members, invites, settings
    └── Attendance History — events attended, attendance rate
```

```
Taps "Create a post"
    ↓
Create Post screen (full-screen, single scrollable form)
    ↓
Fill: photos → description → location → date/time → audience → tags → max attendees
    ↓
Tap "Post"
    ↓
Validate → upload images (parallel) → create post → navigate back to Me tab
    ↓
New post appears in Your Posts section
```

---

## Phase 1 — Bottom nav

### Bottom nav structure
```
[ Me ] [ Explore ] [ Events ]
```

File: `shared/commonMain/ui/navigation/BottomNavBar.kt`
- Three tabs: `Me`, `Explore`, `Events` — all fully selectable
- Me tab: person icon — accent colored when selected
- Me is index 0 — leftmost tab
- `Explore` is center tab — default selected on first launch after onboarding

### Tab icons
- Me: `Icons.Rounded.Person`
- Explore: `Icons.Rounded.Explore`
- Events: `Icons.Rounded.CalendarMonth`

---

## Phase 2 — Me tab screen

File: `shared/commonMain/ui/me/MeScreen.kt`

### Layout — single scrollable column
```
┌─────────────────────────────┐
│  [Avatar] Your Name         │
│  Broad Ripple · ⭐ 92% attended │
├─────────────────────────────┤
│  [ + Create a post      ]   │  ← primary CTA, accent filled button full width
├─────────────────────────────┤
│  Your Posts          See all│
│  [PostManageCard]           │
│  [PostManageCard]           │
├─────────────────────────────┤
│  Your Groups         See all│
│  [GroupCard]                │
│  [GroupCard]                │
│  [ + Create a group     ]   │
├─────────────────────────────┤
│  Attendance History         │
│  [AttendanceCard]           │
│  [AttendanceCard]           │
└─────────────────────────────┘
```

### Header
- Circular avatar (56dp) + display name + neighborhood name
- Attendance rate shown as "⭐ 92% attended" — derived from `user_stats`
- Tap avatar → future profile edit screen (v2)

### Your Posts section
- Show 3 most recent posts, "See all" navigates to full list
- Each `PostManageCard` shows: thumbnail, description truncated, date, RSVP count, status chip (Upcoming / Past)
- Swipe to delete or tap → post detail with Edit / Delete / View RSVPs options
- Empty state: "You haven't posted anything yet" + nudge toward Create CTA

### Your Groups section
- Show all groups (admin or member), "See all" if more than 3
- Each `GroupCard` shows: cover photo or placeholder, group name, member count, role chip (Admin / Member)
- Tap → `GroupManagementScreen`
- "＋ Create a group" button at bottom of section
- Empty state: "Create a group for your crew"

### Attendance History section
- Show 5 most recent attended events, chronological descending
- Each `AttendanceCard` shows: post thumbnail, title, host name, date attended
- Empty state: "Events you attend will show up here"
- No "See all" for MVP — show last 5 only

---

## Phase 3 — Group management screen

File: `shared/commonMain/ui/me/GroupManagementScreen.kt`

```
┌─────────────────────────────┐
│ ← [Group Name]      ⋮ menu  │
├─────────────────────────────┤
│  [Cover photo]              │
│  Description                │
├─────────────────────────────┤
│  Members (4)                │
│  [Avatar] Name   Admin  ×   │
│  [Avatar] Name          ×   │
├─────────────────────────────┤
│  Pending invites (2)        │
│  link@email.com         ×   │
├─────────────────────────────┤
│  [ + Invite members     ]   │
└─────────────────────────────┘
```
- Only admins see remove (×) and invite controls
- Members see read-only view + leave group option
- ⋮ menu (admin only): Edit group info / Delete group
- "Invite members" → share an invite link (generated via `group_invites.token`)
- Delete group: confirmation dialog before proceeding

---

## Phase 4 — Domain layer

### Post domain model
File: `shared/commonMain/domain/model/Post.kt`
```kotlin
data class Post(
    val id: String,
    val userId: String,
    val groupId: String?,
    val neighborhoodId: String,
    val description: String,
    val location: LatLng,
    val address: String,
    val startsAt: Instant,
    val endsAt: Instant?,
    val maxAttendees: Int?,
    val tags: List<PostTag>,
    val images: List<String>,
    val rsvpCount: Int,
    val createdAt: Instant
)

enum class PostTag(val displayName: String) {
    HIKE("Hike"), RUN("Run"), PICNIC("Picnic"),
    SPORT("Sport"), WALK("Walk"), EXPLORE("Explore"), OTHER("Other")
}
```

### Attendance model
File: `shared/commonMain/domain/model/AttendanceRecord.kt`
```kotlin
data class AttendanceRecord(
    val postId: String,
    val postDescription: String,
    val postImageUrl: String?,
    val hostName: String,
    val attendedAt: Instant
)
```

### Repository interfaces
File: `shared/commonMain/domain/repository/PostRepository.kt`
```kotlin
interface PostRepository {
    suspend fun createPost(request: CreatePostRequest): Result<Post>
    suspend fun getUserPosts(): Result<List<Post>>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun getNeighborhoodFeed(neighborhoodId: String): Result<List<Post>>
    suspend fun getGroupFeed(groupId: String): Result<List<Post>>
}
```

File: `shared/commonMain/domain/repository/GroupRepository.kt`
```kotlin
interface GroupRepository {
    suspend fun getUserGroups(): Result<List<Group>>
    suspend fun createGroup(name: String, description: String?): Result<Group>
    suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>>
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
    suspend fun generateInviteLink(groupId: String): Result<String>
    suspend fun deleteGroup(groupId: String): Result<Unit>
}
```

File: `shared/commonMain/domain/repository/AttendanceRepository.kt`
```kotlin
interface AttendanceRepository {
    suspend fun getAttendanceHistory(limit: Int = 5): Result<List<AttendanceRecord>>
    suspend fun getAttendanceRate(): Result<Float>
}
```

### Fake repositories
- `FakePostRepository` — `getUserPosts()` returns 3 mock posts (2 upcoming, 1 past)
- `FakeGroupRepository` — `getUserGroups()` returns 2 mock groups
- `FakeAttendanceRepository` — returns 5 mock attendance records, rate = 0.92f

---

## Phase 5 — ViewModels

### MeViewModel
File: `shared/commonMain/presentation/me/MeViewModel.kt`

```kotlin
data class MeUiState(
    val user: User? = null,
    val recentPosts: List<Post> = emptyList(),
    val groups: List<Group> = emptyList(),
    val attendanceHistory: List<AttendanceRecord> = emptyList(),
    val attendanceRate: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class MeIntent {
    object Load : MeIntent()
    data class DeletePost(val postId: String) : MeIntent()
    data class CreateGroup(val name: String, val description: String?) : MeIntent()
    object NavigateToCreatePost : MeIntent()
}
```

### CreatePostViewModel
File: `shared/commonMain/presentation/createpost/CreatePostViewModel.kt`

```kotlin
data class CreatePostUiState(
    val images: List<String> = emptyList(),
    val title: String = "",
    val description: String = "",
    val location: LatLng? = null,
    val address: String = "",
    val addressSuggestions: List<AddressResult> = emptyList(),
    val startsAt: Instant? = null,
    val endsAt: Instant? = null,
    val audience: PostAudience = PostAudience.Neighborhood,
    val userGroups: List<Group> = emptyList(),
    val tags: Set<PostTag> = emptySet(),
    val maxAttendees: Int? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val locationLoading: Boolean = false,
    // Per-field validation errors — null means no error shown
    val imagesError: String? = null,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val addressError: String? = null,
    val startsAtError: String? = null
)

// Computed property — never stored in state
val CreatePostUiState.isSubmitEnabled: Boolean
get() = images.isNotEmpty()
        && title.length >= 3
        && description.length >= 10
        && address.isNotBlank()
        && startsAt != null
        && !isSubmitting

sealed class PostAudience {
    object Neighborhood : PostAudience()
    data class Group(val groupId: String) : PostAudience()
}

sealed class CreatePostIntent {
    data class AddImage(val uri: String) : CreatePostIntent()
    data class RemoveImage(val uri: String) : CreatePostIntent()
    data class TitleChanged(val text: String) : CreatePostIntent()
    data class DescriptionChanged(val text: String) : CreatePostIntent()
    object UseCurrentLocation : CreatePostIntent()
    data class AddressQueryChanged(val query: String) : CreatePostIntent()
    data class SelectAddress(val result: AddressResult) : CreatePostIntent()
    data class StartsAtChanged(val instant: Instant) : CreatePostIntent()
    data class EndsAtChanged(val instant: Instant?) : CreatePostIntent()
    object SelectNeighborhoodAudience : CreatePostIntent()
    data class SelectGroupAudience(val groupId: String) : CreatePostIntent()
    data class ToggleTag(val tag: PostTag) : CreatePostIntent()
    data class MaxAttendeesChanged(val count: Int?) : CreatePostIntent()
    data class QuickCreateGroup(val name: String, val description: String?) : CreatePostIntent()
    object Submit : CreatePostIntent()
}
```

### Post button behavior
- "Post" button is ALWAYS enabled — never disabled
- On tap: run validation, populate error fields, scroll to first error if any
- Only proceed with upload + submit if `isSubmitEnabled` is true

### Validation on submit
```kotlin
// On Submit intent — validate all fields, show errors, only proceed if all pass
fun onSubmit() {
    val errors = buildErrors()
    if (errors.hasAny()) {
        _state.update { it.copy(
            imagesError = errors.imagesError,
            titleError = errors.titleError,
            descriptionError = errors.descriptionError,
            addressError = errors.addressError,
            startsAtError = errors.startsAtError
        )}
        return // stop here — show errors, do not submit
    }
    // proceed with upload + post creation
}
```

### Field error messages
- `imagesError`: "Add at least one photo"
- `titleError`: "Title must be at least 3 characters"
- `descriptionError`: "Description must be at least 10 characters"
- `addressError`: "Please set a location"
- `startsAtError`: "Please set a start date and time"

### Error display in UI
- Show error message in red directly below the relevant field
- Clear a field's error as soon as the user edits that field
- On successful submit, clear all errors

### Submit logic
```kotlin
// 1. Validate → show errors and return if invalid
// 2. isSubmitting = true
// 3. Upload images in parallel via MediaRepository
// 4. Any upload fails → submitError, isSubmitting = false, return
// 5. createPost with CDN URLs
// 6. Success → pop back to Me tab
```

---

## Phase 6 — Location expect/actual

File: `shared/commonMain/data/location/LocationProvider.kt`
```kotlin
data class LatLng(val lat: Double, val lng: Double)

expect class LocationProvider {
    suspend fun getCurrentLocation(): LatLng?
    suspend fun reverseGeocode(latLng: LatLng): String?
}
```

File: `shared/commonMain/data/location/AddressSearchDataSource.kt`
```kotlin
interface AddressSearchDataSource {
    suspend fun search(query: String): Result<List<AddressResult>>
}
data class AddressResult(val address: String, val latLng: LatLng)
```
- Android actual: `FusedLocationProviderClient` + `ACCESS_FINE_LOCATION`
- iOS actual: `CLLocationManager` + `WhenInUse` authorization
- Fake: returns 3 hardcoded Indianapolis addresses

---

## Phase 7 — Create post screen

File: `shared/commonMain/ui/createpost/CreatePostScreen.kt`

Apply the `/design` skill. Warm, personal, encouraging — not a cold form.

### Top bar
- Left: ✕ close — discard confirmation dialog if any fields filled
- Right: "Post" filled accent button — disabled until `isSubmitEnabled`

### Sections in order
1. **Photos** — horizontal row, 80dp thumbnails, `+` hidden after 3, shimmer progress per image
2. **Description** — multiline field, placeholder "What's happening?", 280 char max with counter
3. **Location** — "Use my location" chip + address search field + address suggestion dropdown + mini map preview (160dp, non-interactive, rounded)
4. **Date & Time** — starts/ends chips, native date+time pickers, future date validation
5. **Audience** — neighborhood (default) vs group radio, group dropdown, "＋ Create new group" → `QuickCreateGroupSheet`
6. **Tags** — wrapping chip row, max 3 selected, disable unselected after 3
7. **Max attendees** — optional stepper [ − ] [ 4 ] [ + ], "No limit" toggle

---

## Phase 8 — Group quick-create bottom sheet

File: `shared/commonMain/ui/createpost/QuickCreateGroupSheet.kt`
- `ModalBottomSheet`, `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`
- Name (required, min 3 chars) + description (optional)
- Create button disabled until name valid
- On success: dismiss sheet, auto-select new group in audience picker

---

## Phase 9 — Discard confirmation

Show dialog when ✕ tapped and any field is filled:
- Title: "Discard post?"
- Body: "You'll lose what you've written."
- Confirm: "Discard" (destructive)
- Dismiss: "Keep editing"

---

## Phase 10 — Koin wiring

File: `shared/commonMain/di/PostModule.kt`
```kotlin
val postModule = module {
    single<PostRepository> { FakePostRepository() }
    single<GroupRepository> { FakeGroupRepository() }
    single<AttendanceRepository> { FakeAttendanceRepository() }
    single<AddressSearchDataSource> { FakeAddressSearchDataSource() }
    single { LocationProvider() }
    viewModel { MeViewModel(get(), get(), get()) }
    viewModel { CreatePostViewModel(get(), get(), get(), get()) }
}
```

---

## Navigation

```
main_graph
    ├── me (tab 0 — Me screen)
    │     ├── create_post (full-screen, pushed from Me CTA)
    │     ├── post_detail/{postId} (pushed from post card tap in Your Posts)
    │     └── group_management/{groupId} (pushed from group card tap)
    ├── explore (tab 1 — default selected on first launch)
    │     └── post_detail/{postId} (pushed from post card tap in feed)
    └── events (tab 2)
```

### PostDetailScreen
File: `shared/commonMain/ui/post/PostDetailScreen.kt`
- Navigated to from any post card tap — in Me tab (Your Posts) or Explore feed
- Route: `post_detail/{postId}` — load post by id from `PostRepository`
- Displays: full photo carousel, host avatar + name, description, location + mini map, date/time, tags, RSVP list, max attendees
- If current user is the host: show Edit and Delete options in top bar ⋮ menu
- If current user is not the host: show "I'm in" / "Cancel RSVP" button
- Add `PostDetailViewModel` with `PostDetailUiState` and `PostDetailIntent`
- Add `getPostById(postId: String): Result<Post>` to `PostRepository` interface

---

## Implementation order
1. Domain models — `Post`, `PostTag`, `AttendanceRecord`, `CreatePostRequest`, `PostAudience`
2. Repository interfaces — `PostRepository` (include `getPostById`), `GroupRepository`, `AttendanceRepository`
3. Fake repositories
4. `LocationProvider` expect/actual + `FakeAddressSearchDataSource`
5. Koin `postModule`
6. `MeViewModel` + `CreatePostViewModel` + `PostDetailViewModel`
7. Bottom nav — `Me`, `Explore`, `Events`
8. `MeScreen` — all sections
9. `CreatePostScreen` — sections in order
10. `QuickCreateGroupSheet`
11. `GroupManagementScreen`
12. `PostDetailScreen`
13. Discard confirmation dialog
14. Navigation hookup

## What NOT to do
- Don't intercept the Me tab tap — it's a real selectable tab now
- Don't block the entire form while images upload — show per-image progress
- Don't submit if any image upload failed — surface error, let user retry
- Don't skip the future date validation on starts_at
- Don't disable the Post button — always enabled, show per-field errors on tap instead
- Don't allow more than 3 tags or 3 images — enforce in UI
- Don't navigate away on submit without waiting for image uploads to complete
- Don't show group management controls to non-admin members
- Don't implement draft posts or push notifications — deferred to v2