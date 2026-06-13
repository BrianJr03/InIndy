package jr.brian.inindy.data.local

import jr.brian.inindy.domain.model.Interest
import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val userId: String? = null,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val neighborhoodId: String? = null,
    val neighborhoodName: String? = null,
    val interests: List<String> = emptyList(),
    val onboardingComplete: Boolean = false,
    val lastSelectedGroupId: String? = null,
    val lastSelectedGroupName: String? = null
)

interface UserPreferencesStore {
    val preferences: Flow<UserPreferences>
    suspend fun saveUserId(id: String)
    suspend fun saveProfile(fullName: String, avatarUrl: String?)
    suspend fun saveNeighborhood(id: String, name: String)
    suspend fun saveInterests(interests: List<Interest>)
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun saveLastSelectedGroup(groupId: String, groupName: String)
    suspend fun clearLastSelectedGroup()
    suspend fun clear()
}

internal object UserPreferencesKeys {
    const val USER_ID = "user_id"
    const val FULL_NAME = "full_name"
    const val AVATAR_URL = "avatar_url"
    const val NEIGHBORHOOD_ID = "neighborhood_id"
    const val NEIGHBORHOOD_NAME = "neighborhood_name"
    const val INTERESTS = "interests"
    const val ONBOARDING_COMPLETE = "onboarding_complete"
    const val LAST_SELECTED_GROUP_ID = "last_selected_group_id"
    const val LAST_SELECTED_GROUP_NAME = "last_selected_group_name"
}
