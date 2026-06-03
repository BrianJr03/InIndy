package jr.brian.inindy.data.media

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual class ImagePicker(
    private val activityProvider: ActivityProvider
) {

    actual suspend fun pickSingle(): String? {
        val activity = activityProvider.current() ?: return null
        return suspendCancellableCoroutine { cont ->
            val key = "image-picker-single-${UUID.randomUUID()}"
            val launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (cont.isActive) cont.resume(uri?.toString())
            }
            cont.invokeOnCancellation { launcher.unregister() }
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    actual suspend fun pickMultiple(max: Int): List<String> {
        if (max <= 0) return emptyList()
        if (max == 1) return listOfNotNull(pickSingle())

        val activity = activityProvider.current() ?: return emptyList()
        return suspendCancellableCoroutine { cont ->
            val key = "image-picker-multi-${UUID.randomUUID()}"
            val launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.PickMultipleVisualMedia(maxItems = max)
            ) { uris ->
                if (cont.isActive) cont.resume(uris.map { it.toString() })
            }
            cont.invokeOnCancellation { launcher.unregister() }
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
}
