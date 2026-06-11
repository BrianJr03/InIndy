package jr.brian.inindy.data.media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class ImageCompressor {

    actual suspend fun compressToFile(uri: String): String = withContext(Dispatchers.Default) {
        val sourceUrl = NSURL.URLWithString(uri) ?: NSURL.fileURLWithPath(uri)
        val original = loadImage(sourceUrl) ?: error("Could not load image: $uri")
        val scaled = scale(original, MAX_DIMENSION_PT)
        val data = UIImageJPEGRepresentation(scaled, JPEG_QUALITY)
            ?: error("Could not encode JPEG")
        val tmp = NSTemporaryDirectory()
        val filename = "${NSUUID().UUIDString}.jpg"
        val path = if (tmp.endsWith("/")) "$tmp$filename" else "$tmp/$filename"
        if (!data.writeToFile(path, atomically = true)) {
            error("Could not write compressed file")
        }
        NSURL.fileURLWithPath(path).absoluteString ?: error("Output URL has no absoluteString")
    }

    actual suspend fun compress(uri: String): ByteArray = withContext(Dispatchers.Default) {
        val sourceUrl = NSURL.URLWithString(uri) ?: NSURL.fileURLWithPath(uri)
        val original = loadImage(sourceUrl) ?: error("Could not load image: $uri")
        val scaled = scale(original, MAX_DIMENSION_PT)
        val data = UIImageJPEGRepresentation(scaled, JPEG_QUALITY)
            ?: error("Could not encode JPEG")
        data.toByteArray()
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        val bytes = ByteArray(size)
        if (size == 0) return bytes
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, length)
        }
        return bytes
    }

    private fun loadImage(url: NSURL): UIImage? {
        val path = url.path ?: return null
        return UIImage.imageWithContentsOfFile(path)
    }

    private fun scale(image: UIImage, maxDimension: Double): UIImage {
        val (targetW, targetH) = image.size.useContents {
            val longest = maxOf(width, height)
            if (longest <= maxDimension) {
                width to height
            } else {
                val ratio = maxDimension / longest
                (width * ratio) to (height * ratio)
            }
        }
        val originalLongest = image.size.useContents { maxOf(width, height) }
        if (originalLongest <= maxDimension) return image
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(targetW, targetH))
        return renderer.imageWithActions { _ ->
            image.drawInRect(CGRectMake(0.0, 0.0, targetW, targetH))
        }
    }

    private companion object {
        const val MAX_DIMENSION_PT = 1200.0
        const val JPEG_QUALITY = 0.8
    }
}
