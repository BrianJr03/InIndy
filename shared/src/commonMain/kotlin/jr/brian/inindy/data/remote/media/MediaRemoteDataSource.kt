package jr.brian.inindy.data.remote.media

interface MediaRemoteDataSource {
    suspend fun getUploadUrl(
        fileName: String,
        contentType: String,
        context: String
    ): Result<UploadUrlResponse>

    suspend fun uploadImage(
        uploadUrl: String,
        bytes: ByteArray,
        contentType: String = "image/jpeg"
    ): Result<Unit>
}
