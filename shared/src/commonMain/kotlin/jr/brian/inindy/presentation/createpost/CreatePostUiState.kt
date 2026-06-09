package jr.brian.inindy.presentation.createpost

import jr.brian.inindy.domain.model.AddressResult
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.PostAudience
import jr.brian.inindy.domain.model.Interest

data class CreatePostUiState(
    val images: List<String> = emptyList(),
    val title: String = "",
    val description: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val addressSuggestions: List<AddressResult> = emptyList(),
    val isSearchingAddress: Boolean = false,
    val locationLoading: Boolean = false,
    val startsAt: Long? = null,
    val endsAt: Long? = null,
    val audience: PostAudience = PostAudience.Neighborhood,
    val userGroups: List<Group> = emptyList(),
    val tags: Set<Interest> = emptySet(),
    val maxAttendees: Int? = null,
    val noLimit: Boolean = true,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submitted: Boolean = false,
    val imagesError: String? = null,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val addressError: String? = null,
    val startsAtError: String? = null
) {
    val isDirty: Boolean
        get() = images.isNotEmpty()
            || title.isNotEmpty()
            || description.isNotEmpty()
            || address.isNotEmpty()
            || startsAt != null
            || endsAt != null
            || tags.isNotEmpty()
            || maxAttendees != null

    val charactersRemaining: Int
        get() = DESCRIPTION_MAX_LENGTH - description.length

    companion object {
        const val DESCRIPTION_MAX_LENGTH = 280
        const val MAX_IMAGES = 3
        const val MAX_TAGS = 3
    }
}

val CreatePostUiState.isSubmitEnabled: Boolean
    get() = images.isNotEmpty()
        && title.length >= 3
        && description.length >= 10
        && address.isNotBlank()
        && startsAt != null
        && !isSubmitting
