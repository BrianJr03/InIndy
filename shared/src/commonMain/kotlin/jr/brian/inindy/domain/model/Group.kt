package jr.brian.inindy.domain.model

data class Group(
    val id: String,
    val name: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val createdBy: String,
    val isOpen: Boolean = false,
    val memberCount: Int = 1,
    val role: GroupRole = GroupRole.MEMBER,
    val createdAt: Long = 0L
)

enum class GroupRole { ADMIN, MEMBER }

data class GroupMember(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: GroupRole,
    val joinedAt: Long
)

data class GroupInvite(
    val id: String,
    val groupId: String,
    val invitedEmail: String,
    val token: String,
    val expiresAt: Long
)
