package jr.brian.inindy.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import jr.brian.inindy.data.remote.push.DeviceTokenInsertDto
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.repository.DeviceTokenRepository

class SupabaseDeviceTokenRepository(
    private val supabase: SupabaseClient,
    private val currentUserProvider: CurrentUserProvider,
) : DeviceTokenRepository {

    override suspend fun upsertToken(token: String, platform: String): Result<Unit> =
        runCatching {
            val userId = currentUserProvider.get().userId ?: error("No signed-in user")
            supabase.from(DEVICE_TOKENS_TABLE).upsert(
                DeviceTokenInsertDto(userId = userId, token = token, platform = platform)
            ) {
                onConflict = "user_id,token"
                ignoreDuplicates = true
            }
        }.map { }

    override suspend fun deleteToken(token: String): Result<Unit> = runCatching {
        val userId = currentUserProvider.get().userId ?: return@runCatching
        supabase.from(DEVICE_TOKENS_TABLE).delete {
            filter {
                eq("user_id", userId)
                eq("token", token)
            }
        }
    }.map { }

    private companion object {
        const val DEVICE_TOKENS_TABLE = "device_tokens"
    }
}
