package jr.brian.inindy.domain.repository

interface MediaRepository {
    suspend fun uploadPostImage(uri: String): Result<String>
    suspend fun uploadAvatar(uri: String): Result<String>
    suspend fun uploadGroupCover(uri: String): Result<String>
}
