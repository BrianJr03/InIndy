package jr.brian.inindy.presentation.creategroup

sealed class CreateGroupIntent {
    data class CoverImageSelected(val uri: String) : CreateGroupIntent()
    object RemoveCoverImage : CreateGroupIntent()
    data class NameChanged(val text: String) : CreateGroupIntent()
    data class DescriptionChanged(val text: String) : CreateGroupIntent()
    object Submit : CreateGroupIntent()
}
