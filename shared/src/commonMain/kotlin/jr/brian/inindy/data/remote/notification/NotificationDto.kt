package jr.brian.inindy.data.remote.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("type") val type: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("read") val read: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("actor") val actor: NotificationActorDto? = null,
    @SerialName("group") val group: NotificationGroupDto? = null
)

@Serializable
data class NotificationActorDto(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class NotificationGroupDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String
)
