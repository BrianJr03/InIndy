package jr.brian.inindy.domain.usecase

import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.repository.ExploreRepository

class RsvpPostUseCase(private val repository: ExploreRepository) {
    suspend operator fun invoke(postId: String): Result<Post> = repository.rsvp(postId)
    fun isRsvpd(postId: String): Boolean = repository.isRsvpd(postId)
}
