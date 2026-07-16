package jr.brian.inindy.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeepLinkBus {
    private val _pendingInviteToken = MutableStateFlow<String?>(null)
    val pendingInviteToken: StateFlow<String?> = _pendingInviteToken.asStateFlow()

    private val _pendingPostId = MutableStateFlow<String?>(null)
    val pendingPostId: StateFlow<String?> = _pendingPostId.asStateFlow()

    fun postInviteToken(token: String) {
        if (token.isBlank()) return
        _pendingInviteToken.value = token
    }

    fun clearInviteToken() {
        _pendingInviteToken.value = null
    }

    fun postPostId(postId: String) {
        if (postId.isBlank()) return
        _pendingPostId.value = postId
    }

    fun clearPostId() {
        _pendingPostId.value = null
    }
}
