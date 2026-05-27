package jr.brian.inindy.domain.usecase

import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.repository.ExploreRepository
import kotlinx.coroutines.flow.Flow

class GetExplorePostsUseCase(private val repository: ExploreRepository) {
    operator fun invoke(): Flow<Result<List<Post>>> = repository.getPosts()
}
