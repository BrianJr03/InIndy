package jr.brian.inindy.data.media

expect class ImageCompressor {
    suspend fun compressToFile(uri: String): String
    suspend fun compress(uri: String): ByteArray
}
