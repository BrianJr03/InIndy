---
name: group-management
description: Implement the GroupManagementScreen for InIndy — group header, At a Glance latest posts section, members list, and invite management. Use when building or modifying the group management experience.
---

# InIndy Group Management Screen

Implement the full `GroupManagementScreen` for InIndy based on $ARGUMENTS.

## Screen overview

```
┌─────────────────────────────┐
│ ←  [Group Name]      ⋮ menu │
├─────────────────────────────┤
│  [Cover photo or placeholder│
│   full width, 200dp height] │
│                             │
│  Group Name                 │
│  Description (if set)       │
│  👥 12 members              │
├─────────────────────────────┤
│  At a Glance                │
│  [CompactPostCard]          │
│  [CompactPostCard]          │
│  [CompactPostCard]          │
│  (empty state if no posts)  │
├─────────────────────────────┤
│  Members (4)                │
│  [MemberRow] Name  Admin ×  │
│  [MemberRow] Name        ×  │
│  [MemberRow] Name        ×  │
├─────────────────────────────┤
│  Pending Invites (2)        │
│  [InviteRow] token/email  × │
│  [ + Invite members     ]   │
└─────────────────────────────┘
```

---

## Phase 1 — Domain layer updates

### Add to GroupRepository interface
File: `shared/commonMain/domain/repository/GroupRepository.kt`
```kotlin
interface GroupRepository {
    suspend fun getUserGroups(): Result<List<Group>>
    suspend fun getGroupById(groupId: String): Result<Group>           // new
    suspend fun createGroup(request: CreateGroupRequest): Result<Group>
    suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>>
    suspend fun getGroupPosts(groupId: String, limit: Int = 3): Result<List<Post>>  // new
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
    suspend fun generateInviteLink(groupId: String): Result<String>
    suspend fun getPendingInvites(groupId: String): Result<List<GroupInvite>>       // new
    suspend fun revokeInvite(inviteId: String): Result<Unit>                        // new
    suspend fun deleteGroup(groupId: String): Result<Unit>
    suspend fun searchGroups(query: String): Result<List<Group>>
}
```

### GroupInvite model
File: `shared/commonMain/domain/model/GroupInvite.kt`
```kotlin
data class GroupInvite(
    val id: String,
    val groupId: String,
    val invitedBy: String,
    val token: String,
    val createdAt: Instant,
    val expiresAt: Instant
)
```

### FakeGroupRepository — add new methods
```kotlin
override suspend fun getGroupById(groupId: String): Result<Group> {
    delay(300)
    return Result.success(
        Group(
            id = groupId,
            name = if (groupId == "g1") "Way Street Runners" else "Irvington Ballers",
            description = if (groupId == "g1") "Weekly runs around Indy, all paces welcome"
                          else "Pickup basketball in Irvington",
            coverUrl = "https://picsum.photos/seed/${groupId}/800/400",
            createdBy = "u1",
            isOpen = false,
            memberCount = if (groupId == "g1") 12 else 8,
            createdAt = Clock.System.now().minus(30.days)
        )
    )
}

override suspend fun getGroupPosts(groupId: String, limit: Int): Result<List<Post>> {
    delay(300)
    // Return subset of FakePostRepository.allMockPosts filtered by groupId
    // Reuse the same mock posts — g1 returns p3, p6 | g2 returns p4
    return Result.success(mockGroupPosts[groupId]?.take(limit) ?: emptyList())
}

override suspend fun getPendingInvites(groupId: String): Result<List<GroupInvite>> {
    delay(200)
    return Result.success(
        listOf(
            GroupInvite("inv1", groupId, "u1", "abc123token",
                Clock.System.now().minus(1.days), Clock.System.now().plus(6.days)),
            GroupInvite("inv2", groupId, "u1", "def456token",
                Clock.System.now().minus(2.hours), Clock.System.now().plus(6.days).minus(2.hours))
        )
    )
}

override suspend fun revokeInvite(inviteId: String): Result<Unit> {
    delay(300)
    return Result.success(Unit)
}
```

---

## Phase 2 — ViewModel

File: `shared/commonMain/presentation/groupmanagement/GroupManagementViewModel.kt`

### UiState
```kotlin
data class GroupManagementUiState(
    val group: Group? = null,
    val members: List<GroupMember> = emptyList(),
    val recentPosts: List<Post> = emptyList(),       // max 3 — At a Glance
    val pendingInvites: List<GroupInvite> = emptyList(),
    val currentUserRole: GroupRole = GroupRole.MEMBER,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isGeneratingInvite: Boolean = false,
    val inviteLink: String? = null,                  // shown in share sheet after generation
    val showDeleteConfirmation: Boolean = false
)
```

### Intents
```kotlin
sealed class GroupManagementIntent {
    object Load : GroupManagementIntent()
    data class RemoveMember(val userId: String) : GroupManagementIntent()
    object GenerateInviteLink : GroupManagementIntent()
    data class RevokeInvite(val inviteId: String) : GroupManagementIntent()
    object ShowDeleteConfirmation : GroupManagementIntent()
    object DismissDeleteConfirmation : GroupManagementIntent()
    object ConfirmDeleteGroup : GroupManagementIntent()
    data class NavigateToPost(val postId: String) : GroupManagementIntent()
}
```

### Load logic
```kotlin
// On Load: fetch group, members, recentPosts(limit=3), pendingInvites in parallel
// Use async/await or zip — don't fetch sequentially
viewModelScope.launch {
    val groupDeferred = async { groupRepository.getGroupById(groupId) }
    val membersDeferred = async { groupRepository.getGroupMembers(groupId) }
    val postsDeferred = async { groupRepository.getGroupPosts(groupId, limit = 3) }
    val invitesDeferred = async { groupRepository.getPendingInvites(groupId) }

    // Determine current user role from members list
    val currentUserId = authRepository.getCurrentUser()?.id
    val role = members.find { it.userId == currentUserId }?.role ?: GroupRole.MEMBER
}
```

### Delete group
- On `ConfirmDeleteGroup`: call `groupRepository.deleteGroup(groupId)` → on success emit navigation event to pop back to Me tab
- Use `popUpTo("me", inclusive = false)` to clear group screens from back stack

---

## Phase 3 — Screen

File: `shared/commonMain/ui/groupmanagement/GroupManagementScreen.kt`

Apply the `/design` skill throughout. Warm, community feel.

### System insets
Outer Box must include `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`

### Top bar
- Back arrow ← left
- Group name center — `maxLines = 1`, `TextOverflow.Ellipsis`
- ⋮ menu right — **admin only** shows: "Edit group" (v2), "Delete group"
- Members see top bar with no ⋮ menu

### Cover photo
- Full width, `200dp` height, `contentScale = ContentScale.Crop`
- If `group.coverUrl` is null: show a generated placeholder using the group name initial letter centered on an accent-colored background
- Load via Coil: `"${group.coverUrl}?width=800&fit=cover"`

### Group info section
- Group name: `headlineSmall`, bold
- Description: `bodyMedium`, secondary color, hidden if null
- Member count: `bodySmall` with people icon

---

## Phase 4 — At a Glance section

### Section header
```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text("At a Glance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}
```

### CompactPostCard
File: `shared/commonMain/ui/components/CompactPostCard.kt`
```
┌─────────────────────────────┐
│ [Photo]  Description trunc. │
│  80x80dp  📅 Sat Jun 7 · 8AM│
└─────────────────────────────┘
```

```kotlin
@Composable
fun CompactPostCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        AsyncImage(
            model = post.images.firstOrNull()?.let { "$it?width=160&height=160&fit=cover" },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = post.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = post.startsAt.toFormattedDate(),  // e.g. "Sat Jun 7 · 8AM"
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### Tap behavior
- Tap `CompactPostCard` → `vm.onIntent(NavigateToPost(post.id))` → navigate to `post_detail/{postId}`

### Empty state for At a Glance
```kotlin
// Show when recentPosts is empty
Box(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    contentAlignment = Alignment.Center
) {
    Text(
        text = "No posts yet — be the first to post to this group!",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}
```

---

## Phase 5 — Members section

### Section header
```kotlin
Text(
    text = "Members (${members.size})",
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
)
```

### MemberRow
```kotlin
@Composable
fun MemberRow(
    member: GroupMember,
    isCurrentUserAdmin: Boolean,
    isCurrentUser: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (member.role == GroupRole.ADMIN) {
                    Spacer(Modifier.width(6.dp))
                    // Admin chip
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "Admin",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        leadingContent = {
            AsyncImage(
                model = member.avatarUrl?.let { "$it?width=80&height=80&fit=cover" },
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
        },
        trailingContent = {
            // Show remove × only if current user is admin and this is not themselves
            if (isCurrentUserAdmin && !isCurrentUser) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove member",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        modifier = modifier
    )
}
```

### Remove member confirmation
Show `AlertDialog` before removing:
- Title: "Remove member?"
- Body: "They'll lose access to the group and its posts."
- Confirm: "Remove" (destructive)
- Dismiss: "Cancel"

---

## Phase 6 — Pending invites section

Only visible to admins. Hidden entirely for regular members.

### Section header
```kotlin
Text(
    text = "Pending Invites (${pendingInvites.size})",
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
)
```

### InviteRow
```kotlin
ListItem(
    headlineContent = {
        Text(
            text = "Invite link · expires ${invite.expiresAt.toRelativeDate()}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    },
    supportingContent = {
        Text(
            text = invite.token,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    },
    trailingContent = {
        IconButton(onClick = { vm.onIntent(RevokeInvite(invite.id)) }) {
            Icon(Icons.Rounded.Close, contentDescription = "Revoke invite",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
)
```

### Invite members button
```kotlin
OutlinedButton(
    onClick = { vm.onIntent(GroupManagementIntent.GenerateInviteLink) },
    modifier = Modifier.fillMaxWidth().padding(16.dp)
) {
    Icon(Icons.Rounded.PersonAdd, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text(if (isGeneratingInvite) "Generating…" else "Invite members")
}
```
- On `GenerateInviteLink` success: open system share sheet with the invite link
- Use `expect/actual ShareHelper` to trigger native share sheet

---

## Phase 7 — Delete group

- ⋮ menu "Delete group" → `ShowDeleteConfirmation` intent → `AlertDialog`
- Title: "Delete group?"
- Body: "This will permanently delete the group and all its posts. This can't be undone."
- Confirm: "Delete" (destructive, red)
- Dismiss: "Cancel"
- On confirm: `ConfirmDeleteGroup` → delete → pop back to Me tab

---

## Phase 8 — Navigation

```
main_graph
    ├── me (tab 0)
    │     ├── group_management/{groupId}  ← pushed from Me tab group card
    │     │     └── post_detail/{postId}  ← pushed from CompactPostCard tap
    │     ├── create_group
    │     ├── create_post
    │     └── post_detail/{postId}
    ├── explore (tab 1)
    └── events (tab 2)
```

---

## Phase 9 — Koin wiring

Add to `postModule` or `groupModule`:
```kotlin
viewModel { (groupId: String) ->
    GroupManagementViewModel(groupId, get(), get())
}
```
Pass `groupId` as a Koin parameter from the navigation argument.

---

## Implementation order
1. `GroupInvite` domain model
2. Update `GroupRepository` interface — `getGroupById`, `getGroupPosts`, `getPendingInvites`, `revokeInvite`
3. Update `FakeGroupRepository` with new method implementations
4. `GroupManagementViewModel` — load in parallel, all intents
5. Koin wiring
6. `CompactPostCard` composable
7. `MemberRow` composable
8. `GroupManagementScreen` — all sections in order
9. Navigation hookup — `post_detail` route from `CompactPostCard` tap
10. Remove member confirmation dialog
11. Delete group confirmation dialog

## What NOT to do
- Don't fetch group, members, posts, and invites sequentially — load in parallel with `async`
- Don't show ⋮ menu or pending invites section to non-admin members
- Don't reuse the full `PostCard` from Explore — use `CompactPostCard` (photo + description + date only)
- Don't show "Edit group" in ⋮ menu — deferred to v2
- Don't navigate back to `GroupManagementScreen` after delete — pop back to Me tab fully
- Don't show pending invites section if user is not admin
- Don't skip the remove member confirmation dialog
- Don't skip the delete group confirmation dialog
  EOF