package jr.brian.inindy.domain.usecase

import jr.brian.inindy.domain.repository.RsvpRepository

class RsvpPostUseCase(private val rsvpRepository: RsvpRepository) {
    suspend operator fun invoke(postId: String): Result<Unit> = rsvpRepository.rsvp(postId)
    suspend fun unRsvp(postId: String): Result<Unit> = rsvpRepository.unRsvp(postId)
    fun isRsvpd(postId: String): Boolean = rsvpRepository.isRsvpd(postId)
}
