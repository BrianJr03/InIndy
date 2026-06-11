package jr.brian.inindy.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.ExploreFilter
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.toBrandMarkText
import jr.brian.inindy.domain.repository.GroupRepository
import jr.brian.inindy.domain.repository.PostRepository
import jr.brian.inindy.domain.usecase.RsvpPostUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val postRepository: PostRepository,
    private val rsvpPost: RsvpPostUseCase,
    private val groupRepository: GroupRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private var searchJob: Job? = null
    private var feedJob: Job? = null
    private var neighborhoodId: String = DEFAULT_NEIGHBORHOOD_ID

    init {
        observeSearchQuery()
        loadUserGroups()
        bootstrap()
    }

    fun onIntent(intent: ExploreIntent) {
        when (intent) {
            ExploreIntent.Refresh -> {
                println("[InIndy] ExploreViewModel — Refresh intent received")
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
            is ExploreIntent.SelectFilterGroup -> applyFilter(
                ExploreFilter.Group(intent.group.id, intent.group.name)
            )
        }
    }

    fun loadPosts() = loadFeed()

    fun refresh() = loadFeed()

    fun rsvp(postId: String) {
        if (rsvpPost.isRsvpd(postId)) return
        viewModelScope.launch {
            rsvpPost(postId).onSuccess { mutateFeedRsvp(postId, delta = +1) }
        }
    }

    fun unRsvp(postId: String) {
        if (!rsvpPost.isRsvpd(postId)) return
        viewModelScope.launch {
            rsvpPost.unRsvp(postId).onSuccess { mutateFeedRsvp(postId, delta = -1) }
        }
    }

    fun isRsvpd(postId: String): Boolean = rsvpPost.isRsvpd(postId)

    private fun mutateFeedRsvp(postId: String, delta: Int) {
        _uiState.update { current ->
            val feed = current.feed as? ExploreUiState.FeedState.Success ?: return@update current
            val nextPosts = feed.posts.map { post ->
                if (post.id == postId) {
                    post.copy(rsvpCount = (post.rsvpCount + delta).coerceAtLeast(0))
                } else post
            }
            current.copy(feed = ExploreUiState.FeedState.Success(nextPosts))
        }
    }

    fun findPost(postId: String): Post? {
        val feed = _uiState.value.feed
        return (feed as? ExploreUiState.FeedState.Success)?.posts?.firstOrNull { it.id == postId }
    }

    private fun bootstrap(loadFeed: Boolean = true) {
        viewModelScope.launch {
            val prefs = currentUserProvider.get()
            neighborhoodId = prefs.neighborhoodId ?: DEFAULT_NEIGHBORHOOD_ID
            val neighborhoodName = prefs.neighborhoodName ?: DEFAULT_NEIGHBORHOOD_NAME
            println("[InIndy] ExploreViewModel bootstrap — neighborhoodId: $neighborhoodId, neighborhoodName: $neighborhoodName, loadFeed: $loadFeed")
            _uiState.update {
                it.copy(
                    neighborhoodName = neighborhoodName,
                    brandMarkText = it.activeFilter.toBrandMarkText(neighborhoodName)
                )
            }
            if (loadFeed) {
                loadFeed()
            }
        }
    }

    private fun applyFilter(filter: ExploreFilter) {
        println("[InIndy] ExploreViewModel applyFilter — filter: $filter")
        _uiState.update { current ->
            current.copy(
                activeFilter = filter,
                brandMarkText = filter.toBrandMarkText(current.neighborhoodName),
                isFilterDropdownVisible = false,
                isGroupSearchSheetVisible = false,
                groupSearchQuery = "",
                searchedGroups = emptyList(),
                isSearchingGroups = false,
                feed = ExploreUiState.FeedState.Loading
            )
        }
        loadFeed()
    }

    private fun loadFeed() {
        feedJob?.cancel()
        val filter = _uiState.value.activeFilter
        println("[InIndy] ExploreViewModel loadFeed — filter: $filter, neighborhoodId: $neighborhoodId")
        feedJob = viewModelScope.launch {
            val result = when (filter) {
                is ExploreFilter.All -> postRepository.getNeighborhoodFeed(neighborhoodId)
                is ExploreFilter.Neighborhood -> postRepository.getNeighborhoodOnlyFeed(neighborhoodId)
                is ExploreFilter.Group -> postRepository.getGroupFeed(filter.groupId)
            }
            result
                .onSuccess { posts ->
                    println("[InIndy] ExploreViewModel loadFeed SUCCESS — ${posts.size} posts for filter: $filter")
                }
                .onFailure { e ->
                    println("[InIndy] ExploreViewModel loadFeed FAILED — filter: $filter, error: ${e::class.simpleName}: ${e.message}")
                    e.printStackTrace()
                }
            _uiState.update { current ->
                if (current.activeFilter != filter) return@update current
                val nextFeed = result.fold(
                    onSuccess = { ExploreUiState.FeedState.Success(it) },
                    onFailure = {
                        ExploreUiState.FeedState.Error(
                            it.message ?: "Something went wrong"
                        )
                    }
                )
                current.copy(feed = nextFeed)
            }
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
        const val DEFAULT_NEIGHBORHOOD_NAME = "Broad Ripple"
        const val DEFAULT_NEIGHBORHOOD_ID = "broad_ripple"
    }
}