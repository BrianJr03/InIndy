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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.GroupInvite
import jr.brian.inindy.domain.model.GroupMember
import jr.brian.inindy.domain.model.GroupRole
import jr.brian.inindy.presentation.me.GroupManagementViewModel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.group_back_cd
import jr.brian.inindy.resources.group_delete_confirm
import jr.brian.inindy.resources.group_delete_dialog_message
import jr.brian.inindy.resources.group_delete_dialog_title
import jr.brian.inindy.resources.group_delete_dismiss
import jr.brian.inindy.resources.group_invite_cta
import jr.brian.inindy.resources.group_invite_link_dismiss
import jr.brian.inindy.resources.group_invite_link_title
import jr.brian.inindy.resources.group_leave_confirm
import jr.brian.inindy.resources.group_leave_dialog_message
import jr.brian.inindy.resources.group_leave_dialog_title
import jr.brian.inindy.resources.group_leave_dismiss
import jr.brian.inindy.resources.group_members_header
import jr.brian.inindy.resources.group_menu_cd
import jr.brian.inindy.resources.group_menu_delete
import jr.brian.inindy.resources.group_menu_edit
import jr.brian.inindy.resources.group_menu_leave
import jr.brian.inindy.resources.group_pending_header
import jr.brian.inindy.resources.group_remove_member_cd
import jr.brian.inindy.resources.group_revoke_invite_cd
import jr.brian.inindy.resources.me_role_admin
import jr.brian.inindy.resources.me_role_member
import jr.brian.inindy.ui.icons.ArrowBackIcon
import jr.brian.inindy.ui.icons.CloseIcon
import jr.brian.inindy.ui.icons.GroupIcon
import jr.brian.inindy.ui.icons.MoreVertIcon
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GroupManagementScreen(
    groupId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupManagementViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(groupId) { viewModel.load(groupId) }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    BackHandler(onBack = onBack)

    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }

    val group = state.group
    val isAdmin = group?.role == GroupRole.ADMIN

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
                onEdit = { menuExpanded = false },
                onDelete = {
                    menuExpanded = false
                    confirmDelete = true
                },
                onLeave = {
                    menuExpanded = false
                    confirmLeave = true
                }
            )
            if (group == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    GroupCover(group)
                    GroupSummary(group)
                    MembersBlock(
                        members = state.members,
                        isAdmin = isAdmin,
                        onRemove = viewModel::removeMember
                    )
                    if (isAdmin) {
                        InvitesBlock(
                            invites = state.pendingInvites,
                            onRevoke = viewModel::revokeInvite
                        )
                        Button(
                            onClick = viewModel::generateInvite,
                            enabled = !state.isGeneratingInvite,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.group_invite_cta),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.group_delete_dialog_title)) },
            text = { Text(stringResource(Res.string.group_delete_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteGroup()
                }) {
                    Text(
                        text = stringResource(Res.string.group_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(Res.string.group_delete_dismiss))
                }
            }
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(Res.string.group_leave_dialog_title)) },
            text = { Text(stringResource(Res.string.group_leave_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    viewModel.leaveGroup()
                }) {
                    Text(
                        text = stringResource(Res.string.group_leave_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) {
                    Text(stringResource(Res.string.group_leave_dismiss))
                }
            }
        )
    }

    state.newInviteLink?.let { link ->
        AlertDialog(
            onDismissRequest = viewModel::dismissInviteLink,
            title = { Text(stringResource(Res.string.group_invite_link_title)) },
            text = {
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissInviteLink) {
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLeave: () -> Unit
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
                .padding(start = 4.dp)
                .heightIn(min = 36.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = MoreVertIcon,
                    contentDescription = stringResource(Res.string.group_menu_cd),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
                if (isAdmin) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.group_menu_edit)) },
                        onClick = onEdit
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(Res.string.group_menu_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = onDelete
                    )
                } else {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(Res.string.group_menu_leave),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = onLeave
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCover(group: Group) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
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
                model = group.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
private fun GroupSummary(group: Group) {
    if (group.description.isNullOrBlank()) return
    Text(
        text = group.description,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 22.sp
    )
}

@Composable
private fun MembersBlock(
    members: List<GroupMember>,
    isAdmin: Boolean,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(Res.string.group_members_header, members.size),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        members.forEach { member ->
            MemberRow(member = member, isAdmin = isAdmin, onRemove = { onRemove(member.userId) })
        }
    }
}

@Composable
private fun MemberRow(
    member: GroupMember,
    isAdmin: Boolean,
    onRemove: () -> Unit
) {
    val isSelf = member.userId == "me"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MemberAvatar(name = member.displayName, avatarUrl = member.avatarUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            RoleLabel(role = member.role)
        }
        if (isAdmin && !isSelf) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = CloseIcon,
                    contentDescription = stringResource(Res.string.group_remove_member_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MemberAvatar(name: String, avatarUrl: String?) {
    val size = 44.dp
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RoleLabel(role: GroupRole) {
    val color = when (role) {
        GroupRole.ADMIN -> MaterialTheme.colorScheme.primary
        GroupRole.MEMBER -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = stringResource(
            if (role == GroupRole.ADMIN) Res.string.me_role_admin else Res.string.me_role_member
        ),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun InvitesBlock(invites: List<GroupInvite>, onRevoke: (String) -> Unit) {
    if (invites.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.group_pending_header, invites.size),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        invites.forEach { invite ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = GroupIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = invite.invitedEmail,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { onRevoke(invite.id) }) {
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
    }
}
