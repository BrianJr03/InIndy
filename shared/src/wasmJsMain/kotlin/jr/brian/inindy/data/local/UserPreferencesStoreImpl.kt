package jr.brian.inindy.data.local

import jr.brian.inindy.domain.model.Interest
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesStoreImpl : UserPreferencesStore {

    private val _preferences = MutableStateFlow(loadFromStorage())
    override val preferences: Flow<UserPreferences> = _preferences.asStateFlow()

    override suspend fun saveUserId(id: String) {
        localStorage.setItem(UserPreferencesKeys.USER_ID, id)
        refresh()
    }

    override suspend fun saveProfile(fullName: String, avatarUrl: String?) {
        localStorage.setItem(UserPreferencesKeys.FULL_NAME, fullName)
        if (avatarUrl != null) {
            localStorage.setItem(UserPreferencesKeys.AVATAR_URL, avatarUrl)
        } else {
            localStorage.removeItem(UserPreferencesKeys.AVATAR_URL)
        }
        refresh()
    }

    override suspend fun saveNeighborhood(id: String, name: String) {
        localStorage.setItem(UserPreferencesKeys.NEIGHBORHOOD_ID, id)
        localStorage.setItem(UserPreferencesKeys.NEIGHBORHOOD_NAME, name)
        refresh()
    }

    override suspend fun saveInterests(interests: List<Interest>) {
        localStorage.setItem(
            UserPreferencesKeys.INTERESTS,
            interests.joinToString(",") { it.name }
        )
        refresh()
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        localStorage.setItem(UserPreferencesKeys.ONBOARDING_COMPLETE, complete.toString())
        refresh()
    }

    override suspend fun saveLastSelectedGroup(groupId: String, groupName: String) {
        localStorage.setItem(UserPreferencesKeys.LAST_SELECTED_GROUP_ID, groupId)
        localStorage.setItem(UserPreferencesKeys.LAST_SELECTED_GROUP_NAME, groupName)
        refresh()
    }

    override suspend fun clearLastSelectedGroup() {
        localStorage.removeItem(UserPreferencesKeys.LAST_SELECTED_GROUP_ID)
        localStorage.removeItem(UserPreferencesKeys.LAST_SELECTED_GROUP_NAME)
        refresh()
    }

    override suspend fun setLocationWarningSeen() {
        localStorage.setItem(UserPreferencesKeys.LOCATION_WARNING_SEEN, true.toString())
        refresh()
    }

    override suspend fun setFeedInterestOrdering(enabled: Boolean) {
        localStorage.setItem(UserPreferencesKeys.FEED_INTEREST_ORDERING, enabled.toString())
        refresh()
    }

    override suspend fun clear() {
        listOf(
            UserPreferencesKeys.USER_ID,
            UserPreferencesKeys.FULL_NAME,
            UserPreferencesKeys.AVATAR_URL,
            UserPreferencesKeys.NEIGHBORHOOD_ID,
            UserPreferencesKeys.NEIGHBORHOOD_NAME,
            UserPreferencesKeys.INTERESTS,
            UserPreferencesKeys.ONBOARDING_COMPLETE,
            UserPreferencesKeys.LAST_SELECTED_GROUP_ID,
            UserPreferencesKeys.LAST_SELECTED_GROUP_NAME,
            UserPreferencesKeys.LOCATION_WARNING_SEEN,
            UserPreferencesKeys.FEED_INTEREST_ORDERING
        ).forEach { localStorage.removeItem(it) }
        refresh()
    }

    private fun refresh() {
        _preferences.value = loadFromStorage()
    }

    private fun loadFromStorage(): UserPreferences = UserPreferences(
        userId = localStorage.getItem(UserPreferencesKeys.USER_ID),
        fullName = localStorage.getItem(UserPreferencesKeys.FULL_NAME),
        avatarUrl = localStorage.getItem(UserPreferencesKeys.AVATAR_URL),
        neighborhoodId = localStorage.getItem(UserPreferencesKeys.NEIGHBORHOOD_ID),
        neighborhoodName = localStorage.getItem(UserPreferencesKeys.NEIGHBORHOOD_NAME),
        interests = localStorage.getItem(UserPreferencesKeys.INTERESTS)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?: emptyList(),
        onboardingComplete = localStorage.getItem(UserPreferencesKeys.ONBOARDING_COMPLETE)?.toBoolean() ?: false,
        lastSelectedGroupId = localStorage.getItem(UserPreferencesKeys.LAST_SELECTED_GROUP_ID),
        lastSelectedGroupName = localStorage.getItem(UserPreferencesKeys.LAST_SELECTED_GROUP_NAME),
        locationWarningSeen = localStorage.getItem(UserPreferencesKeys.LOCATION_WARNING_SEEN)?.toBoolean() ?: false,
        feedInterestOrderingEnabled = localStorage.getItem(UserPreferencesKeys.FEED_INTEREST_ORDERING)?.toBoolean() ?: false
    )
}
