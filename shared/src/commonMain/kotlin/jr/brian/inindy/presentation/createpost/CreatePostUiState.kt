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
    // Radio shows "Group" selected but no specific group has been picked yet.
    // Set when the user taps the Group radio while they aren't in any group —
    // avoids the old behaviour of silently bouncing to create-group. Cleared
    // when either audience is set concretely (Neighborhood or a chosen group).
    val pendingGroupAudience: Boolean = false,
    val userGroups: List<Group> = emptyList(),
    val tags: Set<Interest> = emptySet(),
    val maxAttendees: Int? = null,
    val noLimit: Boolean = true,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submitted: Boolean = false,
    val isEditMode: Boolean = false,
    val isEditPrefillLoading: Boolean = false,
    // Snapshot of user-editable fields taken right after the edit-mode prefill
    // completes. Non-null iff we're in edit mode AND prefill finished. Used only
    // to compute isDirty as "form differs from what we loaded" — create mode
    // stays null and falls back to the non-empty check.
    val editBaseline: EditBaseline? = null,
    val imagesError: String? = null,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val addressError: String? = null,
    val startsAtError: String? = null,
    val endsAtError: String? = null,
    val locationWarningSeen: Boolean = false
) {
    val isDirty: Boolean
        get() = editBaseline?.let { baseline ->
            // Edit mode after prefill: any user-editable field diverges from what
            // we loaded. Order matters for images (sort_order maps to index) so
            // List != is the right comparison.
            title != baseline.title
                || description != baseline.description
                || address != baseline.address
                || latitude != baseline.latitude
                || longitude != baseline.longitude
                || startsAt != baseline.startsAt
                || endsAt != baseline.endsAt
                || tags != baseline.tags
                || maxAttendees != baseline.maxAttendees
                || noLimit != baseline.noLimit
                || images != baseline.images
                || audience != baseline.audience
        } ?: (
            // Create mode (or edit-mode prefill hasn't landed yet): "user has
            // put anything in."
            images.isNotEmpty()
                || title.isNotEmpty()
                || description.isNotEmpty()
                || address.isNotEmpty()
                || startsAt != null
                || endsAt != null
                || tags.isNotEmpty()
                || maxAttendees != null
        )

    val charactersRemaining: Int
        get() = DESCRIPTION_MAX_LENGTH - description.length

    companion object {
        const val DESCRIPTION_MAX_LENGTH = 280
        const val MAX_IMAGES = 3
        const val MAX_TAGS = 3
    }
}

data class EditBaseline(
    val title: String,
    val description: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val startsAt: Long?,
    val endsAt: Long?,
    val tags: Set<Interest>,
    val maxAttendees: Int?,
    val noLimit: Boolean,
    val images: List<String>,
    val audience: PostAudience
)

val CreatePostUiState.isSubmitEnabled: Boolean
    get() = images.isNotEmpty()
        && title.length >= 3
        && description.length >= 10
        && address.isNotBlank()
        && startsAt != null
        && !isSubmitting
        // Can't submit while "Group" is selected but no specific group has
        // been chosen — the audience is still Neighborhood underneath.
        && !pendingGroupAudience
