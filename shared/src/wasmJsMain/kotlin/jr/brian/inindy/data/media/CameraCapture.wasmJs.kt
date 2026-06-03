package jr.brian.inindy.data.media

actual class CameraCapture {
    actual suspend fun capturePhoto(): CameraResult =
        CameraResult.Error("Camera not supported on web")
}
