package jr.brian.inindy.data.repository

import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Neighborhood
import jr.brian.inindy.domain.repository.OnboardingRepository
import kotlinx.coroutines.delay

class FakeOnboardingRepository(
    private val userPreferencesStore: UserPreferencesStore
) : OnboardingRepository {

    override suspend fun updateProfile(fullName: String, avatarUrl: String?): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        userPreferencesStore.saveProfile(fullName, avatarUrl)
        return Result.success(Unit)
    }

    override suspend fun updateNeighborhood(neighborhoodId: String): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        val name = SAMPLE_NEIGHBORHOODS.firstOrNull { it.id == neighborhoodId }?.name ?: neighborhoodId
        userPreferencesStore.saveNeighborhood(neighborhoodId, name)
        return Result.success(Unit)
    }

    override suspend fun updateInterests(interests: List<Interest>): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        userPreferencesStore.saveInterests(interests)
        return Result.success(Unit)
    }

    override suspend fun completeOnboarding(): Result<Unit> {
        userPreferencesStore.setOnboardingComplete(true)
        return Result.success(Unit)
    }

    override suspend fun getNeighborhoods(): Result<List<Neighborhood>> {
        delay(NETWORK_DELAY_MS / 2)
        return Result.success(SAMPLE_NEIGHBORHOODS)
    }

    private companion object {
        const val NETWORK_DELAY_MS = 800L

        val SAMPLE_NEIGHBORHOODS = listOf(
            Neighborhood("broad_ripple", "Broad Ripple", slug = "broad-ripple"),
            Neighborhood("fountain_square", "Fountain Square", slug = "fountain-square"),
            Neighborhood("mass_ave", "Mass Ave", slug = "mass-ave"),
            Neighborhood("irvington", "Irvington", slug = "irvington"),
            Neighborhood("downtown", "Downtown", slug = "downtown"),
            Neighborhood("bates_hendricks", "Bates-Hendricks", slug = "bates-hendricks"),
            Neighborhood("cottage_home", "Cottage Home", slug = "cottage-home"),
            Neighborhood("herron_morton", "Herron-Morton Place", slug = "herron-morton"),
            Neighborhood("butler_tarkington", "Butler-Tarkington", slug = "butler-tarkington"),
            Neighborhood("meridian_kessler", "Meridian-Kessler", slug = "meridian-kessler")
        )
    }
}
