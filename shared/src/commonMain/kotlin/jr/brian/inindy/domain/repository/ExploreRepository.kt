package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ExploreRepository {
    fun getPosts(): Flow<Result<List<Post>>>
}
