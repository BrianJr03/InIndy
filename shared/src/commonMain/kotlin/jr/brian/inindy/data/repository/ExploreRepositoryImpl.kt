package jr.brian.inindy.data.repository

import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.repository.ExploreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class ExploreRepositoryImpl : ExploreRepository {
    private val postsState = MutableStateFlow(listOf<Post>())
    override fun getPosts(): Flow<Result<List<Post>>> =
        postsState.asStateFlow().map { Result.success(it) }
}
