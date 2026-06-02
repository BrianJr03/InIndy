package jr.brian.inindy.domain.model

data class GroupInvite(
    val id: String,
    val groupId: String,
    val invitedBy: String,
    val token: String,
    val createdAt: Long,
    val expiresAt: Long
)
