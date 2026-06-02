---
name: explore-filter
description: Implement the Explore feed filter — dropdown with neighborhood and group options, group search bottom sheet, and dynamic BrandMark text update. Use when building or modifying the Explore screen filter or BrandMark header.
---

# InIndy Explore Filter

Implement the Explore feed filter and dynamic BrandMark for InIndy based on $ARGUMENTS.

## Feature overview

```
ExploreScreen top bar
    ├── [BrandMark ▾] ← tap arrow to open filter dropdown
    └── filter changes feed + BrandMark text instantly

Filter options:
    ├── InIndy (default) — all neighborhood + group posts
    ├── In{Neighborhood} — user's neighborhood posts only
    └── Search Groups → bottom sheet → select group → In{GroupName} feed
```

---

## Phase 1 — Filter domain model

File: `shared/commonMain/domain/model/ExploreFilter.kt`
```kotlin
sealed class ExploreFilter {
    object All : ExploreFilter()                          // InIndy — default
    object Neighborhood : ExploreFilter()                 // user's neighborhood
    data class Group(
        val groupId: String,
        val groupName: String
    ) : ExploreFilter()
}

// Converts filter to BrandMark display text
fun ExploreFilter.toBrandMarkText(neighborhoodName: String): String =
    when (this) {
        is ExploreFilter.All -> "InIndy"
        is ExploreFilter.Neighborhood -> "In${neighborhoodName.toFilterLabel()}"
        is ExploreFilter.Group -> "In${groupName.toFilterLabel()}"
    }

// CamelCase conversion — strip non-alphanumeric, capitalize each word, prefix "In"
fun String.toFilterLabel(): String =
    this.split(" ")
        .filter { it.isNotBlank() }
        .joinToString("") { word ->
            word.filter { it.isLetterOrDigit() }
                .replaceFirstChar { it.uppercaseChar() }
        }

// Examples:
// "Broad Ripple" → "BroadRipple" → full result: "InBroadRipple"
// "Way Street Runners" → "WayStreetRunners" → full result: "InWayStreetRunners"
// "St. John's Crew" → "StJohnsCrew" → full result: "InStJohnsCrew"
```

---

## Phase 2 — Group search data

File: `shared/commonMain/domain/repository/GroupRepository.kt`
Add to existing interface:
```kotlin
suspend fun searchGroups(query: String): Result<List<Group>>
suspend fun getUserGroups(): Result<List<Group>>   // already present — used for "Your Groups" section
```

File: `shared/commonMain/data/repository/FakeGroupRepository.kt`
```kotlin
override suspend fun searchGroups(query: String): Result<List<Group>> {
    delay(300)
    val allGroups = listOf(
        Group("g1", "Broad Ripple Runners", null, null, "uid", false, 12, Clock.System.now()),
        Group("g2", "Sunday Hikers", null, null, "uid", false, 8, Clock.System.now()),
        Group("g3", "Indy Cyclists", null, null, "uid", false, 24, Clock.System.now()),
        Group("g4", "Fountain Square Crew", null, null, "uid", false, 6, Clock.System.now()),
    )
    return Result.success(
        if (query.isBlank()) allGroups
        else allGroups.filter { it.name.contains(query, ignoreCase = true) }
    )
}
```

---

## Phase 3 — ExploreViewModel updates

File: `shared/commonMain/presentation/explore/ExploreViewModel.kt`

### Add to ExploreUiState
```kotlin
data class ExploreUiState(
    // existing fields...
    val activeFilter: ExploreFilter = ExploreFilter.All,
    val brandMarkText: String = "InIndy",
    val isFilterDropdownVisible: Boolean = false,
    val isGroupSearchSheetVisible: Boolean = false,
    // Group search sheet state
    val groupSearchQuery: String = "",
    val userGroups: List<Group> = emptyList(),
    val searchedGroups: List<Group> = emptyList(),
    val isSearchingGroups: Boolean = false
)
```

### Add to ExploreIntent
```kotlin
sealed class ExploreIntent {
    // existing intents...
    object ToggleFilterDropdown : ExploreIntent()
    object DismissFilterDropdown : ExploreIntent()
    object SelectFilterAll : ExploreIntent()
    object SelectFilterNeighborhood : ExploreIntent()
    object OpenGroupSearch : ExploreIntent()
    object DismissGroupSearch : ExploreIntent()
    data class GroupSearchQueryChanged(val query: String) : ExploreIntent()
    data class SelectFilterGroup(val group: Group) : ExploreIntent()
}
```

### Filter logic in ViewModel
```kotlin
// On SelectFilterAll:
_state.update { it.copy(
    activeFilter = ExploreFilter.All,
    brandMarkText = "InIndy",
    isFilterDropdownVisible = false
)}
loadFeed() // reload with no filter

// On SelectFilterNeighborhood:
_state.update { it.copy(
    activeFilter = ExploreFilter.Neighborhood,
    brandMarkText = ExploreFilter.Neighborhood.toBrandMarkText(user.neighborhoodName),
    isFilterDropdownVisible = false
)}
loadFeed() // reload filtered by neighborhoodId

// On SelectFilterGroup:
_state.update { it.copy(
    activeFilter = ExploreFilter.Group(group.id, group.name),
    brandMarkText = ExploreFilter.Group(group.id, group.name).toBrandMarkText(""),
    isFilterDropdownVisible = false,
    isGroupSearchSheetVisible = false
)}
loadFeed() // reload filtered by groupId

// On GroupSearchQueryChanged — debounce 300ms then call searchGroups
```

### Feed loading by filter
```kotlin
private fun loadFeed() {
    when (val filter = state.value.activeFilter) {
        is ExploreFilter.All -> postRepository.getNeighborhoodFeed(user.neighborhoodId)
            // + getGroupFeed for all user's groups, merged and sorted by createdAt
        is ExploreFilter.Neighborhood -> postRepository.getNeighborhoodFeed(user.neighborhoodId)
        is ExploreFilter.Group -> postRepository.getGroupFeed(filter.groupId)
    }
}
```

---

## Phase 4 — BrandMark with filter arrow

File: `shared/commonMain/ui/explore/FilterableBrandMark.kt`

```kotlin
@Composable
fun FilterableBrandMark(
    text: String,
    onArrowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // BrandMark text — ellipsis on overflow
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false) // shrinks before pushing arrow off screen
        )

        Spacer(Modifier.width(4.dp))

        // Down arrow — tappable
        IconButton(
            onClick = onArrowClick,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Filter feed",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

### Animate arrow rotation when dropdown is open
```kotlin
val arrowRotation by animateFloatAsState(
    targetValue = if (isFilterDropdownVisible) 180f else 0f,
    animationSpec = tween(200),
    label = "arrowRotation"
)

Icon(
    imageVector = Icons.Rounded.KeyboardArrowDown,
    modifier = Modifier.rotate(arrowRotation),
    ...
)
```

### Ellipsis behavior
- `Modifier.weight(1f, fill = false)` on the Text — lets it shrink but never grow past available space
- Arrow icon always visible — never pushed off screen regardless of text length
- Max width of the entire `FilterableBrandMark` should be constrained to ~60% of screen width in the top bar

---

## Phase 5 — Filter dropdown

File: `shared/commonMain/ui/explore/FilterDropdown.kt`

```kotlin
@Composable
fun FilterDropdown(
    activeFilter: ExploreFilter,
    neighborhoodName: String,
    onSelectAll: () -> Unit,
    onSelectNeighborhood: () -> Unit,
    onSearchGroups: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```

### Using DropdownMenu
```kotlin
DropdownMenu(
    expanded = isVisible,
    onDismissRequest = onDismiss,
    modifier = modifier
) {
    // Option 1 — InIndy (default)
    DropdownMenuItem(
        text = {
            Row {
                Text("InIndy")
                if (activeFilter is ExploreFilter.All) {
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.Check, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        onClick = onSelectAll
    )

    // Option 2 — In{Neighborhood}
    DropdownMenuItem(
        text = {
            Row {
                Text("In${neighborhoodName.toFilterLabel()}")
                if (activeFilter is ExploreFilter.Neighborhood) {
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.Check, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        onClick = onSelectNeighborhood
    )

    HorizontalDivider()

    // Option 3 — Search Groups
    DropdownMenuItem(
        text = { Text("Search Groups") },
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null)
        },
        onClick = onSearchGroups
    )
}
```

### Dropdown behavior
- Appears anchored below the ▾ arrow
- Tapping outside dismisses via `onDismissRequest`
- Active filter shown with a checkmark — only one at a time
- "Search Groups" has no checkmark — it's an action, not a state

---

## Phase 6 — Group search bottom sheet

File: `shared/commonMain/ui/explore/GroupSearchSheet.kt`

```kotlin
@Composable
fun GroupSearchSheet(
    query: String,
    userGroups: List<Group>,
    searchedGroups: List<Group>,
    isSearching: Boolean,
    onQueryChanged: (String) -> Unit,
    onGroupSelected: (Group) -> Unit,
    onDismiss: () -> Unit
)
```

### Layout
```
┌─────────────────────────────┐
│  ▬▬▬  (drag handle)         │
│  Find a group          ✕    │
│  [🔍 Search groups...    ]  │
│  ─────────────────────────  │
│  YOUR GROUPS          label │
│  [GroupRow]                 │
│  [GroupRow]                 │
│  ─────────────────────────  │
│  ALL GROUPS           label │
│  [GroupRow]                 │
│  [GroupRow]                 │
│  (loading shimmer if isSearching)│
└─────────────────────────────┘
```

### Section labels
```kotlin
// Section header style — uppercase, small, tertiary color
Text(
    text = "YOUR GROUPS",
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    letterSpacing = 0.08.em,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
)
```

### GroupRow composable
```kotlin
@Composable
fun GroupRow(
    group: Group,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = group.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = { Text("${group.memberCount} members") },
        leadingContent = {
            // Group cover thumbnail or placeholder
            AsyncImage(
                model = group.coverUrl?.let { "$it?width=80&height=80&fit=cover" },
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
            )
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}
```

### Sheet behavior
- `ModalBottomSheet` with drag handle
- Search field auto-focused on open — keyboard appears immediately
- If query is blank: show YOUR GROUPS section only (no ALL GROUPS)
- If query is not blank: show both sections — YOUR GROUPS filtered by query, ALL GROUPS from `searchGroups(query)`
- YOUR GROUPS section hidden if user has no groups matching query
- ALL GROUPS section excludes groups already shown in YOUR GROUPS (deduplicate by id)
- Selecting a group: dismiss sheet, apply filter instantly, update BrandMark
- isSearching shows shimmer in ALL GROUPS section while debounced search is in flight

---

## Phase 7 — ExploreScreen integration

File: `shared/commonMain/ui/explore/ExploreScreen.kt`

### Top bar
```kotlin
TopAppBar(
    title = {
        FilterableBrandMark(
            text = uiState.brandMarkText,
            onArrowClick = { vm.onIntent(ExploreIntent.ToggleFilterDropdown) },
            modifier = Modifier.widthIn(max = (screenWidth * 0.6f).dp)
        )
    },
    actions = { /* search icon etc */ }
)

// Dropdown anchored to title area
FilterDropdown(
    activeFilter = uiState.activeFilter,
    neighborhoodName = uiState.user?.neighborhoodName ?: "",
    isVisible = uiState.isFilterDropdownVisible,
    onSelectAll = { vm.onIntent(ExploreIntent.SelectFilterAll) },
    onSelectNeighborhood = { vm.onIntent(ExploreIntent.SelectFilterNeighborhood) },
    onSearchGroups = { vm.onIntent(ExploreIntent.OpenGroupSearch) },
    onDismiss = { vm.onIntent(ExploreIntent.DismissFilterDropdown) }
)

// Group search sheet
if (uiState.isGroupSearchSheetVisible) {
    GroupSearchSheet(
        query = uiState.groupSearchQuery,
        userGroups = uiState.userGroups,
        searchedGroups = uiState.searchedGroups,
        isSearching = uiState.isSearchingGroups,
        onQueryChanged = { vm.onIntent(ExploreIntent.GroupSearchQueryChanged(it)) },
        onGroupSelected = { vm.onIntent(ExploreIntent.SelectFilterGroup(it)) },
        onDismiss = { vm.onIntent(ExploreIntent.DismissGroupSearch) }
    )
}
```

### BrandMark text animation
Animate text change with `AnimatedContent`:
```kotlin
AnimatedContent(
    targetState = uiState.brandMarkText,
    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
    label = "brandMarkText"
) { text ->
    FilterableBrandMark(text = text, ...)
}
```

---

## Phase 8 — Fake data for development

### FakePostRepository — full dummy data set
File: `shared/commonMain/data/repository/FakePostRepository.kt`

Provide richly varied dummy posts so filter switching produces visibly different feeds.
Each post must have realistic Indianapolis locations, descriptions, and tags.

```kotlin
// Shared mock post pool — used across all feed methods
private val allMockPosts = listOf(
    Post(
        id = "p1", userId = "u1", groupId = null,
        neighborhoodId = "broad_ripple",
        description = "Morning trail run along the Monon, easy pace welcome!",
        location = LatLng(39.8676, -86.1431),
        address = "Monon Trail, Broad Ripple, Indianapolis",
        startsAt = Clock.System.now().plus(2.hours),
        endsAt = Clock.System.now().plus(3.hours),
        maxAttendees = 8, tags = listOf(PostTag.RUN),
        images = listOf("https://picsum.photos/seed/run1/800/600"),
        rsvpCount = 3, createdAt = Clock.System.now().minus(20.minutes)
    ),
    Post(
        id = "p2", userId = "u2", groupId = null,
        neighborhoodId = "fountain_square",
        description = "Sunday picnic at Garfield Park, bring a dish to share",
        location = LatLng(39.7447, -86.1358),
        address = "Garfield Park, Indianapolis",
        startsAt = Clock.System.now().plus(1.days),
        endsAt = Clock.System.now().plus(1.days).plus(3.hours),
        maxAttendees = 20, tags = listOf(PostTag.PICNIC),
        images = listOf("https://picsum.photos/seed/picnic1/800/600"),
        rsvpCount = 7, createdAt = Clock.System.now().minus(1.hours)
    ),
    Post(
        id = "p3", userId = "u3", groupId = "g1",
        neighborhoodId = "broad_ripple",
        description = "Group ride along White River, 15 miles at a chill pace",
        location = LatLng(39.8512, -86.1674),
        address = "White River State Park, Indianapolis",
        startsAt = Clock.System.now().plus(3.hours),
        endsAt = null,
        maxAttendees = 6, tags = listOf(PostTag.EXPLORE),
        images = listOf("https://picsum.photos/seed/bike1/800/600"),
        rsvpCount = 2, createdAt = Clock.System.now().minus(45.minutes)
    ),
    Post(
        id = "p4", userId = "u4", groupId = "g2",
        neighborhoodId = "irvington",
        description = "Pickup basketball at Irving Circle Park, all skill levels",
        location = LatLng(39.7689, -86.0531),
        address = "Irving Circle Park, Irvington, Indianapolis",
        startsAt = Clock.System.now().plus(4.hours),
        endsAt = Clock.System.now().plus(6.hours),
        maxAttendees = 12, tags = listOf(PostTag.SPORT),
        images = listOf("https://picsum.photos/seed/bball1/800/600"),
        rsvpCount = 5, createdAt = Clock.System.now().minus(30.minutes)
    ),
    Post(
        id = "p5", userId = "u5", groupId = null,
        neighborhoodId = "broad_ripple",
        description = "Sunset yoga at Holliday Park, bring a mat",
        location = LatLng(39.8723, -86.1612),
        address = "Holliday Park, Indianapolis",
        startsAt = Clock.System.now().plus(5.hours),
        endsAt = Clock.System.now().plus(6.hours),
        maxAttendees = null, tags = listOf(PostTag.WALK),
        images = listOf("https://picsum.photos/seed/yoga1/800/600"),
        rsvpCount = 9, createdAt = Clock.System.now().minus(2.hours)
    ),
    Post(
        id = "p6", userId = "u6", groupId = "g1",
        neighborhoodId = "broad_ripple",
        description = "Way Street Runners weekly long run — 8 miles this week",
        location = LatLng(39.8676, -86.1431),
        address = "Monon Trail at Broad Ripple Ave, Indianapolis",
        startsAt = Clock.System.now().plus(1.days).plus(7.hours),
        endsAt = null,
        maxAttendees = 10, tags = listOf(PostTag.RUN),
        images = listOf("https://picsum.photos/seed/run2/800/600"),
        rsvpCount = 6, createdAt = Clock.System.now().minus(3.hours)
    ),
    Post(
        id = "p7", userId = "u7", groupId = null,
        neighborhoodId = "irvington",
        description = "Dog walk around Ellenberger Park, all breeds welcome",
        location = LatLng(39.7712, -86.0612),
        address = "Ellenberger Park, Irvington, Indianapolis",
        startsAt = Clock.System.now().plus(6.hours),
        endsAt = null,
        maxAttendees = null, tags = listOf(PostTag.WALK),
        images = listOf("https://picsum.photos/seed/dogwalk1/800/600"),
        rsvpCount = 4, createdAt = Clock.System.now().minus(1.5.hours)
    )
)

// ALL posts — neighborhood + group (InIndy default filter)
override suspend fun getNeighborhoodFeed(neighborhoodId: String): Result<List<Post>> {
    delay(400)
    return Result.success(allMockPosts.sortedByDescending { it.createdAt })
}

// NEIGHBORHOOD filter — only posts matching neighborhoodId, no group posts
override suspend fun getNeighborhoodOnlyFeed(neighborhoodId: String): Result<List<Post>> {
    delay(400)
    return Result.success(
        allMockPosts
            .filter { it.neighborhoodId == neighborhoodId && it.groupId == null }
            .sortedByDescending { it.createdAt }
    )
}

// GROUP filter — only posts belonging to the selected group
override suspend fun getGroupFeed(groupId: String): Result<List<Post>> {
    delay(400)
    return Result.success(
        allMockPosts
            .filter { it.groupId == groupId }
            .sortedByDescending { it.createdAt }
    )
}

override suspend fun getUserPosts(): Result<List<Post>> {
    delay(300)
    return Result.success(allMockPosts.filter { it.userId == "u1" })
}
```

### Dummy neighborhood ID
Use `"broad_ripple"` as the current user's `neighborhoodId` in `FakeAuthRepository`.
This means:
- `ExploreFilter.Neighborhood` shows posts where `neighborhoodId == "broad_ripple"` and `groupId == null`
- Posts p1, p5 match — demonstrably different from the full InIndy feed

### Dummy group IDs
- `"g1"` = Way Street Runners — posts p3, p6
- `"g2"` = Irvington Ballers — post p4
  These are referenced in `FakeGroupRepository` so group selection produces real filtered results.

---

## Phase 9 — Feed wiring on filter switch

### ExploreViewModel — full filter → feed reload logic
```kotlin
private fun applyFilter(filter: ExploreFilter) {
    _state.update { it.copy(
        activeFilter = filter,
        brandMarkText = filter.toBrandMarkText(
            neighborhoodName = currentUser?.neighborhoodName ?: "Neighborhood"
        ),
        isFilterDropdownVisible = false,
        isGroupSearchSheetVisible = false,
        posts = emptyList(),    // clear current feed immediately
        isLoading = true
    )}
    loadFeed()
}

private fun loadFeed() {
    viewModelScope.launch {
        val result = when (val filter = state.value.activeFilter) {
            is ExploreFilter.All ->
                postRepository.getNeighborhoodFeed(currentUser?.neighborhoodId ?: "")
            is ExploreFilter.Neighborhood ->
                postRepository.getNeighborhoodOnlyFeed(currentUser?.neighborhoodId ?: "")
            is ExploreFilter.Group ->
                postRepository.getGroupFeed(filter.groupId)
        }
        result.fold(
            onSuccess = { posts ->
                _state.update { it.copy(posts = posts, isLoading = false, error = null) }
            },
            onFailure = { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        )
    }
}
```

### ExploreScreen — feed list reacts to state
The `LazyColumn` (or `LazyVerticalGrid`) rendering posts must:
- Observe `uiState.posts` — recomposes automatically when filter changes
- Show shimmer skeleton when `uiState.isLoading` is true
- Show empty state when `uiState.posts` is empty and `!uiState.isLoading`
- Empty state message varies by filter:

```kotlin
val emptyMessage = when (uiState.activeFilter) {
    is ExploreFilter.All -> "Nothing happening nearby yet — be the first to post!"
    is ExploreFilter.Neighborhood -> "No posts in your neighborhood yet"
    is ExploreFilter.Group -> "No posts in this group yet"
}
```

### Feed transition animation
Animate the feed list swap when filter changes:
```kotlin
AnimatedContent(
    targetState = uiState.activeFilter,
    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
    label = "feedTransition"
) { filter ->
    when {
        uiState.isLoading -> ShimmerPostFeed()
        uiState.posts.isEmpty() -> EmptyFeedState(message = emptyMessage)
        else -> PostFeed(posts = uiState.posts, onPostClick = { ... })
    }
}
```

### PostRepository interface — add getNeighborhoodOnlyFeed
```kotlin
interface PostRepository {
    suspend fun getNeighborhoodFeed(neighborhoodId: String): Result<List<Post>>       // All — hood + groups
    suspend fun getNeighborhoodOnlyFeed(neighborhoodId: String): Result<List<Post>>   // Neighborhood filter only
    suspend fun getGroupFeed(groupId: String): Result<List<Post>>                     // Group filter
    suspend fun createPost(request: CreatePostRequest): Result<Post>
    suspend fun getUserPosts(): Result<List<Post>>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun getPostById(postId: String): Result<Post>
}
```

---

## Implementation order
1. `ExploreFilter` sealed class + `toBrandMarkText()` + `toFilterLabel()` extension
2. Add `getNeighborhoodOnlyFeed()` to `PostRepository` interface + `FakePostRepository`
3. Add `searchGroups()` to `GroupRepository` interface + `FakeGroupRepository`
4. Update `FakePostRepository` with full dummy data set (7 posts across neighborhoods + groups)
5. Update `ExploreUiState` + `ExploreIntent` + `ExploreViewModel` — `applyFilter()` + `loadFeed()`
6. `FilterableBrandMark` composable with arrow rotation animation
7. `FilterDropdown` composable
8. `GroupRow` composable
9. `GroupSearchSheet` composable
10. Wire everything into `ExploreScreen` — feed list, empty states, shimmer, `AnimatedContent` transitions
11. `AnimatedContent` for BrandMark text transitions

## What NOT to do
- Don't disable the ▾ arrow while the feed is loading — always tappable
- Don't show ALL GROUPS when search query is blank — only YOUR GROUPS
- Don't include groups from YOUR GROUPS in ALL GROUPS — deduplicate by id
- Don't animate the BrandMark text with a slide — use fade only, slide feels jarring for a header
- Don't allow the arrow icon to be pushed off screen by long group names — use `Modifier.weight(1f, fill = false)` on Text
- Don't hardcode neighborhood name — always read from user state
- Don't skip the 300ms debounce on group search — avoid hammering the API on every keystroke
- Don't store `brandMarkText` as a separate field that could get out of sync — derive it from `activeFilter` in the ViewModel and store both
  EOF