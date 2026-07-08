package jr.brian.inindy.domain.model

data class Notification(
    val id: String,
    val type: NotificationType,
    val actorId: String?,
    val actorName: String?,
    val actorAvatarUrl: String?,
    val groupId: String?,
    val groupName: String?,
    val postId: String?,
    val read: Boolean,
    val createdAt: Long
)

enum class NotificationType {
    GROUP_POST,
    UNKNOWN;

    companion object {
        fun fromServer(value: String?): NotificationType = when (value) {
            "group_post" -> GROUP_POST
            else -> UNKNOWN
        }
    }
}
