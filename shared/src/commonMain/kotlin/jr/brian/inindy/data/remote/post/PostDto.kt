package jr.brian.inindy.data.remote.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PostDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("neighborhood_id") val neighborhoodId: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("location") val location: JsonElement? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("max_attendees") val maxAttendees: Int? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("author") val author: UserDto? = null,
    @SerialName("neighborhood") val neighborhood: PostNeighborhoodDto? = null,
    @SerialName("images") val images: List<PostImageDto> = emptyList(),
    @SerialName("tags") val tags: List<PostTagDto> = emptyList(),
    @SerialName("rsvp_count") val rsvpCount: Int = 0,
    @SerialName("rsvps") val rsvps: List<RsvpWithUserDto> = emptyList()
)
