package jr.brian.inindy.presentation.profileedit

import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Neighborhood

data class ProfileEditUiState(
    val currentAvatarUrl: String? = null,
    val newAvatarUri: String? = null,
    val fullName: String = "",
    val neighborhoodId: String = "",
    val selectedInterests: Set<Interest> = emptySet(),
    val neighborhoods: List<Neighborhood> = emptyList(),

    val initialAvatarUrl: String? = null,
    val initialFullName: String = "",
    val initialNeighborhoodId: String = "",
    val initialInterests: Set<Interest> = emptySet(),

    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val nameError: String? = null,
    val saved: Boolean = false
)

val ProfileEditUiState.hasChanges: Boolean
    get() = newAvatarUri != null
        || fullName.trim() != initialFullName
        || neighborhoodId != initialNeighborhoodId
        || selectedInterests != initialInterests

val ProfileEditUiState.isValid: Boolean
    get() = fullName.trim().length >= 2
        && neighborhoodId.isNotBlank()
        && selectedInterests.isNotEmpty()
