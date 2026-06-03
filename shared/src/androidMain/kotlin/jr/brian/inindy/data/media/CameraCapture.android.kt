package jr.brian.inindy.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual class CameraCapture(
    private val context: Context,
    private val activityProvider: ActivityProvider
) {

    actual suspend fun capturePhoto(): CameraResult {
        val activity = activityProvider.current()
            ?: return CameraResult.Error("No active Activity")

        val permission = checkCameraPermission(activity)
        if (permission != CameraResult.Success("")) return permission

        val photoFile = createPhotoFile() ?: return CameraResult.Error("Could not create photo file")
        val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)

        return suspendCancellableCoroutine { cont ->
            val key = "camera-capture-${UUID.randomUUID()}"
            val launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.TakePicture()
            ) { saved ->
                if (cont.isActive) {
                    val result = if (saved == true) {
                        CameraResult.Success(photoUri.toString())
                    } else {
                        photoFile.delete()
                        CameraResult.Cancelled
                    }
                    cont.resume(result)
                }
            }
            cont.invokeOnCancellation {
                launcher.unregister()
                photoFile.delete()
            }
            launcher.launch(photoUri)
        }
    }

    private suspend fun checkCameraPermission(activity: ComponentActivity): CameraResult {
        val granted = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) return CameraResult.Success("")

        return suspendCancellableCoroutine { cont ->
            val key = "camera-permission-${UUID.randomUUID()}"
            val shouldShowBefore = ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.CAMERA
            )
            val launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!cont.isActive) return@register
                val result = when {
                    isGranted -> CameraResult.Success("")
                    !shouldShowBefore && !ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, Manifest.permission.CAMERA
                    ) -> CameraResult.PermissionPermanentlyDenied
                    else -> CameraResult.PermissionDenied
                }
                cont.resume(result)
            }
            cont.invokeOnCancellation { launcher.unregister() }
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun createPhotoFile(): File? {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }
}
