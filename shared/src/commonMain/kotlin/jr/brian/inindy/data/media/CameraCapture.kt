package jr.brian.inindy.data.media

expect class CameraCapture {
    suspend fun capturePhoto(): CameraResult
}

sealed class CameraResult {
    data class Success(val uri: String) : CameraResult()
    object Cancelled : CameraResult()
    object PermissionDenied : CameraResult()
    object PermissionPermanentlyDenied : CameraResult()
    data class Error(val message: String) : CameraResult()
}
