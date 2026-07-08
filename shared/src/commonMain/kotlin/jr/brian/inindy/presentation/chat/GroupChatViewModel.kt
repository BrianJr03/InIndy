package jr.brian.inindy.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.data.chat.ProfanityFilter
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.GroupMember
import jr.brian.inindy.domain.model.GroupMessage
import jr.brian.inindy.domain.repository.ChatEvent
import jr.brian.inindy.domain.repository.GroupChatRepository
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupChatViewModel(
    private val groupId: String,
    private val chatRepository: GroupChatRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val groupRepository: GroupRepository,
    private val profanityFilter: ProfanityFilter
) : ViewModel() {
    private val _uiState = MutableStateFlow(GroupChatUiState())
    val uiState: StateFlow<GroupChatUiState> = _uiState.asStateFlow()

    // Cached member sender info (senderId → member), used to enrich messages
    // that come in via realtime UPDATE where the join wasn't refetched.
    private var members: Map<String, GroupMember> = emptyMap()

    private var lastTypingBroadcastMs: Long = 0L
    private val typingExpiries = mutableMapOf<String, Long>()
    private var typingExpiryJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(currentUserId = currentUserProvider.get().userId) }
            members = groupRepository.getGroupMembers(groupId).getOrDefault(emptyList())
                .associateBy { it.userId }
        }

        chatRepository.observeMessages(groupId)
            .onEach { event -> handleChatEvent(event) }
            .launchIn(viewModelScope)

        chatRepository.observeTyping(groupId)
            .onEach { userId -> onTypingReceived(userId) }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: GroupChatIntent) {
        when (intent) {
            is GroupChatIntent.DraftChanged -> onDraftChanged(intent.text)
            GroupChatIntent.Send -> send()
            GroupChatIntent.LoadOlder -> loadOlder()
            is GroupChatIntent.DeleteMessage -> deleteMessage(intent.messageId)
            GroupChatIntent.DismissProfanityBlock ->
                _uiState.update { it.copy(profanityBlocked = false) }
            GroupChatIntent.ChatOpened -> markRead()
        }
    }

    private fun handleChatEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.InitialLoad -> {
                val enriched = event.messages.map(::enrich)
                _uiState.update {
                    it.copy(
                        messages = enriched.sortedBy { m -> m.createdAt },
                        isLoading = false,
                        error = null,
                        hasReachedOldest = enriched.size < INITIAL_LIMIT
                    )
                }
            }
            is ChatEvent.NewMessage -> {
                val enriched = enrich(event.message)
                _uiState.update { state ->
                    if (state.messages.any { it.id == enriched.id }) state
                    else state.copy(messages = (state.messages + enriched).sortedBy { it.createdAt })
                }
                if (event.message.senderId != _uiState.value.currentUserId) markRead()
            }
            is ChatEvent.Updated -> {
                val enriched = enrich(event.message)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { if (it.id == enriched.id) enriched else it }
                    )
                }
            }
            is ChatEvent.Error -> {
                _uiState.update { it.copy(isLoading = false, error = event.throwable.message) }
            }
        }
    }

    private fun enrich(message: GroupMessage): GroupMessage {
        if (!message.senderName.isNullOrBlank() && !message.senderAvatarUrl.isNullOrBlank()) {
            return message
        }
        val member = members[message.senderId] ?: return message
        return message.copy(
            senderName = message.senderName ?: member.displayName,
            senderAvatarUrl = message.senderAvatarUrl ?: member.avatarUrl
        )
    }

    private fun onDraftChanged(text: String) {
        _uiState.update {
            it.copy(
                draft = text,
                // Clear the block as soon as they edit past the flagged text.
                profanityBlocked = it.profanityBlocked && profanityFilter.matches(text)
            )
        }
        maybeBroadcastTyping()
    }

    private fun maybeBroadcastTyping() {
        val now = currentTimeMillis()
        if (now - lastTypingBroadcastMs < TYPING_THROTTLE_MS) return
        lastTypingBroadcastMs = now
        viewModelScope.launch { chatRepository.broadcastTyping(groupId) }
    }

    private fun send() {
        val state = _uiState.value
        val body = state.draft.trim()
        if (body.isEmpty() || state.isSending) return
        if (profanityFilter.matches(body)) {
            _uiState.update { it.copy(profanityBlocked = true) }
            return
        }
        _uiState.update { it.copy(isSending = true, profanityBlocked = false) }
        viewModelScope.launch {
            chatRepository.sendMessage(groupId, body)
                .onSuccess {
                    _uiState.update { it.copy(draft = "", isSending = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSending = false, error = e.message ?: "Failed to send")
                    }
                }
        }
    }

    private fun loadOlder() {
        val state = _uiState.value
        if (state.isLoadingOlder || state.hasReachedOldest) return
        val oldest = state.messages.minByOrNull { it.createdAt } ?: return
        _uiState.update { it.copy(isLoadingOlder = true) }
        viewModelScope.launch {
            chatRepository.loadOlder(groupId, beforeCreatedAt = oldest.createdAt)
                .onSuccess { older ->
                    val enriched = older.map(::enrich)
                    _uiState.update { current ->
                        val existingIds = current.messages.mapTo(mutableSetOf()) { it.id }
                        val merged = (current.messages + enriched.filter { it.id !in existingIds })
                            .sortedBy { it.createdAt }
                        current.copy(
                            messages = merged,
                            isLoadingOlder = false,
                            hasReachedOldest = older.size < LOAD_OLDER_LIMIT
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingOlder = false) }
                }
        }
    }

    private fun deleteMessage(messageId: String) {
        viewModelScope.launch { chatRepository.deleteMessage(messageId) }
    }

    private fun markRead() {
        viewModelScope.launch { chatRepository.markChatRead(groupId) }
    }

    private fun onTypingReceived(userId: String) {
        val expiresAt = currentTimeMillis() + TYPING_TTL_MS
        typingExpiries[userId] = expiresAt
        _uiState.update { it.copy(typingUserIds = typingExpiries.keys.toSet()) }
        scheduleTypingExpiry()
    }

    private fun scheduleTypingExpiry() {
        if (typingExpiryJob?.isActive == true) return
        typingExpiryJob = viewModelScope.launch {
            while (typingExpiries.isNotEmpty()) {
                delay(1_000L)
                val now = currentTimeMillis()
                val stillTyping = typingExpiries.filterValues { it > now }
                if (stillTyping.size != typingExpiries.size) {
                    typingExpiries.clear()
                    typingExpiries.putAll(stillTyping)
                    _uiState.update { it.copy(typingUserIds = typingExpiries.keys.toSet()) }
                }
            }
        }
    }

    fun senderNameFor(userId: String): String? = members[userId]?.displayName

    private companion object {
        const val TYPING_THROTTLE_MS = 2_000L
        const val TYPING_TTL_MS = 3_000L
        const val INITIAL_LIMIT = 30
        const val LOAD_OLDER_LIMIT = 30
    }
}
