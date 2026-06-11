package jr.brian.inindy.data.repository

import jr.brian.inindy.data.media.ImageCompressor
import jr.brian.inindy.data.remote.media.MediaRemoteDataSource
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.util.currentTimeMillis

class MediaRepositoryImpl(
    private val remoteDataSource: MediaRemoteDataSource,
    private val imageCompressor: ImageCompressor
) : MediaRepository {

    override suspend fun uploadPostImage(uri: String): Result<String> =
        uploadInternal(uri, context = "post", fileName = "post_${currentTimeMillis()}.jpg")

    override suspend fun uploadAvatar(uri: String): Result<String> =
        uploadInternal(uri, context = "avatar", fileName = "avatar.jpg")

    override suspend fun uploadGroupCover(uri: String): Result<String> =
        uploadInternal(uri, context = "group", fileName = "cover.jpg")

    private suspend fun uploadInternal(
        uri: String,
        context: String,
        fileName: String
    ): Result<String> = runCatching {
        require(!uri.startsWith("http")) {
            "uploadInternal received a remote URL instead of a local URI: $uri"
        }
        val bytes = imageCompressor.compress(uri)
        val uploadResponse = remoteDataSource
            .getUploadUrl(fileName = fileName, contentType = CONTENT_TYPE, context = context)
            .getOrThrow()
        remoteDataSource
            .uploadImage(uploadResponse.uploadUrl, bytes, CONTENT_TYPE)
            .getOrThrow()
        uploadResponse.publicUrl
    }

    private companion object {
        const val CONTENT_TYPE = "image/jpeg"
    }
}
