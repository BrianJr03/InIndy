package jr.brian.inindy.data.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import jr.brian.inindy.data.media.ActivityProvider
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual class NotificationPermissionManager(private val activityProvider: ActivityProvider) {

    actual suspend fun requestPermission(): NotificationPermissionResult {
        // POST_NOTIFICATIONS is a runtime permission only from API 33.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationPermissionResult.Granted
        }
        return suspendCancellableCoroutine { cont ->
            val activity = activityProvider.current() ?: run {
                cont.resume(NotificationPermissionResult.Denied)
                return@suspendCancellableCoroutine
            }
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                cont.resume(NotificationPermissionResult.Granted)
                return@suspendCancellableCoroutine
            }
            val key = "notification-permission-${UUID.randomUUID()}"
            val launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                val result = when {
                    granted -> NotificationPermissionResult.Granted
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) ->
                        NotificationPermissionResult.Denied
                    else -> NotificationPermissionResult.PermanentlyDenied
                }
                if (cont.isActive) cont.resume(result)
            }
            cont.invokeOnCancellation { launcher.unregister() }
            launcher.launch(permission)
        }
    }

    actual suspend fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val activity = activityProvider.current() ?: return false
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
