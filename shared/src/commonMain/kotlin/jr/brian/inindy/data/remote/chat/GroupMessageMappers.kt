package jr.brian.inindy.data.remote.chat

import jr.brian.inindy.data.remote.post.toEpochMillis
import jr.brian.inindy.domain.model.GroupMessage

fun GroupMessageDto.toDomain(): GroupMessage {
    val removed = redacted || deleted
    return GroupMessage(
        id = id,
        groupId = groupId,
        senderId = senderId,
        senderName = sender?.fullName,
        senderAvatarUrl = sender?.avatarUrl,
        body = if (removed) null else body,
        isRemoved = removed,
        createdAt = createdAt.toEpochMillis()
    )
}
