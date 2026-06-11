package jr.brian.inindy.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Neighborhood
import jr.brian.inindy.domain.repository.OnboardingRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseOnboardingRepository(
    private val supabase: SupabaseClient,
    private val userPreferencesStore: UserPreferencesStore
) : OnboardingRepository {
    override suspend fun getNeighborhoods(): Result<List<Neighborhood>> = runCatching {
        supabase.from(NEIGHBORHOODS_TABLE)
            .select { order("name", order = Order.ASCENDING) }
            .decodeList<NeighborhoodDto>()
            .map { it.toDomain() }
    }

    override suspend fun updateProfile(fullName: String, avatarUrl: String?): Result<Unit> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No signed-in user")
        supabase.from(USERS_TABLE).upsert(
            UserProfileUpsertDto(id = userId, fullName = fullName, avatarUrl = avatarUrl)
        )
        userPreferencesStore.saveProfile(fullName, avatarUrl)
    }

    override suspend fun updateNeighborhood(neighborhoodId: String): Result<Unit> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No signed-in user")
        supabase.from(USERS_TABLE).upsert(
            UserNeighborhoodUpsertDto(id = userId, neighborhoodId = neighborhoodId)
        )
        val neighborhoodName = supabase.from(NEIGHBORHOODS_TABLE)
            .select { filter { eq("id", neighborhoodId) } }
            .decodeSingleOrNull<NeighborhoodDto>()
            ?.name
            ?: neighborhoodId
        userPreferencesStore.saveNeighborhood(neighborhoodId, neighborhoodName)
    }

    override suspend fun updateInterests(interests: List<Interest>): Result<Unit> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No signed-in user")
        supabase.from(USER_INTERESTS_TABLE).delete {
            filter { eq("user_id", userId) }
        }
        if (interests.isNotEmpty()) {
            val rows = interests.map { UserInterestDto(userId = userId, interest = it.name) }
            supabase.from(USER_INTERESTS_TABLE).insert(rows)
        }
        userPreferencesStore.saveInterests(interests)
    }

    override suspend fun completeOnboarding(): Result<Unit> = runCatching {
        userPreferencesStore.setOnboardingComplete(true)
    }

    @Serializable
    private data class NeighborhoodDto(
        @SerialName("id") val id: String,
        @SerialName("name") val name: String,
        @SerialName("city") val city: String = "Indianapolis",
        @SerialName("slug") val slug: String
    ) {
        fun toDomain(): Neighborhood = Neighborhood(id = id, name = name, city = city, slug = slug)
    }

    @Serializable
    private data class UserProfileUpsertDto(
        val id: String,
        @SerialName("full_name") val fullName: String,
        @SerialName("avatar_url") val avatarUrl: String?
    )

    @Serializable
    private data class UserNeighborhoodUpsertDto(
        val id: String,
        @SerialName("neighborhood_id") val neighborhoodId: String
    )

    @Serializable
    private data class UserInterestDto(
        @SerialName("user_id") val userId: String,
        val interest: String
    )

    private companion object {
        const val USERS_TABLE = "users"
        const val USER_INTERESTS_TABLE = "user_interests"
        const val NEIGHBORHOODS_TABLE = "neighborhoods"
    }
}
