package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Neighborhood

interface ProfileEditRepository {
    suspend fun updateProfile(
        fullName: String,
        avatarUri: String?,
        neighborhoodId: String,
        interests: List<Interest>
    ): Result<Unit>

    suspend fun getNeighborhoods(): Result<List<Neighborhood>>
}
