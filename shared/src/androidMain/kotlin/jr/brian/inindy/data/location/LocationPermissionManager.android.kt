package jr.brian.inindy.data.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import jr.brian.inindy.data.media.ActivityProvider
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual class LocationPermissionManager(private val activityProvider: ActivityProvider) {

    actual suspend fun requestPermission(): LocationPermissionResult =
        suspendCancellableCoroutine { cont ->
            val activity = activityProvider.current() ?: run {
                cont.resume(LocationPermissionResult.Denied)
                return@suspendCancellableCoroutine
            }
            val permission = Manifest.permission.ACCESS_FINE_LOCATION
            if (ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                cont.resume(LocationPermissionResult.Granted)
                return@suspendCancellableCoroutine
            }
            val key = "location-permission-${UUID.randomUUID()}"
            val launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                val result = when {
                    granted -> LocationPermissionResult.Granted
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) ->
                        LocationPermissionResult.Denied
                    else -> LocationPermissionResult.PermanentlyDenied
                }
                if (cont.isActive) cont.resume(result)
            }
            cont.invokeOnCancellation { launcher.unregister() }
            launcher.launch(permission)
        }

    actual fun hasPermission(): Boolean {
        val activity = activityProvider.current() ?: return false
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
