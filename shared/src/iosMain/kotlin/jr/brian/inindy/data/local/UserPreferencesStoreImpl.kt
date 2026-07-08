package jr.brian.inindy.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import jr.brian.inindy.domain.model.Interest
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

class UserPreferencesStoreImpl : UserPreferencesStore {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { datastorePath().toPath() }
    )

    private val userIdKey = stringPreferencesKey(UserPreferencesKeys.USER_ID)
    private val fullNameKey = stringPreferencesKey(UserPreferencesKeys.FULL_NAME)
    private val avatarUrlKey = stringPreferencesKey(UserPreferencesKeys.AVATAR_URL)
    private val neighborhoodIdKey = stringPreferencesKey(UserPreferencesKeys.NEIGHBORHOOD_ID)
    private val neighborhoodNameKey = stringPreferencesKey(UserPreferencesKeys.NEIGHBORHOOD_NAME)
    private val interestsKey = stringPreferencesKey(UserPreferencesKeys.INTERESTS)
    private val onboardingCompleteKey = booleanPreferencesKey(UserPreferencesKeys.ONBOARDING_COMPLETE)
    private val lastSelectedGroupIdKey = stringPreferencesKey(UserPreferencesKeys.LAST_SELECTED_GROUP_ID)
    private val lastSelectedGroupNameKey = stringPreferencesKey(UserPreferencesKeys.LAST_SELECTED_GROUP_NAME)
    private val locationWarningSeenKey = booleanPreferencesKey(UserPreferencesKeys.LOCATION_WARNING_SEEN)
    private val feedInterestOrderingKey = booleanPreferencesKey(UserPreferencesKeys.FEED_INTEREST_ORDERING)

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            userId = prefs[userIdKey],
            fullName = prefs[fullNameKey],
            avatarUrl = prefs[avatarUrlKey],
            neighborhoodId = prefs[neighborhoodIdKey],
            neighborhoodName = prefs[neighborhoodNameKey],
            interests = prefs[interestsKey]
                ?.split(',')
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            onboardingComplete = prefs[onboardingCompleteKey] ?: false,
            lastSelectedGroupId = prefs[lastSelectedGroupIdKey],
            lastSelectedGroupName = prefs[lastSelectedGroupNameKey],
            locationWarningSeen = prefs[locationWarningSeenKey] ?: false,
            // Absence in the store means "user hasn't chosen" → default off.
            feedInterestOrderingEnabled = prefs[feedInterestOrderingKey] ?: false
        )
    }

    override suspend fun saveUserId(id: String) {
        dataStore.edit { it[userIdKey] = id }
    }

    override suspend fun saveProfile(fullName: String, avatarUrl: String?) {
        dataStore.edit {
            it[fullNameKey] = fullName
            if (avatarUrl != null) it[avatarUrlKey] = avatarUrl else it.remove(avatarUrlKey)
        }
    }

    override suspend fun saveNeighborhood(id: String, name: String) {
        dataStore.edit {
            it[neighborhoodIdKey] = id
            it[neighborhoodNameKey] = name
        }
    }

    override suspend fun saveInterests(interests: List<Interest>) {
        dataStore.edit {
            it[interestsKey] = interests.joinToString(",") { interest -> interest.name }
        }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[onboardingCompleteKey] = complete }
    }

    override suspend fun saveLastSelectedGroup(groupId: String, groupName: String) {
        dataStore.edit {
            it[lastSelectedGroupIdKey] = groupId
            it[lastSelectedGroupNameKey] = groupName
        }
    }

    override suspend fun clearLastSelectedGroup() {
        dataStore.edit {
            it.remove(lastSelectedGroupIdKey)
            it.remove(lastSelectedGroupNameKey)
        }
    }

    override suspend fun setLocationWarningSeen() {
        dataStore.edit { it[locationWarningSeenKey] = true }
    }

    override suspend fun setFeedInterestOrdering(enabled: Boolean) {
        dataStore.edit { it[feedInterestOrderingKey] = enabled }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun datastorePath(): String {
        val documentDir: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        val path = requireNotNull(documentDir?.path) { "Could not resolve documents directory" }
        return "$path/$DATASTORE_FILE"
    }

    private companion object {
        const val DATASTORE_FILE = "inindy_user_prefs.preferences_pb"
    }
}
