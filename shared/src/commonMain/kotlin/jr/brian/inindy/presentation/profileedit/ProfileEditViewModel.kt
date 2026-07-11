package jr.brian.inindy.presentation.profileedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.repository.ProfileEditRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileEditViewModel(
    private val currentUserProvider: CurrentUserProvider,
    private val profileEditRepository: ProfileEditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        onIntent(ProfileEditIntent.Load)
    }

    fun onIntent(intent: ProfileEditIntent) {
        when (intent) {
            ProfileEditIntent.Load -> load()
            ProfileEditIntent.Dismiss -> dismiss()
            is ProfileEditIntent.AvatarSelected -> selectAvatar(intent.uri)
            ProfileEditIntent.RemoveAvatar -> removeAvatar()
            is ProfileEditIntent.NameChanged -> changeName(intent.text)
            is ProfileEditIntent.NeighborhoodSelected -> selectNeighborhood(intent.neighborhoodId)
            is ProfileEditIntent.ToggleInterest -> toggleInterest(intent.interest)
            ProfileEditIntent.Save -> save()
            ProfileEditIntent.ClearError -> _uiState.update { it.copy(saveError = null) }
        }
    }

    private fun dismiss() {
        _uiState.update {
            it.copy(saved = false)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    saved = false
                )
            }
            val prefs = currentUserProvider.get()
            val neighborhoods = profileEditRepository.getNeighborhoods()
                .getOrDefault(emptyList())
            val interests = Interest.fromStorageNames(prefs.interests).toSet()
            val fullName = prefs.fullName ?: ""
            val neighborhoodId = prefs.neighborhoodId ?: ""
            _uiState.update {
                it.copy(
                    currentAvatarUrl = prefs.avatarUrl,
                    fullName = fullName,
                    neighborhoodId = neighborhoodId,
                    selectedInterests = interests,
                    neighborhoods = neighborhoods,
                    initialAvatarUrl = prefs.avatarUrl,
                    initialFullName = fullName,
                    initialNeighborhoodId = neighborhoodId,
                    initialInterests = interests,
                    isLoading = false
                )
            }
        }
    }

    private fun selectAvatar(uri: String) {
        _uiState.update { it.copy(newAvatarUri = uri) }
    }

    private fun removeAvatar() {
        _uiState.update { it.copy(newAvatarUri = null) }
    }

    private fun changeName(text: String) {
        _uiState.update { it.copy(fullName = text, nameError = null) }
    }

    private fun selectNeighborhood(id: String) {
        _uiState.update { it.copy(neighborhoodId = id) }
    }

    private fun toggleInterest(interest: Interest) {
        _uiState.update { current ->
            val next = current.selectedInterests.toMutableSet().apply {
                if (!add(interest)) remove(interest)
            }
            current.copy(selectedInterests = next)
        }
    }

    private fun save() {
        val current = _uiState.value
        if (current.isSaving) return
        val trimmed = current.fullName.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(nameError = "Name must be at least 2 characters") }
            return
        }
        if (current.selectedInterests.isEmpty()) {
            _uiState.update { it.copy(saveError = "Pick at least one interest") }
            return
        }
        if (current.neighborhoodId.isBlank()) {
            _uiState.update { it.copy(saveError = "Pick a neighborhood") }
            return
        }
        if (!current.hasChanges) {
            _uiState.update { it.copy(saved = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null, nameError = null) }
            profileEditRepository.updateProfile(
                fullName = trimmed,
                avatarUri = current.newAvatarUri,
                neighborhoodId = current.neighborhoodId,
                interests = current.selectedInterests.toList()
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, saved = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveError = error.message ?: "Could not save profile"
                        )
                    }
                }
            )
        }
    }
}
