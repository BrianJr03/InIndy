package jr.brian.inindy.domain.repository

interface DeviceTokenRepository {
    suspend fun upsertToken(token: String, platform: String): Result<Unit>
    suspend fun deleteToken(token: String): Result<Unit>
}
