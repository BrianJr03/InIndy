package jr.brian.inindy.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.group_invite_accept_decline
import jr.brian.inindy.resources.group_invite_accept_error_dismiss
import jr.brian.inindy.resources.group_invite_accept_error_message
import jr.brian.inindy.resources.group_invite_accept_error_title
import jr.brian.inindy.resources.group_invite_accept_join
import jr.brian.inindy.resources.group_invite_accept_joining
import jr.brian.inindy.resources.group_invite_accept_loading
import jr.brian.inindy.resources.group_invite_accept_member_count
import jr.brian.inindy.resources.group_invite_accept_subtitle
import jr.brian.inindy.resources.group_invite_accept_title
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.presentation.me.GroupInviteUiState
import jr.brian.inindy.presentation.me.GroupInviteViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInviteSheet(
    token: String,
    onDismiss: () -> Unit,
    onJoined: (Group) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupInviteViewModel = koinViewModel(key = "invite-$token")
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(token) { viewModel.load(token) }

    LaunchedEffect(state) {
        val joined = state as? GroupInviteUiState.Joined ?: return@LaunchedEffect
        onJoined(joined.group)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val current = state) {
                GroupInviteUiState.Loading -> LoadingContent()
                is GroupInviteUiState.Preview -> PreviewContent(
                    group = current.group,
                    isJoining = false,
                    onJoin = { viewModel.join() },
                    onDecline = onDismiss
                )
                is GroupInviteUiState.Joining -> PreviewContent(
                    group = current.group,
                    isJoining = true,
                    onJoin = {},
                    onDecline = {}
                )
                is GroupInviteUiState.Joined -> PreviewContent(
                    group = current.group,
                    isJoining = true,
                    onJoin = {},
                    onDecline = {}
                )
                GroupInviteUiState.Error -> ErrorContent(onDismiss = onDismiss)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(Res.string.group_invite_accept_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreviewContent(
    group: Group,
    isJoining: Boolean,
    onJoin: () -> Unit,
    onDecline: () -> Unit
) {
    GroupAvatar(group = group)
    Text(
        text = stringResource(Res.string.group_invite_accept_title, group.name),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = stringResource(Res.string.group_invite_accept_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (!group.description.isNullOrBlank()) {
        Text(
            text = group.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Text(
        text = stringResource(Res.string.group_invite_accept_member_count, group.memberCount),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onDecline,
            enabled = !isJoining,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(
                text = stringResource(Res.string.group_invite_accept_decline),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Button(
            onClick = onJoin,
            enabled = !isJoining,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(
                text = stringResource(
                    if (isJoining) Res.string.group_invite_accept_joining
                    else Res.string.group_invite_accept_join
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun ErrorContent(onDismiss: () -> Unit) {
    Text(
        text = stringResource(Res.string.group_invite_accept_error_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = stringResource(Res.string.group_invite_accept_error_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(
                text = stringResource(Res.string.group_invite_accept_error_dismiss),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun GroupAvatar(group: Group) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.primary
                    )
                )
            )
    ) {
        if (!group.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = "${group.coverUrl}?width=200&height=200&fit=cover",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = group.name.firstOrNull()?.uppercase() ?: "?",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center
            )
        }
    }
}
