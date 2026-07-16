package jr.brian.inindy.data.push

import com.google.firebase.messaging.FirebaseMessaging
import jr.brian.inindy.domain.push.PushTokenProvider
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidPushTokenProvider : PushTokenProvider {
    override val platform: String = "android"

    // Wrap the Task API directly so we don't need the play-services-coroutines
    // artifact just for one await().
    override suspend fun currentToken(): String? =
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                cont.resume(if (task.isSuccessful) task.result else null)
            }
        }
}
