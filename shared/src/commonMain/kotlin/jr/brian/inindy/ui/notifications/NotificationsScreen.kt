package jr.brian.inindy.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import jr.brian.inindy.domain.model.Notification
import jr.brian.inindy.presentation.notifications.NotificationsViewModel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.notifications_back_cd
import jr.brian.inindy.resources.notifications_delete_cd
import jr.brian.inindy.resources.notifications_empty
import jr.brian.inindy.resources.notifications_error_title
import jr.brian.inindy.resources.notifications_group_post
import jr.brian.inindy.resources.notifications_group_post_unknown_actor
import jr.brian.inindy.resources.notifications_group_post_unknown_group
import jr.brian.inindy.resources.notifications_mark_all_read
import jr.brian.inindy.resources.notifications_time_days
import jr.brian.inindy.resources.notifications_time_hours
import jr.brian.inindy.resources.notifications_time_just_now
import jr.brian.inindy.resources.notifications_time_minutes
import jr.brian.inindy.resources.notifications_title
import jr.brian.inindy.ui.icons.ArrowBackIcon
import jr.brian.inindy.ui.icons.DeleteIcon
import jr.brian.inindy.util.currentTimeMillis
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNotificationClick: (Notification) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotificationsTopBar(
                unreadCount = state.unreadCount,
                onBack = onBack,
                onMarkAllRead = { viewModel.markAllRead() }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            when {
                state.error != null && state.notifications.isEmpty() -> {
                    NotificationsError(message = state.error!!)
                }
                state.notifications.isEmpty() && !state.isLoading -> {
                    NotificationsEmpty()
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.notifications, key = { it.id }) { notification ->
                            SwipeToDeleteRow(
                                onDelete = { viewModel.delete(notification.id) }
                            ) {
                                NotificationRow(
                                    notification = notification,
                                    onClick = {
                                        viewModel.markAsRead(notification.id)
                                        onNotificationClick(notification)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsTopBar(
    unreadCount: Int,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit
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
                contentDescription = stringResource(Res.string.notifications_back_cd),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = stringResource(Res.string.notifications_title),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (unreadCount > 0) {
            TextButton(onClick = onMarkAllRead) {
                Text(
                    text = stringResource(Res.string.notifications_mark_all_read),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = DeleteIcon,
                    contentDescription = stringResource(Res.string.notifications_delete_cd),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        content()
    }
}

@Composable
private fun NotificationRow(
    notification: Notification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actorName = notification.actorName?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.notifications_group_post_unknown_actor)
    val groupName = notification.groupName?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.notifications_group_post_unknown_group)
    val messageText = stringResource(Res.string.notifications_group_post, actorName, groupName)
    val timeText = relativeTimeLabel(notification.createdAt)

    val background = if (notification.read) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    }

    Surface(
        color = background,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NotificationAvatar(url = notification.actorAvatarUrl, fallbackName = actorName)
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (notification.read) FontWeight.Normal else FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!notification.read) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun NotificationAvatar(url: String?, fallbackName: String) {
    val letter = fallbackName.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = "$url?width=200&height=200&fit=cover",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun NotificationsEmpty() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.notifications_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotificationsError(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.notifications_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun relativeTimeLabel(createdAtMs: Long): String {
    val diff = currentTimeMillis() - createdAtMs
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        minutes < 1L -> stringResource(Res.string.notifications_time_just_now)
        hours < 1L -> stringResource(Res.string.notifications_time_minutes, minutes.toInt())
        days < 1L -> stringResource(Res.string.notifications_time_hours, hours.toInt())
        else -> stringResource(Res.string.notifications_time_days, days.toInt())
    }
}
