package jr.brian.inindy.presentation.createpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.data.location.AddressSearchDataSource
import jr.brian.inindy.data.location.LocationProvider
import jr.brian.inindy.domain.model.AddressResult
import jr.brian.inindy.domain.model.CreatePostRequest
import jr.brian.inindy.domain.model.PostAudience
import jr.brian.inindy.domain.model.PostTag
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.presentation.createpost.CreatePostUiState.Companion.MAX_IMAGES
import jr.brian.inindy.presentation.createpost.CreatePostUiState.Companion.MAX_TAGS
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val groupRepository: GroupRepository,
    private val addressSearch: AddressSearchDataSource,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private var addressSearchJob: Job? = null

    init {
        groupRepository.observeUserGroups()
            .onEach { groups ->
                _uiState.value = _uiState.value.copy(userGroups = groups)
            }
            .launchIn(viewModelScope)
    }

    fun addImage(uri: String) {
        val current = _uiState.value
        if (current.images.size >= MAX_IMAGES) return
        if (uri in current.images) return
        _uiState.value = current.copy(
            images = current.images + uri,
            imagesError = null
        )
    }

    fun removeImage(uri: String) {
        _uiState.value = _uiState.value.copy(
            images = _uiState.value.images.filterNot { it == uri }
        )
    }

    fun setTitle(text: String) {
        _uiState.value = _uiState.value.copy(
            title = text.take(80),
            titleError = null
        )
    }

    fun setDescription(text: String) {
        _uiState.value = _uiState.value.copy(
            description = text.take(CreatePostUiState.DESCRIPTION_MAX_LENGTH),
            descriptionError = null
        )
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(locationLoading = true)
            val result = locationProvider.getCurrentLocation()
            if (result != null) {
                _uiState.value = _uiState.value.copy(
                    address = result.address,
                    latitude = result.latitude,
                    longitude = result.longitude,
                    addressSuggestions = emptyList(),
                    locationLoading = false,
                    addressError = null
                )
            } else {
                _uiState.value = _uiState.value.copy(locationLoading = false)
            }
        }
    }

    fun onAddressQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(address = query, addressError = null)
        addressSearchJob?.cancel()
        addressSearchJob = viewModelScope.launch {
            if (query.length < 2) {
                _uiState.value = _uiState.value.copy(addressSuggestions = emptyList())
                return@launch
            }
            delay(180L)
            _uiState.value = _uiState.value.copy(isSearchingAddress = true)
            val result = addressSearch.search(query).getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                addressSuggestions = result,
                isSearchingAddress = false
            )
        }
    }

    fun selectAddress(result: AddressResult) {
        _uiState.value = _uiState.value.copy(
            address = result.address,
            latitude = result.latitude,
            longitude = result.longitude,
            addressSuggestions = emptyList(),
            addressError = null
        )
    }

    fun setStartsAt(epochMs: Long) {
        _uiState.value = _uiState.value.copy(startsAt = epochMs, startsAtError = null)
    }

    fun setEndsAt(epochMs: Long?) {
        _uiState.value = _uiState.value.copy(endsAt = epochMs)
    }

    fun selectNeighborhoodAudience() {
        _uiState.value = _uiState.value.copy(audience = PostAudience.Neighborhood)
    }

    fun selectGroupAudience(groupId: String) {
        _uiState.value = _uiState.value.copy(audience = PostAudience.GroupAudience(groupId))
    }

    fun toggleTag(tag: PostTag) {
        val current = _uiState.value.tags
        val updated = when {
            tag in current -> current - tag
            current.size >= MAX_TAGS -> current
            else -> current + tag
        }
        _uiState.value = _uiState.value.copy(tags = updated)
    }

    fun setMaxAttendees(count: Int?) {
        _uiState.value = _uiState.value.copy(
            maxAttendees = count,
            noLimit = count == null
        )
    }

    fun setNoLimit(noLimit: Boolean) {
        _uiState.value = _uiState.value.copy(
            noLimit = noLimit,
            maxAttendees = if (noLimit) null else (_uiState.value.maxAttendees ?: 4)
        )
    }

    fun quickCreateGroup(name: String, description: String?) {
        viewModelScope.launch {
            groupRepository.createGroup(name, description).onSuccess { group ->
                _uiState.value = _uiState.value.copy(
                    audience = PostAudience.GroupAudience(group.id)
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(submitError = null)
    }

    fun resetAfterSubmit() {
        _uiState.value = CreatePostUiState(userGroups = _uiState.value.userGroups)
    }

    fun submit() {
        val state = _uiState.value
        val validated = state.withValidationErrors()
        if (validated.hasFieldErrors) {
            _uiState.value = validated
            return
        }
        val startsAt = state.startsAt ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(
                isSubmitting = true,
                submitError = null,
                imagesError = null,
                titleError = null,
                descriptionError = null,
                addressError = null,
                startsAtError = null
            )
            val request = CreatePostRequest(
                title = state.title.trim(),
                description = state.description.trim(),
                latitude = state.latitude ?: INDIANAPOLIS_LAT,
                longitude = state.longitude ?: INDIANAPOLIS_LNG,
                address = state.address.trim(),
                startsAt = startsAt,
                endsAt = state.endsAt,
                tags = state.tags.toList(),
                imageUris = state.images,
                audience = state.audience,
                maxAttendees = state.maxAttendees
            )
            postRepository.createPost(request)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitted = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitError = error.message ?: "Couldn't post — try again"
                    )
                }
        }
    }

    private fun CreatePostUiState.withValidationErrors(): CreatePostUiState {
        val now = currentTimeMillis()
        val starts = startsAt
        val startsError = when {
            starts == null -> "Please set a start date and time"
            starts <= now -> "Pick a future start time"
            else -> null
        }
        return copy(
            imagesError = if (images.isEmpty()) "Add at least one photo" else null,
            titleError = if (title.length < 3) "Title must be at least 3 characters" else null,
            descriptionError = if (description.length < 10) "Description must be at least 10 characters" else null,
            addressError = if (address.isBlank()) "Please set a location" else null,
            startsAtError = startsError
        )
    }

    private val CreatePostUiState.hasFieldErrors: Boolean
        get() = imagesError != null
            || titleError != null
            || descriptionError != null
            || addressError != null
            || startsAtError != null

    private companion object {
        const val INDIANAPOLIS_LAT = 39.7684
        const val INDIANAPOLIS_LNG = -86.1581
    }
}
