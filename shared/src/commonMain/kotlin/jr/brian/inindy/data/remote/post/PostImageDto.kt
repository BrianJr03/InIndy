package jr.brian.inindy.data.remote.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostImageDto(
    @SerialName("id") val id: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("storage_url") val storageUrl: String,
    @SerialName("sort_order") val sortOrder: Int = 0
)
