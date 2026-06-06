package jr.brian.inindy.presentation.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.model.CreateGroupRequest
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    private val groupRepository: GroupRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    fun onIntent(intent: CreateGroupIntent) {
        when (intent) {
            is CreateGroupIntent.CoverImageSelected -> setCoverImage(intent.uri)
            CreateGroupIntent.RemoveCoverImage -> removeCoverImage()
            is CreateGroupIntent.NameChanged -> setName(intent.text)
            is CreateGroupIntent.DescriptionChanged -> setDescription(intent.text)
            CreateGroupIntent.Submit -> submit()
        }
    }

    private fun setCoverImage(uri: String) {
        _uiState.value = _uiState.value.copy(
            coverImageUri = uri,
            coverUploadUrl = null
        )
    }

    private fun removeCoverImage() {
        _uiState.value = _uiState.value.copy(
            coverImageUri = null,
            coverUploadUrl = null
        )
    }

    private fun setName(text: String) {
        _uiState.value = _uiState.value.copy(
            name = text.take(CreateGroupUiState.NAME_MAX_LENGTH),
            nameError = null
        )
    }

    private fun setDescription(text: String) {
        _uiState.value = _uiState.value.copy(
            description = text.take(CreateGroupUiState.DESCRIPTION_MAX_LENGTH)
        )
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val trimmedName = state.name.trim()
        if (trimmedName.length < CreateGroupUiState.NAME_MIN_LENGTH) {
            _uiState.value = state.copy(
                nameError = NAME_TOO_SHORT_ERROR
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSubmitting = true,
                submitError = null,
                nameError = null
            )

            val coverUrl = state.coverImageUri?.let { uri ->
                val uploadResult = mediaRepository.uploadGroupCover(uri)
                uploadResult.fold(
                    onSuccess = { url -> url },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            submitError = error.message ?: COVER_UPLOAD_ERROR
                        )
                        return@launch
                    }
                )
            }

            val trimmedDescription = state.description.trim().takeIf { it.isNotEmpty() }
            val request = CreateGroupRequest(
                name = trimmedName,
                description = trimmedDescription,
                coverImageUri = coverUrl
            )

            groupRepository.createGroup(request)
                .onSuccess { group ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        coverUploadUrl = coverUrl,
                        createdGroupId = group.id
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitError = error.message ?: CREATE_FAILED_ERROR
                    )
                }
        }
    }

    private companion object {
        const val NAME_TOO_SHORT_ERROR = "Group name must be at least 3 characters"
        const val COVER_UPLOAD_ERROR = "Couldn't upload cover photo — try again"
        const val CREATE_FAILED_ERROR = "Couldn't create group — try again"
    }
}
