package jr.brian.inindy.data.repository

import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.delay

class FakeMediaRepository : MediaRepository {

    override suspend fun uploadPostImage(uri: String): Result<String> {
        delay(UPLOAD_DELAY_MS)
        return Result.success("https://picsum.photos/seed/post-${seed()}/1200/800")
    }

    override suspend fun uploadAvatar(uri: String): Result<String> {
        delay(UPLOAD_DELAY_MS)
        return Result.success("https://picsum.photos/seed/avatar-${seed()}/200/200")
    }

    override suspend fun uploadGroupCover(uri: String): Result<String> {
        delay(UPLOAD_DELAY_MS)
        return Result.success("https://picsum.photos/seed/group-${seed()}/800/400")
    }

    private fun seed(): String = currentTimeMillis().toString()

    private companion object {
        const val UPLOAD_DELAY_MS = 1_500L
    }
}
