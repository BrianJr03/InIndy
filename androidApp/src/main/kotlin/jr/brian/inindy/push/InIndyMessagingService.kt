package jr.brian.inindy.push

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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
    // block on the inindy_default channel.
    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification ?: return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(n.title)
            .setContentText(n.body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
        // TODO Phase 4: PendingIntent -> MainActivity carrying message.data["post_id"].
        manager.notify(message.messageId?.hashCode() ?: 0, builder.build())
    }

    companion object {
        const val CHANNEL_ID = "inindy_default"
    }
}
