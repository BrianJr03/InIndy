package jr.brian.inindy.presentation.profileedit

import jr.brian.inindy.domain.model.Interest

sealed class ProfileEditIntent {
    data object Load : ProfileEditIntent()
    data object Dismiss : ProfileEditIntent()
    data class AvatarSelected(val uri: String) : ProfileEditIntent()
    data object RemoveAvatar : ProfileEditIntent()
    data class NameChanged(val text: String) : ProfileEditIntent()
    data class NeighborhoodSelected(val neighborhoodId: String) : ProfileEditIntent()
    data class ToggleInterest(val interest: Interest) : ProfileEditIntent()
    data object Save : ProfileEditIntent()
    data object ClearError : ProfileEditIntent()
}
