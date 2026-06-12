package jr.brian.inindy.data.remote.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RsvpWithUserDto(
    @SerialName("user_id") val userId: String,
    @SerialName("user") val user: UserDto
)
