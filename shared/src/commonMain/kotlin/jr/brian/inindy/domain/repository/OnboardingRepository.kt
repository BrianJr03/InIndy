package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Neighborhood

interface OnboardingRepository {
    suspend fun updateProfile(fullName: String, avatarUrl: String?): Result<Unit>
    suspend fun updateNeighborhood(neighborhoodId: String): Result<Unit>
    suspend fun updateInterests(interests: List<Interest>): Result<Unit>
    suspend fun completeOnboarding(): Result<Unit>
    suspend fun getNeighborhoods(): Result<List<Neighborhood>>
}
