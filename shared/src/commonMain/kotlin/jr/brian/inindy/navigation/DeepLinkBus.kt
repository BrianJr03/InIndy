package jr.brian.inindy.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeepLinkBus {
    private val _pendingInviteToken = MutableStateFlow<String?>(null)
    val pendingInviteToken: StateFlow<String?> = _pendingInviteToken.asStateFlow()

    fun postInviteToken(token: String) {
        if (token.isBlank()) return
        _pendingInviteToken.value = token
    }

    fun clearInviteToken() {
        _pendingInviteToken.value = null
    }
}
