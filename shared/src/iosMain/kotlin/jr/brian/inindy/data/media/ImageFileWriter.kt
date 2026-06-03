package jr.brian.inindy.data.media

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.URLByAppendingPathComponent
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

@OptIn(ExperimentalForeignApi::class)
internal object ImageFileWriter {

    fun writeJpeg(image: UIImage, quality: Double = 0.95): String? {
        val data = UIImageJPEGRepresentation(image, quality) ?: return null
        val dir = NSFileManager.defaultManager.temporaryDirectory
        val target = dir.URLByAppendingPathComponent("${NSUUID().UUIDString}.jpg")
            ?: return null
        val ok = data.writeToURL(target, atomically = true)
        return if (ok) target.absoluteString else null
    }

    fun copyToTemp(source: NSURL): String? {
        val dir = NSFileManager.defaultManager.temporaryDirectory
        val target = dir.URLByAppendingPathComponent(
            "${NSUUID().UUIDString}.${source.pathExtension ?: "jpg"}"
        ) ?: return null
        return if (NSFileManager.defaultManager.copyItemAtURL(source, target, null)) {
            target.absoluteString
        } else null
    }
}
