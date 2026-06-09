package jr.brian.inindy.domain.model

data class CreatePostRequest(
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val startsAt: Long,
    val endsAt: Long?,
    val tags: List<Interest>,
    val imageUris: List<String>,
    val audience: PostAudience,
    val maxAttendees: Int?
)

sealed class PostAudience {
    object Neighborhood : PostAudience()
    data class GroupAudience(val groupId: String) : PostAudience()
}

data class AddressResult(
    val address: String,
    val latitude: Double,
    val longitude: Double
)
