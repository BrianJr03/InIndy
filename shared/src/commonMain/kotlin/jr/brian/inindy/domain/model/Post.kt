package jr.brian.inindy.domain.model

data class Post(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val startsAt: Long,
    val endsAt: Long?,
    val createdAt: Long,
    val tags: List<Interest>,
    val images: List<String>,
    val videos: List<VideoMedia>,
    val rsvpCount: Int,
    val author: User?,
    val neighborhoodId: String? = null,
    val neighborhoodName: String? = null,
    val groupId: String? = null
)
