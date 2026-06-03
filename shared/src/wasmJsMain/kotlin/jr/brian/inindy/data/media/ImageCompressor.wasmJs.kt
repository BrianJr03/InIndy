package jr.brian.inindy.data.media

actual class ImageCompressor {
    actual suspend fun compressToFile(uri: String): String = uri
}
