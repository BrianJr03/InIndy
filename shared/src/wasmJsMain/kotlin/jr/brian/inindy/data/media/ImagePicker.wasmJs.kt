package jr.brian.inindy.data.media

actual class ImagePicker {
    actual suspend fun pickSingle(): String? = null
    actual suspend fun pickMultiple(max: Int): List<String> = emptyList()
}
