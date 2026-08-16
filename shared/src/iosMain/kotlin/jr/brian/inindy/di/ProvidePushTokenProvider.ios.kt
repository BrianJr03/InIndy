package jr.brian.inindy.di

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import jr.brian.inindy.domain.push.PushTokenProvider
import jr.brian.inindy.util.appLog

private val log = appLog("IosPushTokenProvider")

actual fun providePushTokenProvider(): PushTokenProvider = object : PushTokenProvider {
    override val platform: String = "ios"

    override suspend fun currentToken(): String? = try {
        Firebase.messaging.getToken()
    } catch (t: Throwable) {
        // Common reasons the token is unavailable at call time:
        //   - user hasn't accepted push permission yet
        //   - APNs token hasn't been forwarded to FCM yet (app hasn't finished
        //     registering for remote notifications)
        //   - simulator without APNs sandbox capability
        log.w(t) { "FCM token fetch failed" }
        null
    }
}
