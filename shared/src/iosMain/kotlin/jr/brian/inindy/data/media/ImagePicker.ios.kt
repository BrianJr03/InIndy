package jr.brian.inindy.data.media

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class ImagePicker {

    actual suspend fun pickSingle(): String? = present(selectionLimit = 1).firstOrNull()

    actual suspend fun pickMultiple(max: Int): List<String> {
        if (max <= 0) return emptyList()
        return present(selectionLimit = max)
    }

    private suspend fun present(selectionLimit: Int): List<String> {
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: return emptyList()

        val config = PHPickerConfiguration().apply {
            this.selectionLimit = selectionLimit.toLong()
            this.filter = PHPickerFilter.imagesFilter()
        }

        return suspendCancellableCoroutine { cont ->
            val controller = PHPickerViewController(configuration = config)
            val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                override fun picker(
                    picker: PHPickerViewController,
                    didFinishPicking: List<*>
                ) {
                    picker.dismissViewControllerAnimated(true) {
                        if (!cont.isActive) return@dismissViewControllerAnimated
                        @Suppress("UNCHECKED_CAST")
                        val results = didFinishPicking as List<PHPickerResult>
                        if (results.isEmpty()) {
                            cont.resume(emptyList())
                            return@dismissViewControllerAnimated
                        }
                        loadResults(results) { uris ->
                            if (cont.isActive) cont.resume(uris)
                        }
                    }
                }
            }
            controller.delegate = delegate
            cont.invokeOnCancellation { controller.dismissViewControllerAnimated(true, null) }
            root.presentViewController(controller, animated = true, completion = null)
        }
    }

    private fun loadResults(
        results: List<PHPickerResult>,
        onComplete: (List<String>) -> Unit
    ) {
        val outputs = MutableList<String?>(results.size) { null }
        var remaining = results.size
        results.forEachIndexed { index, result ->
            val provider = result.itemProvider
            provider.loadFileRepresentationForTypeIdentifier("public.image") { url, _ ->
                val nsUrl = url as? NSURL
                outputs[index] = nsUrl?.let { ImageFileWriter.copyToTemp(it) }
                remaining -= 1
                if (remaining == 0) onComplete(outputs.filterNotNull())
            }
        }
    }
}
