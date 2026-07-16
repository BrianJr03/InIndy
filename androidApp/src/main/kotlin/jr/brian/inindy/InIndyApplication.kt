package jr.brian.inindy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import jr.brian.inindy.di.initKoin
import jr.brian.inindy.push.InIndyMessagingService
import jr.brian.inindy.util.setIsDebugBuild
import org.koin.android.ext.koin.androidContext

class InIndyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Must run before initKoin so [initAppLogging] sees the right severity.
        setIsDebugBuild(BuildConfig.DEBUG)
        initKoin {
            androidContext(this@InIndyApplication)
        }
        createDefaultChannel()
    }

    private fun createDefaultChannel() {
        // NotificationChannel is Oreo (API 26) only; minSdk is 24.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            InIndyMessagingService.CHANNEL_ID,
            "General",
            NotificationManager.IMPORTANCE_HIGH,
        )
        manager.createNotificationChannel(channel)
    }
}
