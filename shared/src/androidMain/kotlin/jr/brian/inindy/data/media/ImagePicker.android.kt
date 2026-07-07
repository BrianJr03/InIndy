package jr.brian.inindy.data.media

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class ImagePicker(
    private val activityProvider: ActivityProvider
) {

    private var singleLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var multipleLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null

    private val singleChannel = Channel<Uri?>(capacity = Channel.BUFFERED)
    private val multipleChannel = Channel<List<Uri>>(capacity = Channel.BUFFERED)

    private val mutex = Mutex()

    fun bindToActivity(activity: ComponentActivity, maxMultipleItems: Int = DEFAULT_MAX_ITEMS) {
        singleLauncher = activity.activityResultRegistry.register(
            KEY_SINGLE,
            activity,
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            singleChannel.trySend(uri)
        }
        multipleLauncher = activity.activityResultRegistry.register(
            KEY_MULTIPLE,
            activity,
            ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxMultipleItems)
        ) { uris ->
            multipleChannel.trySend(uris)
        }
    }

    actual suspend fun pickSingle(): String? = mutex.withLock {
        val launcher = singleLauncher ?: return@withLock null
        drainSingle()
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
        singleChannel.receive()?.toString()
    }

    actual suspend fun pickMultiple(max: Int): List<String> {
        if (max <= 0) return emptyList()
        if (max == 1) return listOfNotNull(pickSingle())
        return mutex.withLock {
            val launcher = multipleLauncher ?: return@withLock emptyList()
            drainMultiple()
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            multipleChannel.receive().take(max).map { it.toString() }
        }
    }

    private fun drainSingle() {
        while (singleChannel.tryReceive().isSuccess) Unit
    }

    private fun drainMultiple() {
        while (multipleChannel.tryReceive().isSuccess) Unit
    }

    private companion object {
        const val KEY_SINGLE = "inindy-image-picker-single"
        const val KEY_MULTIPLE = "inindy-image-picker-multiple"
        const val DEFAULT_MAX_ITEMS = 10
    }
}
