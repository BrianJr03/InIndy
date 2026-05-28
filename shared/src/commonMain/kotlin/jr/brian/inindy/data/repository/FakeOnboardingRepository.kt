package jr.brian.inindy.data.repository

import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Neighborhood
import jr.brian.inindy.domain.repository.OnboardingRepository
import kotlinx.coroutines.delay

class FakeOnboardingRepository : OnboardingRepository {

    override suspend fun updateProfile(fullName: String, avatarUrl: String?): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        return Result.success(Unit)
    }

    override suspend fun updateNeighborhood(neighborhoodId: String): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        return Result.success(Unit)
    }

    override suspend fun updateInterests(interests: List<Interest>): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        return Result.success(Unit)
    }

    override suspend fun getNeighborhoods(): Result<List<Neighborhood>> {
        delay(NETWORK_DELAY_MS / 2)
        return Result.success(SAMPLE_NEIGHBORHOODS)
    }

    private companion object {
        const val NETWORK_DELAY_MS = 800L

        val SAMPLE_NEIGHBORHOODS = listOf(
            Neighborhood("n1", "Broad Ripple", slug = "broad-ripple"),
            Neighborhood("n2", "Fountain Square", slug = "fountain-square"),
            Neighborhood("n3", "Mass Ave", slug = "mass-ave"),
            Neighborhood("n4", "Fletcher Place", slug = "fletcher-place"),
            Neighborhood("n5", "Irvington", slug = "irvington"),
            Neighborhood("n6", "SoBro", slug = "sobro"),
            Neighborhood("n7", "Meridian-Kessler", slug = "meridian-kessler"),
            Neighborhood("n8", "Garfield Park", slug = "garfield-park"),
            Neighborhood("n9", "Downtown", slug = "downtown"),
            Neighborhood("n10", "Bates-Hendricks", slug = "bates-hendricks"),
            Neighborhood("n11", "Speedway", slug = "speedway"),
            Neighborhood("n12", "Castleton", slug = "castleton")
        )
    }
}
