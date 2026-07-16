package jr.brian.inindy.di

import jr.brian.inindy.data.push.AndroidPushTokenProvider
import jr.brian.inindy.domain.push.PushTokenProvider

actual fun providePushTokenProvider(): PushTokenProvider = AndroidPushTokenProvider()
