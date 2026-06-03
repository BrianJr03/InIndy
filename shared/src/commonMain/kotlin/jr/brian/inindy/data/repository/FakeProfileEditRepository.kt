package jr.brian.inindy.data.repository

import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Neighborhood
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.ProfileEditRepository
import kotlinx.coroutines.delay

class FakeProfileEditRepository(
    private val userPreferencesStore: UserPreferencesStore,
    private val mediaRepository: MediaRepository
) : ProfileEditRepository {

    override suspend fun updateProfile(
        fullName: String,
        avatarUri: String?,
        neighborhoodId: String,
        interests: List<Interest>
    ): Result<Unit> {
        delay(SAVE_DELAY_MS)
        val avatarCdnUrl = if (avatarUri != null) {
            mediaRepository.uploadAvatar(avatarUri).getOrNull()
        } else {
            null
        }
        userPreferencesStore.saveProfile(fullName.trim(), avatarCdnUrl)
        val neighborhoodName = SAMPLE_NEIGHBORHOODS
            .firstOrNull { it.id == neighborhoodId }?.name ?: neighborhoodId
        userPreferencesStore.saveNeighborhood(neighborhoodId, neighborhoodName)
        userPreferencesStore.saveInterests(interests)
        return Result.success(Unit)
    }

    override suspend fun getNeighborhoods(): Result<List<Neighborhood>> {
        delay(LOAD_DELAY_MS)
        return Result.success(SAMPLE_NEIGHBORHOODS)
    }

    private companion object {
        const val SAVE_DELAY_MS = 800L
        const val LOAD_DELAY_MS = 200L

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
