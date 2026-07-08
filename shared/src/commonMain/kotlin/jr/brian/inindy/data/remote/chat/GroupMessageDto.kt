package jr.brian.inindy.data.remote.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMessageDto(
    @SerialName("id") val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("body") val body: String,
    @SerialName("redacted") val redacted: Boolean = false,
    @SerialName("deleted") val deleted: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("sender") val sender: GroupMessageSenderDto? = null
)

@Serializable
data class GroupMessageSenderDto(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)
