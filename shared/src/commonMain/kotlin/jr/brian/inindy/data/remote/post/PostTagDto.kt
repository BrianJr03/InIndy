package jr.brian.inindy.data.remote.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostTagDto(
    @SerialName("post_id") val postId: String? = null,
    @SerialName("tag") val tag: String
)
