package jr.brian.inindy.domain.push

import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.DeviceTokenRepository
import jr.brian.inindy.util.appLog

class PushRegistrar(
    private val tokenProvider: PushTokenProvider,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val currentUserProvider: CurrentUserProvider,
) {
    private val log = appLog("PushRegistrar")

    suspend fun registerCurrentToken() {
        val userId = currentUserProvider.get().userId ?: return
        val token = tokenProvider.currentToken() ?: return
        deviceTokenRepository.upsertToken(token, tokenProvider.platform)
            .onFailure { log.e(it) { "push token upsert failed" } }
    }

    // Wire on sign-out later (Phase 4) so a shared device stops receiving
    // pushes for a logged-out account. Must run while still authenticated.
    suspend fun unregisterCurrentToken() {
        val token = tokenProvider.currentToken() ?: return
        deviceTokenRepository.deleteToken(token)
    }
}
