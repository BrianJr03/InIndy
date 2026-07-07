package jr.brian.inindy.presentation.createpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.data.location.AddressSearchDataSource
import jr.brian.inindy.data.location.LocationProvider
import jr.brian.inindy.domain.model.AddressResult
import jr.brian.inindy.domain.model.CreateGroupRequest
import jr.brian.inindy.domain.model.CreatePostRequest
import jr.brian.inindy.domain.model.PostAudience
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.MediaRepository
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.presentation.createpost.CreatePostUiState.Companion.MAX_IMAGES
import jr.brian.inindy.presentation.createpost.CreatePostUiState.Companion.MAX_TAGS
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val locationProvider: LocationProvider,
    private val mediaRepository: MediaRepository,
    private val userPreferencesStore: UserPreferencesStore
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
        userPreferencesStore.preferences
            .onEach { prefs ->
                _uiState.value = _uiState.value.copy(
                    locationWarningSeen = prefs.locationWarningSeen
                )
            }
            .launchIn(viewModelScope)
    }

    fun acknowledgeLocationWarning() {
        viewModelScope.launch {
            userPreferencesStore.setLocationWarningSeen()
        }
    }

    fun addImage(uri: String) {
        println("[InIndy] addImage called with: ${uri.take(80)}")
        val current = _uiState.value
        if (current.images.size >= MAX_IMAGES) return
        if (uri in current.images) return
        // Reject CDN URLs — only local device URIs allowed
        if (uri.startsWith("http")) {
            println("[InIndy] addImage rejected non-local URI: $uri")
            return
        }
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
        _uiState.value = _uiState.value.copy(endsAt = epochMs, endsAtError = null)
    }

    fun selectNeighborhoodAudience() {
        _uiState.value = _uiState.value.copy(audience = PostAudience.Neighborhood)
    }

    fun selectGroupAudience(groupId: String) {
        _uiState.value = _uiState.value.copy(audience = PostAudience.GroupAudience(groupId))
    }

    fun toggleTag(tag: Interest) {
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
            groupRepository.createGroup(
                CreateGroupRequest(
                    name = name,
                    description = description,
                    coverImageUri = null
                )
            ).onSuccess { group ->
                _uiState.value = _uiState.value.copy(
                    audience = PostAudience.GroupAudience(group.id)
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(submitError = null)
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
                startsAtError = null,
                endsAtError = null
            )

            val uploadResults = coroutineScope {
                state.images.map { uri ->
                    async { mediaRepository.uploadPostImage(uri) }
                }.awaitAll()
            }
            val firstFailure = uploadResults.firstOrNull { it.isFailure }
            if (firstFailure != null) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submitError = firstFailure.exceptionOrNull().toUploadError(),
                    images = emptyList()
                )
                return@launch
            }
            val cdnUrls = uploadResults.map { it.getOrThrow() }

            val request = CreatePostRequest(
                title = state.title.trim(),
                description = state.description.trim(),
                latitude = state.latitude ?: INDIANAPOLIS_LAT,
                longitude = state.longitude ?: INDIANAPOLIS_LNG,
                address = state.address.trim(),
                startsAt = startsAt,
                endsAt = state.endsAt,
                tags = state.tags.toList(),
                imageUris = cdnUrls,
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
        val starts = startsAt
        val startsError = when {
            starts == null -> "Please set a start date and time"
            starts <= currentTimeMillis() -> "Start time must be in the future"
            else -> null
        }
        val endsError = when {
            endsAt != null && endsAt <= (starts ?: 0L) ->
                "End time must be after start time"
            else -> null
        }
        return copy(
            imagesError = if (images.isEmpty()) "Add at least one photo" else null,
            titleError = if (title.length < 3) "Title must be at least 3 characters" else null,
            descriptionError = if (description.length < 10) "Description must be at least 10 characters" else null,
            addressError = if (address.isBlank()) "Please set a location" else null,
            startsAtError = startsError,
            endsAtError = endsError
        )
    }

    private val CreatePostUiState.hasFieldErrors: Boolean
        get() = imagesError != null
            || titleError != null
            || descriptionError != null
            || addressError != null
            || startsAtError != null
            || endsAtError != null

    private companion object {
        const val INDIANAPOLIS_LAT = 39.7684
        const val INDIANAPOLIS_LNG = -86.1581
    }
}

internal fun Throwable?.toUploadError(): String {
    val message = this?.message.orEmpty()
    return when {
        message.contains("timeout", ignoreCase = true) ->
            "Upload timed out — check your connection and try again"
        message.contains("403") ->
            "Upload permission denied — try signing out and back in"
        message.contains("413") ->
            "Image is too large — try a smaller photo"
        message.contains("404") ->
            "Upload service unavailable — please contact support"
        else -> "Image upload failed — check your connection and try again"
    }
}
