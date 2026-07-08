package jr.brian.inindy.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.data.local.UserPreferencesStore
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.ExploreFilter
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.GroupRole
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.model.toBrandMarkText
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.domain.usecase.RsvpPostUseCase
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ExploreViewModel(
    private val postRepository: PostRepository,
    private val rsvpPost: RsvpPostUseCase,
    private val groupRepository: GroupRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val userPreferencesStore: UserPreferencesStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private var searchJob: Job? = null
    private var feedJob: Job? = null
    // Kept on viewModelScope (not feedJob) so a rapid second pull can't leave the spinner stuck on
    // when the feedJob for the prior refresh is cancelled by loadFeed().
    private var refreshClearJob: Job? = null
    private var refreshStartMs: Long? = null
    private var neighborhoodId: String = DEFAULT_NEIGHBORHOOD_ID
    private var currentUserId: String? = null
    // Latest known viewer interests, kept in sync via observeFeedOrderingPreferences().
    // Read by the feed emission handler to rank neighborhood/explore posts;
    // empty until the first UserPreferences emission arrives (early emissions
    // sort as recency-only, which is safe and matches the no-interests case).
    private var currentInterests: Set<Interest> = emptySet()
    // Persisted toggle from Settings — defaults to false ("off") until the
    // first UserPreferences emission arrives, matching the store default.
    private var feedInterestOrderingEnabled: Boolean = false

    init {
        observeSearchQuery()
        observeFeedOrderingPreferences()
        loadUserGroups()
        bootstrap()
    }

    fun onIntent(intent: ExploreIntent) {
        when (intent) {
            ExploreIntent.Refresh -> {
                refreshClearJob?.cancel()
                refreshStartMs = currentTimeMillis()
                _uiState.update { it.copy(isRefreshing = true) }
                loadFeed()
            }
            ExploreIntent.ToggleFilterDropdown -> {
                _uiState.update {
                    it.copy(isFilterDropdownVisible = !it.isFilterDropdownVisible)
                }
                bootstrap(loadFeed = false)
            }

            ExploreIntent.DismissFilterDropdown -> _uiState.update {
                it.copy(isFilterDropdownVisible = false)
            }

            ExploreIntent.SelectFilterAll -> applyFilter(ExploreFilter.All)
            ExploreIntent.SelectFilterNeighborhood -> applyFilter(ExploreFilter.Neighborhood)
            ExploreIntent.OpenGroupSearch -> openGroupSearch()
            ExploreIntent.DismissGroupSearch -> _uiState.update {
                it.copy(
                    isGroupSearchSheetVisible = false,
                    groupSearchQuery = "",
                    searchedGroups = emptyList(),
                    isSearchingGroups = false
                )
            }

            is ExploreIntent.GroupSearchQueryChanged -> onSearchQueryChanged(intent.query)
            is ExploreIntent.SelectFilterGroup -> {
                _uiState.update { it.copy(lastSelectedGroup = intent.group) }
                applyFilter(ExploreFilter.Group(intent.group.id, intent.group.name))
                viewModelScope.launch {
                    userPreferencesStore.saveLastSelectedGroup(intent.group.id, intent.group.name)
                }
            }
        }
    }

    fun loadPosts() = loadFeed()

    fun isRsvpd(postId: String): Boolean = rsvpPost.isRsvpd(postId)

    fun isOwnPost(post: Post): Boolean = post.userId == currentUserId

    fun rsvp(postId: String) {
        if (rsvpPost.isRsvpd(postId)) return
        viewModelScope.launch {
            rsvpPost(postId).onSuccess {
                mutateFeedRsvp(postId, delta = 1, me = currentUserAsAttendee())
            }.onFailure { e ->
                println("[InIndy] ExploreViewModel rsvp FAILED — postId: $postId, error: ${e.message}")
            }
        }
    }

    fun unRsvp(postId: String) {
        if (!rsvpPost.isRsvpd(postId)) return
        viewModelScope.launch {
            rsvpPost.unRsvp(postId).onSuccess {
                mutateFeedRsvp(postId, delta = -1, me = currentUserAsAttendee())
            }.onFailure { e ->
                println("[InIndy] ExploreViewModel unRsvp FAILED — postId: $postId, error: ${e.message}")
            }
        }
    }

    private fun mutateFeedRsvp(postId: String, delta: Int, me: User?) {
        _uiState.update { current ->
            val feed = current.feed as? ExploreUiState.FeedState.Success ?: return@update current
            val updated = feed.posts.map { post ->
                if (post.id == postId) {
                    post.copy(
                        rsvpCount = (post.rsvpCount + delta).coerceAtLeast(0),
                        previewAttendees = updatedAttendees(post.previewAttendees, me, delta)
                    )
                } else post
            }
            current.copy(feed = ExploreUiState.FeedState.Success(updated))
        }
    }

    private fun updatedAttendees(current: List<User>, me: User?, delta: Int): List<User> {
        if (me == null) return current
        return when {
            delta > 0 && current.none { it.id == me.id } -> current + me
            delta < 0 -> current.filterNot { it.id == me.id }
            else -> current
        }
    }

    private suspend fun currentUserAsAttendee(): User? {
        val prefs = currentUserProvider.get()
        val id = prefs.userId ?: return null
        return User(id = id, fullName = prefs.fullName, avatarUrl = prefs.avatarUrl)
    }

    private fun bootstrap(loadFeed: Boolean = true) {
        viewModelScope.launch {
            val prefs = currentUserProvider.get()
            neighborhoodId = prefs.neighborhoodId ?: DEFAULT_NEIGHBORHOOD_ID
            currentUserId = prefs.userId
            val neighborhoodName = prefs.neighborhoodName ?: DEFAULT_NEIGHBORHOOD_NAME
            println("[InIndy] ExploreViewModel bootstrap — neighborhoodId: $neighborhoodId, neighborhoodName: $neighborhoodName, loadFeed: $loadFeed")
            _uiState.update {
                it.copy(
                    neighborhoodName = neighborhoodName,
                    brandMarkText = it.activeFilter.toBrandMarkText(neighborhoodName)
                )
            }
            val lastGroupId = prefs.lastSelectedGroupId
            val lastGroupName = prefs.lastSelectedGroupName
            if (!lastGroupId.isNullOrBlank() && !lastGroupName.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        lastSelectedGroup = Group(
                            id = lastGroupId,
                            name = lastGroupName,
                            description = null,
                            coverUrl = null,
                            createdBy = "",
                            isOpen = true,
                            memberCount = 0,
                            role = GroupRole.MEMBER,
                            createdAt = 0L
                        )
                    )
                }
            }
            if (loadFeed) {
                loadFeed()
            }
        }
    }

    private fun applyFilter(filter: ExploreFilter) {
        println("[InIndy] ExploreViewModel applyFilter — filter: $filter")
        // A filter switch supersedes any in-flight refresh; make sure the spinner doesn't linger.
        refreshClearJob?.cancel()
        refreshStartMs = null
        _uiState.update { current ->
            current.copy(
                activeFilter = filter,
                brandMarkText = filter.toBrandMarkText(current.neighborhoodName),
                isFilterDropdownVisible = false,
                isGroupSearchSheetVisible = false,
                groupSearchQuery = "",
                searchedGroups = emptyList(),
                isSearchingGroups = false,
                feed = ExploreUiState.FeedState.Loading,
                isRefreshing = false
            )
        }
        loadFeed()
    }

    private fun loadFeed() {
        feedJob?.cancel()
        val filter = _uiState.value.activeFilter
        println("[InIndy] ExploreViewModel loadFeed — filter: $filter, neighborhoodId: $neighborhoodId")
        val feedFlow = when (filter) {
            is ExploreFilter.All,
            is ExploreFilter.Neighborhood -> postRepository.observeNeighborhoodOnlyFeed(neighborhoodId)
            is ExploreFilter.Group -> postRepository.observeGroupFeed(filter.groupId)
        }
        feedJob = viewModelScope.launch {
            feedFlow.collect { result ->
                result
                    .onSuccess { posts ->
                        println("[InIndy] ExploreViewModel feed emission — ${posts.size} posts for filter: $filter")
                    }
                    .onFailure { e ->
                        if (e is CancellationException) return@collect
                        println("[InIndy] ExploreViewModel feed emission FAILED — filter: $filter, error: ${e::class.simpleName}: ${e.message}")
                    }
                _uiState.update { current ->
                    if (current.activeFilter != filter) return@update current
                    val nextFeed = result.fold(
                        onSuccess = { posts ->
                            ExploreUiState.FeedState.Success(orderPostsFor(filter, posts))
                        },
                        onFailure = { e ->
                            if (e is CancellationException) return@update current
                            ExploreUiState.FeedState.Error(e.message ?: "Something went wrong")
                        }
                    )
                    current.copy(feed = nextFeed)
                }
                maybeScheduleRefreshClear()
            }
        }
    }

    // Stable sort: posts with more matching tags come first; ties keep incoming
    // order, which the repository has already ordered by created_at DESC. An
    // empty `interests` set makes every match count zero, so the stable sort is
    // a no-op and recency wins — no special case needed.
    private fun rankPostsByInterests(
        posts: List<Post>,
        interests: Set<Interest>
    ): List<Post> = posts.sortedByDescending { post ->
        post.tags.count { it in interests }
    }

    // Single point that resolves "what order should this feed be in right now?"
    // — the toggle gate + the "group feeds are recency-only" rule both live
    // here so loadFeed's emission handler and the reactive re-order below stay
    // in sync.
    private fun orderPostsFor(filter: ExploreFilter, posts: List<Post>): List<Post> =
        when (filter) {
            is ExploreFilter.All,
            is ExploreFilter.Neighborhood ->
                if (feedInterestOrderingEnabled) {
                    rankPostsByInterests(posts, currentInterests)
                } else {
                    posts
                }
            is ExploreFilter.Group -> posts
        }

    private fun observeFeedOrderingPreferences() {
        userPreferencesStore.preferences
            .map { prefs ->
                val interests = prefs.interests
                    .mapNotNull { name -> runCatching { Interest.valueOf(name) }.getOrNull() }
                    .toSet()
                interests to prefs.feedInterestOrderingEnabled
            }
            .distinctUntilChanged()
            .onEach { (interests, enabled) ->
                currentInterests = interests
                feedInterestOrderingEnabled = enabled
                // Live re-order: if the feed is already loaded, apply the new
                // ordering immediately so the Settings toggle (and interest
                // edits) reflect in the current view without a refresh.
                _uiState.update { current ->
                    val success = current.feed as? ExploreUiState.FeedState.Success
                        ?: return@update current
                    val ordered = orderPostsFor(current.activeFilter, success.posts)
                    if (ordered === success.posts) current
                    else current.copy(feed = ExploreUiState.FeedState.Success(ordered))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun maybeScheduleRefreshClear() {
        val start = refreshStartMs ?: return
        // One-shot per refresh: subsequent realtime emissions in the same subscription won't
        // re-arm the clear.
        refreshStartMs = null
        refreshClearJob?.cancel()
        refreshClearJob = viewModelScope.launch {
            val remaining = REFRESH_MIN_SPINNER_MS - (currentTimeMillis() - start)
            if (remaining > 0) delay(remaining)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun openGroupSearch() {
        _uiState.update {
            it.copy(
                isFilterDropdownVisible = false,
                isGroupSearchSheetVisible = true,
                groupSearchQuery = "",
                searchedGroups = emptyList(),
                isSearchingGroups = false
            )
        }
    }

    private fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                groupSearchQuery = query,
                isSearchingGroups = query.isNotBlank(),
                searchedGroups = if (query.isBlank()) emptyList() else it.searchedGroups
            )
        }
        searchQueryFlow.tryEmit(query)
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        searchQueryFlow
            .debounce(SEARCH_DEBOUNCE_MS)
            .onEach { query ->
                if (query.isBlank()) {
                    _uiState.update {
                        it.copy(
                            searchedGroups = emptyList(),
                            isSearchingGroups = false
                        )
                    }
                    return@onEach
                }
                runSearch(query)
            }
            .launchIn(viewModelScope)
    }

    private fun runSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            println("[InIndy] ExploreViewModel runSearch — query: $query")
            val result = groupRepository.searchGroups(query)
            result
                .onSuccess { groups -> println("[InIndy] ExploreViewModel runSearch SUCCESS — ${groups.size} groups for query: $query") }
                .onFailure { e -> println("[InIndy] ExploreViewModel runSearch FAILED — ${e.message}") }
            _uiState.update { current ->
                if (current.groupSearchQuery != query) return@update current
                current.copy(
                    isSearchingGroups = false,
                    searchedGroups = result.getOrDefault(emptyList())
                )
            }
        }
    }

    private fun loadUserGroups() {
        viewModelScope.launch {
            println("[InIndy] ExploreViewModel loadUserGroups — loading")
            val result = groupRepository.getUserGroups()
            result
                .onSuccess { groups -> println("[InIndy] ExploreViewModel loadUserGroups SUCCESS — ${groups.size} groups") }
                .onFailure { e -> println("[InIndy] ExploreViewModel loadUserGroups FAILED — ${e.message}") }
            _uiState.update { it.copy(userGroups = result.getOrDefault(emptyList())) }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val REFRESH_MIN_SPINNER_MS = 600L
        const val DEFAULT_NEIGHBORHOOD_NAME = "Broad Ripple"
        const val DEFAULT_NEIGHBORHOOD_ID = "broad_ripple"
    }
}