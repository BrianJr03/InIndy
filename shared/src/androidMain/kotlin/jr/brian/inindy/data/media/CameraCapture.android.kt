package jr.brian.inindy.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class CameraCapture(
    private val context: Context,
    private val activityProvider: ActivityProvider
) {

    private var pictureLauncher: ActivityResultLauncher<Uri>? = null
    private var permissionLauncher: ActivityResultLauncher<String>? = null

    private val pictureChannel = Channel<Boolean>(capacity = Channel.BUFFERED)
    private val permissionChannel = Channel<Boolean>(capacity = Channel.BUFFERED)

    private var rationaleBeforeRequest: Boolean = false
    private val mutex = Mutex()

    fun bindToActivity(activity: ComponentActivity) {
        pictureLauncher = activity.activityResultRegistry.register(
            KEY_PICTURE,
            activity,
            ActivityResultContracts.TakePicture()
        ) { saved ->
            pictureChannel.trySend(saved == true)
        }
        permissionLauncher = activity.activityResultRegistry.register(
            KEY_PERMISSION,
            activity,
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permissionChannel.trySend(granted)
        }
    }

    actual suspend fun capturePhoto(): CameraResult = mutex.withLock {
        val picture = pictureLauncher
            ?: return@withLock CameraResult.Error("Camera not initialized")

        val permissionResult = ensureCameraPermission()
        if (permissionResult != null) return@withLock permissionResult

        val photoFile = createPhotoFile()
            ?: return@withLock CameraResult.Error("Could not create photo file")
        val photoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )

        drain(pictureChannel)
        picture.launch(photoUri)
        val saved = pictureChannel.receive()

        if (saved) {
            CameraResult.Success(photoUri.toString())
        } else {
            photoFile.delete()
            CameraResult.Cancelled
        }
    }

    private suspend fun ensureCameraPermission(): CameraResult? {
        val activity = activityProvider.current()
            ?: return CameraResult.Error("No active Activity")
        val granted = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return null

        val permission = permissionLauncher
            ?: return CameraResult.Error("Permission launcher not initialized")

        rationaleBeforeRequest = ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.CAMERA
        )

        drain(permissionChannel)
        permission.launch(Manifest.permission.CAMERA)
        val isGranted = permissionChannel.receive()
        if (isGranted) return null

        val rationaleAfter = activityProvider.current()?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: false
        return if (!rationaleBeforeRequest && !rationaleAfter) {
            CameraResult.PermissionPermanentlyDenied
        } else {
            CameraResult.PermissionDenied
        }
    }

    private fun drain(channel: Channel<Boolean>) {
        while (channel.tryReceive().isSuccess) Unit
    }

    private fun createPhotoFile(): File? {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    private companion object {
        const val KEY_PICTURE = "inindy-camera-capture"
        const val KEY_PERMISSION = "inindy-camera-permission"
    }
}
