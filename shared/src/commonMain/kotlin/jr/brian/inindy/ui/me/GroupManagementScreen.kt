package jr.brian.inindy.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.GroupInvite
import jr.brian.inindy.domain.model.GroupRole
import jr.brian.inindy.presentation.me.GroupManagementIntent
import jr.brian.inindy.presentation.me.GroupManagementViewModel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.group_at_a_glance
import jr.brian.inindy.resources.group_at_a_glance_empty
import jr.brian.inindy.resources.group_back_cd
import jr.brian.inindy.resources.group_delete_confirm
import jr.brian.inindy.resources.group_delete_dialog_message
import jr.brian.inindy.resources.group_delete_dialog_title
import jr.brian.inindy.resources.group_delete_dismiss
import jr.brian.inindy.resources.group_invite_cta
import jr.brian.inindy.resources.group_invite_cta_generating
import jr.brian.inindy.resources.group_invite_expires
import jr.brian.inindy.resources.group_invite_link_dismiss
import jr.brian.inindy.resources.group_invite_link_title
import jr.brian.inindy.resources.group_invite_token_label
import jr.brian.inindy.resources.group_member_count
import jr.brian.inindy.resources.group_members_header
import jr.brian.inindy.resources.group_menu_cd
import jr.brian.inindy.resources.group_menu_delete
import jr.brian.inindy.resources.group_pending_header
import jr.brian.inindy.resources.group_remove_member_confirm
import jr.brian.inindy.resources.group_remove_member_dialog_message
import jr.brian.inindy.resources.group_remove_member_dialog_title
import jr.brian.inindy.resources.group_remove_member_dismiss
import jr.brian.inindy.resources.group_revoke_invite_cd
import jr.brian.inindy.ui.components.CompactPostCard
import jr.brian.inindy.ui.components.MemberRow
import jr.brian.inindy.ui.icons.ArrowBackIcon
import jr.brian.inindy.ui.icons.CloseIcon
import jr.brian.inindy.ui.icons.GroupIcon
import jr.brian.inindy.ui.icons.MoreVertIcon
import jr.brian.inindy.util.DateUtil
import jr.brian.inindy.util.currentTimeMillis
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val CURRENT_USER_ID = "me"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GroupManagementScreen(
    groupId: String,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupManagementViewModel = koinViewModel(
        key = "group-management-$groupId",
        parameters = { parametersOf(groupId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.postNavigation.collect { postId -> onPostClick(postId) }
    }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    BackHandler(onBack = onBack)

    var menuExpanded by remember { mutableStateOf(false) }
    var memberPendingRemoval by remember { mutableStateOf<String?>(null) }

    val group = state.group
    val isAdmin = state.currentUserRole == GroupRole.ADMIN

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GroupTopBar(
                title = group?.name.orEmpty(),
                isAdmin = isAdmin,
                menuExpanded = menuExpanded,
                onBack = onBack,
                onMenuClick = { menuExpanded = true },
                onMenuDismiss = { menuExpanded = false },
                onDelete = {
                    menuExpanded = false
                    viewModel.onIntent(GroupManagementIntent.ShowDeleteConfirmation)
                }
            )

            if (group == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Loading…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item { GroupCover(group) }
                    item { GroupInfo(group) }
                    item { SectionDivider() }
                    item { SectionHeader(text = stringResource(Res.string.group_at_a_glance)) }
                    if (state.recentPosts.isEmpty()) {
                        item { AtAGlanceEmpty() }
                    } else {
                        items(state.recentPosts, key = { it.id }) { post ->
                            CompactPostCard(
                                post = post,
                                onClick = {
                                    viewModel.onIntent(GroupManagementIntent.NavigateToPost(post.id))
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    item { SectionDivider() }
                    item {
                        SectionHeader(
                            text = stringResource(
                                Res.string.group_members_header,
                                state.members.size
                            )
                        )
                    }
                    items(state.members, key = { it.userId }) { member ->
                        MemberRow(
                            member = member,
                            isCurrentUserAdmin = isAdmin,
                            isCurrentUser = member.userId == CURRENT_USER_ID,
                            onRemove = { memberPendingRemoval = member.userId },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    if (isAdmin) {
                        item { SectionDivider() }
                        item {
                            SectionHeader(
                                text = stringResource(
                                    Res.string.group_pending_header,
                                    state.pendingInvites.size
                                )
                            )
                        }
                        items(state.pendingInvites, key = { it.id }) { invite ->
                            InviteRow(
                                invite = invite,
                                onRevoke = {
                                    viewModel.onIntent(GroupManagementIntent.RevokeInvite(invite.id))
                                }
                            )
                        }
                        item {
                            Button(
                                onClick = {
                                    viewModel.onIntent(GroupManagementIntent.GenerateInviteLink)
                                },
                                enabled = !state.isGeneratingInvite,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                                    .heightIn(min = 52.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        if (state.isGeneratingInvite) Res.string.group_invite_cta_generating
                                        else Res.string.group_invite_cta
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (memberPendingRemoval != null) {
        AlertDialog(
            onDismissRequest = { memberPendingRemoval = null },
            title = { Text(stringResource(Res.string.group_remove_member_dialog_title)) },
            text = { Text(stringResource(Res.string.group_remove_member_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = memberPendingRemoval
                    memberPendingRemoval = null
                    if (id != null) {
                        viewModel.onIntent(GroupManagementIntent.RemoveMember(id))
                    }
                }) {
                    Text(
                        text = stringResource(Res.string.group_remove_member_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingRemoval = null }) {
                    Text(stringResource(Res.string.group_remove_member_dismiss))
                }
            }
        )
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onIntent(GroupManagementIntent.DismissDeleteConfirmation)
            },
            title = { Text(stringResource(Res.string.group_delete_dialog_title)) },
            text = { Text(stringResource(Res.string.group_delete_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onIntent(GroupManagementIntent.ConfirmDeleteGroup)
                }) {
                    Text(
                        text = stringResource(Res.string.group_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.onIntent(GroupManagementIntent.DismissDeleteConfirmation)
                }) {
                    Text(stringResource(Res.string.group_delete_dismiss))
                }
            }
        )
    }

    state.inviteLink?.let { link ->
        AlertDialog(
            onDismissRequest = {
                viewModel.onIntent(GroupManagementIntent.DismissInviteLink)
            },
            title = { Text(stringResource(Res.string.group_invite_link_title)) },
            text = {
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onIntent(GroupManagementIntent.DismissInviteLink)
                }) {
                    Text(
                        text = stringResource(Res.string.group_invite_link_dismiss),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}

@Composable
private fun GroupTopBar(
    title: String,
    isAdmin: Boolean,
    menuExpanded: Boolean,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = ArrowBackIcon,
                contentDescription = stringResource(Res.string.group_back_cd),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isAdmin) {
            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = MoreVertIcon,
                        contentDescription = stringResource(Res.string.group_menu_cd),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(Res.string.group_menu_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = onDelete
                    )
                }
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun GroupCover(group: Group) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.primary
                    )
                )
            )
    ) {
        if (!group.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = "${group.coverUrl}?width=800&fit=cover",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = group.name.firstOrNull()?.uppercase() ?: "?",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 96.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.92f)
            )
        }
    }
}

@Composable
private fun GroupInfo(group: Group) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = group.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!group.description.isNullOrBlank()) {
            Text(
                text = group.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = GroupIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.group_member_count, group.memberCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun AtAGlanceEmpty() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.group_at_a_glance_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InviteRow(invite: GroupInvite, onRevoke: () -> Unit) {
    val now = currentTimeMillis()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.group_invite_token_label) + " · " +
                        stringResource(
                            Res.string.group_invite_expires,
                            DateUtil.formatRelativeDate(invite.expiresAt, now)
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = invite.token,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRevoke) {
                Icon(
                    imageVector = CloseIcon,
                    contentDescription = stringResource(Res.string.group_revoke_invite_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
