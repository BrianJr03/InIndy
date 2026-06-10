package jr.brian.inindy.data.remote.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePostDto(
    @SerialName("user_id") val userId: String,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("neighborhood_id") val neighborhoodId: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("location") val location: String,
    @SerialName("address") val address: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("max_attendees") val maxAttendees: Int? = null
)
