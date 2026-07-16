package jr.brian.inindy.di

import jr.brian.inindy.domain.push.PushTokenProvider

/**
 * Returns the active PushTokenProvider for this platform.
 * On Android this is AndroidPushTokenProvider (FCM). iOS is a Phase 3 stub.
 */
expect fun providePushTokenProvider(): PushTokenProvider
