package jr.brian.inindy.data.media

actual class ImageCompressor {
    actual suspend fun compressToFile(uri: String): String = uri
    actual suspend fun compress(uri: String): ByteArray = ByteArray(0)
}
