package jr.brian.inindy.di

import jr.brian.inindy.domain.push.PushTokenProvider

actual fun providePushTokenProvider(): PushTokenProvider = object : PushTokenProvider {
    override val platform: String = "ios"
    override suspend fun currentToken(): String? = null // TODO Phase 3: APNs -> Firebase
}
