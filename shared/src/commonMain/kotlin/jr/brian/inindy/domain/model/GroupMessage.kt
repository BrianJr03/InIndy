package jr.brian.inindy.domain.model

data class GroupMessage(
    val id: String,
    val groupId: String,
    val senderId: String,
    val senderName: String?,
    val senderAvatarUrl: String?,
    // Null when the message is redacted or deleted — UI should render as
    // "message removed" and never fall back to the raw body.
    val body: String?,
    val isRemoved: Boolean,
    val createdAt: Long
) {
    val isOwnedBy: (String) -> Boolean get() = { it == senderId }
}
