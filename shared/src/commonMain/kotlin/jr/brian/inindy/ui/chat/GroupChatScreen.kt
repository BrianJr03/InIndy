package jr.brian.inindy.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import jr.brian.inindy.domain.model.GroupMessage
import jr.brian.inindy.presentation.chat.GroupChatIntent
import jr.brian.inindy.presentation.chat.GroupChatViewModel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.group_chat_back_cd
import jr.brian.inindy.resources.group_chat_delete_confirm
import jr.brian.inindy.resources.group_chat_delete_dialog_body
import jr.brian.inindy.resources.group_chat_delete_dialog_title
import jr.brian.inindy.resources.group_chat_delete_dismiss
import jr.brian.inindy.resources.group_chat_empty
import jr.brian.inindy.resources.group_chat_error_title
import jr.brian.inindy.resources.group_chat_input_placeholder
import jr.brian.inindy.resources.group_chat_profanity_blocked
import jr.brian.inindy.resources.group_chat_profanity_dismiss
import jr.brian.inindy.resources.group_chat_removed
import jr.brian.inindy.resources.group_chat_send_cd
import jr.brian.inindy.resources.group_chat_time_today
import jr.brian.inindy.resources.group_chat_time_yesterday
import jr.brian.inindy.resources.group_chat_title
import jr.brian.inindy.resources.group_chat_typing_multiple
import jr.brian.inindy.resources.group_chat_typing_single
import jr.brian.inindy.resources.group_chat_typing_unknown
import jr.brian.inindy.ui.icons.ArrowBackIcon
import jr.brian.inindy.ui.icons.SendIcon
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupChatScreen(
    groupId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupChatViewModel = koinViewModel(
        key = "group-chat-$groupId",
        parameters = { parametersOf(groupId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.onIntent(GroupChatIntent.ChatOpened) }

    val listState = rememberLazyListState()

    // Reverse-layout means "top of list" (oldest) = last index. Trigger loadOlder
    // when the user has scrolled up to within a few items of that boundary.
    LaunchedEffect(listState, state.messages.size, state.hasReachedOldest) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                val total = state.messages.size
                if (
                    !state.isLoadingOlder &&
                    !state.hasReachedOldest &&
                    total > 0 &&
                    lastVisibleIndex >= total - LOAD_OLDER_TRIGGER_OFFSET
                ) {
                    viewModel.onIntent(GroupChatIntent.LoadOlder)
                }
            }
    }

    // Auto-scroll to newest on each new message. reverseLayout=true means index 0
    // is the bottom (newest); scrolling to 0 keeps the view stuck to the latest.
    LaunchedEffect(state.messages.firstOrNull()?.id) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    var messagePendingDelete by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatTopBar(onBack = onBack)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.error != null && state.messages.isEmpty() -> ChatErrorState(state.error!!)
                    state.messages.isEmpty() && !state.isLoading -> ChatEmptyState()
                    else -> ChatMessageList(
                        listState = listState,
                        messages = state.messages,
                        currentUserId = state.currentUserId,
                        onLongPressOwnMessage = { messagePendingDelete = it }
                    )
                }
            }

            TypingIndicator(
                typingUserIds = state.typingUserIds,
                senderNameFor = viewModel::senderNameFor
            )

            ChatInputRow(
                draft = state.draft,
                isSending = state.isSending,
                profanityBlocked = state.profanityBlocked,
                onDraftChanged = { viewModel.onIntent(GroupChatIntent.DraftChanged(it)) },
                onSend = { viewModel.onIntent(GroupChatIntent.Send) },
                onDismissProfanityBlock = {
                    viewModel.onIntent(GroupChatIntent.DismissProfanityBlock)
                }
            )
        }
    }

    if (messagePendingDelete != null) {
        AlertDialog(
            onDismissRequest = { messagePendingDelete = null },
            title = { Text(stringResource(Res.string.group_chat_delete_dialog_title)) },
            text = { Text(stringResource(Res.string.group_chat_delete_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = messagePendingDelete
                    messagePendingDelete = null
                    if (id != null) viewModel.onIntent(GroupChatIntent.DeleteMessage(id))
                }) {
                    Text(
                        text = stringResource(Res.string.group_chat_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { messagePendingDelete = null }) {
                    Text(stringResource(Res.string.group_chat_delete_dismiss))
                }
            }
        )
    }
}

@Composable
private fun ChatTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = ArrowBackIcon,
                contentDescription = stringResource(Res.string.group_chat_back_cd),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = stringResource(Res.string.group_chat_title),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatMessageList(
    listState: LazyListState,
    messages: List<GroupMessage>,
    currentUserId: String?,
    onLongPressOwnMessage: (String) -> Unit
) {
    // Reverse so index 0 = newest at the bottom; iterate messages newest-first
    // by reversing the ordered list once.
    val newestFirst = remember(messages) { messages.asReversed() }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(items = newestFirst, key = { _, m -> m.id }) { index, message ->
            val isOwn = currentUserId != null && message.senderId == currentUserId
            // In reversed order, the "older" neighbour sits at index+1.
            val older = newestFirst.getOrNull(index + 1)
            val showDayDivider = shouldShowDayDivider(message.createdAt, older?.createdAt)
            Column {
                if (showDayDivider) {
                    DayDivider(createdAtMs = message.createdAt)
                }
                MessageBubble(
                    message = message,
                    isOwn = isOwn,
                    onLongPress = { if (isOwn) onLongPressOwnMessage(message.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: GroupMessage,
    isOwn: Boolean,
    onLongPress: () -> Unit
) {
    val alignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when {
        message.isRemoved -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        isOwn -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        message.isRemoved -> MaterialTheme.colorScheme.onSurfaceVariant
        isOwn -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val bubbleShape = if (isOwn) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }
    val displayText = if (message.isRemoved) {
        stringResource(Res.string.group_chat_removed)
    } else {
        message.body.orEmpty()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            if (!isOwn) {
                SenderAvatar(
                    url = message.senderAvatarUrl,
                    fallbackName = message.senderName.orEmpty()
                )
                Spacer(Modifier.width(6.dp))
            }
            Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
                if (!isOwn && !message.senderName.isNullOrBlank()) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }
                Surface(
                    color = bubbleColor,
                    contentColor = contentColor,
                    shape = bubbleShape,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongPress
                    )
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = if (message.isRemoved) {
                                androidx.compose.ui.text.font.FontStyle.Italic
                            } else {
                                androidx.compose.ui.text.font.FontStyle.Normal
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Text(
                    text = timeLabel(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SenderAvatar(url: String?, fallbackName: String) {
    val letter = fallbackName.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(28.dp)
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
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DayDivider(createdAtMs: Long) {
    val label = dayLabel(createdAtMs)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ChatEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.group_chat_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChatErrorState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.group_chat_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TypingIndicator(
    typingUserIds: Set<String>,
    senderNameFor: (String) -> String?
) {
    val label = when (typingUserIds.size) {
        0 -> null
        1 -> {
            val name = senderNameFor(typingUserIds.first())
            if (name.isNullOrBlank()) {
                stringResource(Res.string.group_chat_typing_unknown)
            } else {
                stringResource(Res.string.group_chat_typing_single, name)
            }
        }
        else -> stringResource(Res.string.group_chat_typing_multiple)
    }
    if (label != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChatInputRow(
    draft: String,
    isSending: Boolean,
    profanityBlocked: Boolean,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onDismissProfanityBlock: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (profanityBlocked) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.group_chat_profanity_blocked),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismissProfanityBlock) {
                        Text(
                            text = stringResource(Res.string.group_chat_profanity_dismiss),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChanged,
                placeholder = { Text(stringResource(Res.string.group_chat_input_placeholder)) },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            SendButton(
                enabled = draft.isNotBlank() && !isSending,
                onClick = onSend
            )
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = SendIcon,
                contentDescription = stringResource(Res.string.group_chat_send_cd),
                tint = fg
            )
        }
    }
}

// ── Date/time helpers ────────────────────────────────────────────────────────

@Composable
private fun timeLabel(createdAtMs: Long): String {
    val local = createdAtMs.toLocal()
    val hour24 = local.hour
    val hour12 = when {
        hour24 == 0 -> 12       // midnight → 12 AM
        hour24 > 12 -> hour24 - 12
        else -> hour24          // 12 stays 12 (noon)
    }
    val minute = local.minute.toString().padStart(2, '0')
    val amPm = if (hour24 < 12) "AM" else "PM"
    return "$hour12:$minute $amPm"
}

@Composable
private fun dayLabel(createdAtMs: Long): String {
    val today = currentTimeMillis().toLocalDate()
    val date = createdAtMs.toLocalDate()
    val diffDays = today.toEpochDayNumber() - date.toEpochDayNumber()
    return when (diffDays) {
        0L -> stringResource(Res.string.group_chat_time_today)
        1L -> stringResource(Res.string.group_chat_time_yesterday)
        else -> "${date.year}-${date.monthNumber.pad2()}-${date.dayOfMonth.pad2()}"
    }
}

private fun shouldShowDayDivider(currentMs: Long, olderMs: Long?): Boolean {
    if (olderMs == null) return true
    return currentMs.toLocalDate().toEpochDayNumber() != olderMs.toLocalDate().toEpochDayNumber()
}

private fun Long.toLocal() =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())

private fun Long.toLocalDate() = toLocal().date

// Howard Hinnant civil calendar for a stable "days since epoch" comparison
// without pulling more from kotlinx-datetime.
private fun kotlinx.datetime.LocalDate.toEpochDayNumber(): Long {
    val y = if (monthNumber <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = (y - era * 400).toLong()
    val mp = if (monthNumber > 2) monthNumber - 3 else monthNumber + 9
    val doy = (153L * mp + 2) / 5 + dayOfMonth - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era.toLong() * 146097L + doe - 719468L
}

private fun Int.pad2() = toString().padStart(2, '0')

private const val LOAD_OLDER_TRIGGER_OFFSET = 4
