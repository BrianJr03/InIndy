package jr.brian.inindy.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ImageCompressor(private val context: Context) {

    actual suspend fun compressToFile(uri: String): String = withContext(Dispatchers.IO) {
        val parsed = uri.toUri()
        val bitmap = loadBitmap(parsed) ?: error("Could not decode image: $uri")
        val scaled = scaleToMaxDimension(bitmap, MAX_DIMENSION_PX)
        val outputFile = File(
            File(context.cacheDir, "compressed").apply { mkdirs() },
            "${UUID.randomUUID()}.jpg"
        )
        FileOutputStream(outputFile).use { stream ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        Uri.fromFile(outputFile).toString()
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun calculateInSampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / sample > target * 2) sample *= 2
        return sample
    }

    private fun scaleToMaxDimension(bitmap: Bitmap, max: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= max) return bitmap
        val ratio = max.toFloat() / longest.toFloat()
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private companion object {
        const val MAX_DIMENSION_PX = 1200
        const val JPEG_QUALITY = 80
    }
}
