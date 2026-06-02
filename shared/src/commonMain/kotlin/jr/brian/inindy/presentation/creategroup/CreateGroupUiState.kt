package jr.brian.inindy.presentation.creategroup

data class CreateGroupUiState(
    val coverImageUri: String? = null,
    val coverUploadUrl: String? = null,
    val name: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val nameError: String? = null,
    val createdGroupId: String? = null
) {
    companion object {
        const val NAME_MIN_LENGTH = 3
        const val NAME_MAX_LENGTH = 50
        const val DESCRIPTION_MAX_LENGTH = 200
    }
}

val CreateGroupUiState.isValid: Boolean
    get() = name.trim().length >= CreateGroupUiState.NAME_MIN_LENGTH && !isSubmitting
