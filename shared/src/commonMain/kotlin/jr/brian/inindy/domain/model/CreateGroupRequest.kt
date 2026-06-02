package jr.brian.inindy.domain.model

data class CreateGroupRequest(
    val name: String,
    val description: String?,
    val coverImageUri: String?
)
