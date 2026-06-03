package jr.brian.inindy.data.media

expect class ImagePicker {
    suspend fun pickSingle(): String?
    suspend fun pickMultiple(max: Int): List<String>
}
