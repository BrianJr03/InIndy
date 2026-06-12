package jr.brian.inindy.data.remote.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostNeighborhoodDto(
    @SerialName("name") val name: String
)
