package jr.brian.inindy.data.media

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

@OptIn(ExperimentalForeignApi::class)
internal object ImageFileWriter {

    fun writeJpeg(image: UIImage, quality: Double = 0.95): String? {
        val data = UIImageJPEGRepresentation(image, quality) ?: return null
        val path = tempPath(extension = "jpg")
        return if (data.writeToFile(path, atomically = true)) {
            NSURL.fileURLWithPath(path).absoluteString
        } else null
    }

    fun copyToTemp(source: NSURL): String? {
        val ext = source.pathExtension ?: "jpg"
        val targetPath = tempPath(extension = ext)
        val target = NSURL.fileURLWithPath(targetPath)
        return if (NSFileManager.defaultManager.copyItemAtURL(source, target, null)) {
            target.absoluteString
        } else null
    }

    private fun tempPath(extension: String): String {
        val tmp = NSTemporaryDirectory()
        val filename = "${NSUUID().UUIDString}.$extension"
        return if (tmp.endsWith("/")) "$tmp$filename" else "$tmp/$filename"
    }
}
