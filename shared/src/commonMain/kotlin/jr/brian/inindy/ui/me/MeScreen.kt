package jr.brian.inindy.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import jr.brian.inindy.domain.model.AttendanceRecord
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.GroupRole
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.presentation.me.MeUiState
import jr.brian.inindy.presentation.me.MeViewModel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.explore_settings_content_description
import jr.brian.inindy.resources.me_attendance_rate
import jr.brian.inindy.resources.me_create_group_cta
import jr.brian.inindy.resources.me_create_post_cta
import jr.brian.inindy.resources.me_delete_post_confirm
import jr.brian.inindy.resources.me_delete_post_dialog_message
import jr.brian.inindy.resources.me_delete_post_dialog_title
import jr.brian.inindy.resources.me_delete_post_dismiss
import jr.brian.inindy.resources.me_empty_attendance
import jr.brian.inindy.resources.me_empty_groups
import jr.brian.inindy.resources.me_empty_posts
import jr.brian.inindy.resources.me_group_member_count
import jr.brian.inindy.resources.me_post_rsvp_count
import jr.brian.inindy.resources.me_post_rsvp_count_past
import jr.brian.inindy.resources.me_post_rsvp_count_single
import jr.brian.inindy.resources.me_post_rsvp_count_single_past
import jr.brian.inindy.resources.me_role_admin
import jr.brian.inindy.resources.me_role_member
import jr.brian.inindy.resources.me_section_attendance
import jr.brian.inindy.resources.me_section_your_groups
import jr.brian.inindy.resources.me_section_your_posts
import jr.brian.inindy.resources.me_see_all
import jr.brian.inindy.resources.me_status_past
import jr.brian.inindy.resources.me_status_upcoming
import jr.brian.inindy.ui.icons.DateRangeIcon
import jr.brian.inindy.ui.icons.DeleteIcon
import jr.brian.inindy.ui.icons.GroupIcon
import jr.brian.inindy.ui.icons.SettingsIcon
import jr.brian.inindy.ui.icons.StarIcon
import jr.brian.inindy.ui.profile.ProfileEditSheet
import jr.brian.inindy.util.DateUtil
import jr.brian.inindy.util.currentTimeMillis
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun MeScreen(
    onCreatePostClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onPostClick: (String) -> Unit,
    onGroupClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    refreshTrigger: Int = 0,
    viewModel: MeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) viewModel.refresh()
    }
    MeScreenContent(
        state = state,
        onCreatePostClick = onCreatePostClick,
        onCreateGroupClick = onCreateGroupClick,
        onPostClick = onPostClick,
        onGroupClick = onGroupClick,
        onSettingsClick = onSettingsClick,
        onDeletePost = viewModel::deletePost,
        modifier = modifier
    )
}

@Composable
private fun MeScreenContent(
    state: MeUiState,
    onCreatePostClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onPostClick: (String) -> Unit,
    onGroupClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onDeletePost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var postPendingDeletion by remember { mutableStateOf<String?>(null) }
    var deletingPostIds by remember { mutableStateOf(setOf<String>()) }
    var showProfileEditSheet by remember { mutableStateOf(false) }
    val now = currentTimeMillis()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                MeHeader(
                    user = state.user,
                    neighborhoodName = state.neighborhoodName,
                    attendanceRate = state.attendanceRate,
                    onProfileClick = { showProfileEditSheet = true },
                    onSettingsClick = onSettingsClick
                )
            }
            item {
                CreatePostCta(onClick = onCreatePostClick)
            }
            item {
                SectionHeader(
                    title = stringResource(Res.string.me_section_your_posts),
                    actionLabel = if (state.recentPosts.size > 3) stringResource(Res.string.me_see_all) else null,
                    onActionClick = {}
                )
            }
            if (state.recentPosts.isEmpty()) {
                item {
                    EmptyHint(text = stringResource(Res.string.me_empty_posts))
                }
            } else {
                items(
                    items = state.recentPosts.take(3),
                    key = { it.id }
                ) { post ->
                    AnimatedVisibility(
                        visible = post.id !in deletingPostIds,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        PostManageCard(
                            post = post,
                            nowMs = now,
                            onClick = { onPostClick(post.id) },
                            onDeleteClick = { postPendingDeletion = post.id }
                        )
                    }
                }
            }
            item {
                SectionHeader(
                    title = stringResource(Res.string.me_section_your_groups),
                    actionLabel = if (state.groups.size > 3) stringResource(Res.string.me_see_all) else null,
                    onActionClick = {}
                )
            }
            if (state.groups.isEmpty()) {
                item {
                    EmptyHint(text = stringResource(Res.string.me_empty_groups))
                }
            } else {
                items(state.groups.take(3)) { group ->
                    GroupCard(group = group, onClick = { onGroupClick(group.id) })
                }
            }
            item {
                CreateGroupCta(onClick = onCreateGroupClick)
            }
            item {
                SectionHeader(
                    title = stringResource(Res.string.me_section_attendance),
                    actionLabel = null,
                    onActionClick = {}
                )
            }
            if (state.attendanceHistory.isEmpty()) {
                item {
                    EmptyHint(text = stringResource(Res.string.me_empty_attendance))
                }
            } else {
                items(state.attendanceHistory.take(5)) { record ->
                    AttendanceCard(record = record)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (postPendingDeletion != null) {
        AlertDialog(
            onDismissRequest = { postPendingDeletion = null },
            title = { Text(stringResource(Res.string.me_delete_post_dialog_title)) },
            text = { Text(stringResource(Res.string.me_delete_post_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = postPendingDeletion!!
                    deletingPostIds = deletingPostIds + id
                    onDeletePost(id)
                    postPendingDeletion = null
                }) {
                    Text(
                        stringResource(Res.string.me_delete_post_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { postPendingDeletion = null }) {
                    Text(stringResource(Res.string.me_delete_post_dismiss))
                }
            }
        )
    }

    if (showProfileEditSheet) {
        ProfileEditSheet(onDismiss = {
            showProfileEditSheet = false
        })
    }
}

@Composable
private fun MeHeader(
    user: User?,
    neighborhoodName: String,
    attendanceRate: Float,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onProfileClick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeaderAvatar(name = user?.fullName ?: "You", avatarUrl = user?.avatarUrl)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.fullName ?: "You",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = neighborhoodName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " · ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = StarIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            Res.string.me_attendance_rate,
                            (attendanceRate * 100).roundToInt()
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = SettingsIcon,
                contentDescription = stringResource(Res.string.explore_settings_content_description),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HeaderAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val size = 60.dp
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        val initial = name.firstOrNull()?.uppercase() ?: "?"
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CreatePostCta(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = stringResource(Res.string.me_create_post_cta),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun CreateGroupCta(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = stringResource(Res.string.me_create_group_cta),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 0.3.sp
        )
        if (actionLabel != null) {
            TextButton(onClick = onActionClick, contentPadding = PaddingValues(0.dp)) {
                Text(
                    text = actionLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PostManageCard(
    post: Post,
    nowMs: Long,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isUpcoming = post.startsAt > nowMs
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PostThumbnail(imageUrl = post.images.firstOrNull())
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = DateUtil.formatEventDate(post.startsAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(isUpcoming = isUpcoming)
                    Text(
                        text = when {
                            isUpcoming && post.rsvpCount == 1 ->
                                stringResource(Res.string.me_post_rsvp_count_single, post.rsvpCount)
                            isUpcoming ->
                                stringResource(Res.string.me_post_rsvp_count, post.rsvpCount)
                            post.rsvpCount == 1 ->
                                stringResource(Res.string.me_post_rsvp_count_single_past, post.rsvpCount)
                            else ->
                                stringResource(Res.string.me_post_rsvp_count_past, post.rsvpCount)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = DeleteIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PostThumbnail(imageUrl: String?) {
    val size = 72.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = DateRangeIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun StatusChip(isUpcoming: Boolean) {
    val color =
        if (isUpcoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = stringResource(
                if (isUpcoming) Res.string.me_status_upcoming else Res.string.me_status_past
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GroupCard(
    group: Group,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GroupCover(coverUrl = group.coverUrl, name = group.name)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.me_group_member_count, group.memberCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                RoleChip(role = group.role)
            }
        }
    }
}

@Composable
private fun GroupCover(coverUrl: String?, name: String) {
    val size = 56.dp
    if (!coverUrl.isNullOrBlank()) {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RoleChip(role: GroupRole) {
    val color = when (role) {
        GroupRole.ADMIN -> MaterialTheme.colorScheme.primary
        GroupRole.MEMBER -> MaterialTheme.colorScheme.outline
    }
    val label = stringResource(
        if (role == GroupRole.ADMIN) Res.string.me_role_admin else Res.string.me_role_member
    )
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AttendanceCard(record: AttendanceRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PostThumbnail(imageUrl = record.postImageUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.postTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "with ${record.hostName} · ${DateUtil.formatEventDate(record.attendedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = GroupIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview
@Composable
private fun MeScreenPreview() {
    val sample = MeUiState(
        user = User(id = "me", fullName = "Brian", avatarUrl = null),
        neighborhoodName = "Broad Ripple",
        attendanceRate = 0.92f,
        recentPosts = emptyList(),
        groups = emptyList(),
        attendanceHistory = emptyList(),
        isLoading = false
    )
    MaterialTheme {
        MeScreenContent(
            state = sample,
            onCreatePostClick = {},
            onCreateGroupClick = {},
            onPostClick = {},
            onGroupClick = {},
            onSettingsClick = {},
            onDeletePost = {}
        )
    }
}
