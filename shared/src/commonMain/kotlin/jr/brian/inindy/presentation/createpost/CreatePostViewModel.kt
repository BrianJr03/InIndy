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
    private val userPreferencesStore: UserPreferencesStore,
    private val postId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CreatePostUiState(
            isEditMode = postId != null,
            isEditPrefillLoading = postId != null
        )
    )
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private var addressSearchJob: Job? = null

    init {
        // Realtime-backed shared flow — good for live updates while the screen
        // is open, but can start empty/stale when Realtime for group_members
        // isn't enabled. Only accept non-empty emissions so a stale-empty
        // replay can't clobber the one-shot seed below.
        groupRepository.observeUserGroups()
            .onEach { groups ->
                if (groups.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(userGroups = groups)
                }
            }
            .launchIn(viewModelScope)
        // Reliable one-shot fetch — mirrors ExploreViewModel.loadUserGroups().
        // Guarantees the audience picker is populated on open regardless of
        // whether Realtime is enabled server-side.
        viewModelScope.launch {
            groupRepository.getUserGroups()
                .onSuccess { groups ->
                    if (groups.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(userGroups = groups)
                    }
                }
                .onFailure { e ->
                    println("[InIndy] CreatePostViewModel getUserGroups FAILED — ${e.message}")
                }
        }
        userPreferencesStore.preferences
            .onEach { prefs ->
                _uiState.value = _uiState.value.copy(
                    locationWarningSeen = prefs.locationWarningSeen
                )
            }
            .launchIn(viewModelScope)
        if (postId != null) {
            loadForEdit(postId)
        }
    }

    private fun loadForEdit(postId: String) {
        viewModelScope.launch {
            postRepository.getPostById(postId)
                .onSuccess { post ->
                    val audience = if (post.groupId != null) {
                        PostAudience.GroupAudience(post.groupId)
                    } else {
                        PostAudience.Neighborhood
                    }
                    val tags = post.tags.toSet()
                    val noLimit = post.maxAttendees == null
                    // Baseline captures exactly what we're about to write into
                    // state so isDirty reads false until the user actually
                    // changes something.
                    val baseline = EditBaseline(
                        title = post.title,
                        description = post.description,
                        address = post.address,
                        latitude = post.latitude,
                        longitude = post.longitude,
                        startsAt = post.startsAt,
                        endsAt = post.endsAt,
                        tags = tags,
                        maxAttendees = post.maxAttendees,
                        noLimit = noLimit,
                        images = post.images,
                        audience = audience
                    )
                    _uiState.value = _uiState.value.copy(
                        title = post.title,
                        description = post.description,
                        address = post.address,
                        latitude = post.latitude,
                        longitude = post.longitude,
                        startsAt = post.startsAt,
                        endsAt = post.endsAt,
                        tags = tags,
                        // Existing images are CDN URLs — flagged in submit() to skip re-upload.
                        images = post.images,
                        audience = audience,
                        maxAttendees = post.maxAttendees,
                        noLimit = noLimit,
                        isEditPrefillLoading = false,
                        editBaseline = baseline
                    )
                }
                .onFailure { e ->
                    println("[InIndy] CreatePostViewModel loadForEdit FAILED — postId: $postId, error: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isEditPrefillLoading = false,
                        submitError = "Couldn't load post to edit — try again"
                    )
                }
        }
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
        _uiState.value = _uiState.value.copy(
            audience = PostAudience.Neighborhood,
            pendingGroupAudience = false
        )
    }

    fun selectGroupAudience(groupId: String) {
        _uiState.value = _uiState.value.copy(
            audience = PostAudience.GroupAudience(groupId),
            pendingGroupAudience = false
        )
    }

    // Called when the user taps the "Group" radio while they aren't in any
    // group. Keeps audience as-is (still Neighborhood) but flips the UI into
    // group-picker mode so the picker + empty-state render — no navigation.
    fun enterGroupAudienceMode() {
        _uiState.value = _uiState.value.copy(pendingGroupAudience = true)
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

            // Existing images (edit mode) are CDN http URLs — skip re-upload
            // and keep them as-is. Local device URIs get uploaded fresh. Order
            // is preserved so the sort_order rows match what the user sees.
            val uploadResults = coroutineScope {
                state.images.map { uri ->
                    async {
                        if (uri.startsWith("http")) Result.success(uri)
                        else mediaRepository.uploadPostImage(uri)
                    }
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
            val result = if (postId != null) {
                postRepository.updatePost(postId, request)
            } else {
                postRepository.createPost(request)
            }
            result
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
