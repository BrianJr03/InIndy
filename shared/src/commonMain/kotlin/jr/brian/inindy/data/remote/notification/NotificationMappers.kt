package jr.brian.inindy.data.remote.notification

import jr.brian.inindy.data.remote.post.toEpochMillis
import jr.brian.inindy.domain.model.Notification
import jr.brian.inindy.domain.model.NotificationType

fun NotificationDto.toDomain(): Notification = Notification(
    id = id,
    type = NotificationType.fromServer(type),
    actorId = actor?.id ?: actorId,
    actorName = actor?.fullName,
    actorAvatarUrl = actor?.avatarUrl,
    groupId = group?.id ?: groupId,
    groupName = group?.name,
    postId = postId,
    read = read,
    createdAt = createdAt.toEpochMillis()
)
