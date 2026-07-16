package jr.brian.inindy.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import jr.brian.inindy.MainActivity
import jr.brian.inindy.R
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.DeviceTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InIndyMessagingService : FirebaseMessagingService(), KoinComponent {

    private val deviceTokenRepository: DeviceTokenRepository by inject()
    private val currentUserProvider: CurrentUserProvider by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Fires when FCM rotates the token. Only persist if a user is signed in;
    // otherwise the next SignedIn re-registers it via PushRegistrar.
    override fun onNewToken(token: String) {
        scope.launch {
            if (currentUserProvider.get().userId != null) {
                deviceTokenRepository.upsertToken(token, "android")
            }
        }
    }

    // Only called in the foreground. Backgrounded/killed apps get the tray
    // notification auto-rendered by the system from the message's notification
    // block on the inindy_default channel — the background tap path lands in
    // MainActivity with the same intent extras via FCM's default launcher intent.
    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification ?: return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(n.title)
            .setContentText(n.body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(tapIntent(message))
        manager.notify(message.messageId?.hashCode() ?: 0, builder.build())
    }

    private fun tapIntent(message: RemoteMessage): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            message.data[EXTRA_POST_ID]?.let { putExtra(EXTRA_POST_ID, it) }
            message.data[EXTRA_GROUP_ID]?.let { putExtra(EXTRA_GROUP_ID, it) }
        }
        // Unique per-message requestCode so the extras don't get clobbered by
        // PendingIntent reuse when multiple notifications land back-to-back.
        val requestCode = message.messageId?.hashCode() ?: intent.hashCode()
        return PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "inindy_default"
        const val EXTRA_POST_ID = "post_id"
        const val EXTRA_GROUP_ID = "group_id"
    }
}
