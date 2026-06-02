package jr.brian.inindy.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import jr.brian.inindy.domain.model.Interest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

class UserPreferencesStoreImpl(context: Context) : UserPreferencesStore {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { context.filesDir.resolve(DATASTORE_FILE).absolutePath.toPath() }
    )

    private val userIdKey = stringPreferencesKey(UserPreferencesKeys.USER_ID)
    private val fullNameKey = stringPreferencesKey(UserPreferencesKeys.FULL_NAME)
    private val avatarUrlKey = stringPreferencesKey(UserPreferencesKeys.AVATAR_URL)
    private val neighborhoodIdKey = stringPreferencesKey(UserPreferencesKeys.NEIGHBORHOOD_ID)
    private val neighborhoodNameKey = stringPreferencesKey(UserPreferencesKeys.NEIGHBORHOOD_NAME)
    private val interestsKey = stringPreferencesKey(UserPreferencesKeys.INTERESTS)
    private val onboardingCompleteKey = booleanPreferencesKey(UserPreferencesKeys.ONBOARDING_COMPLETE)

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
            onboardingComplete = prefs[onboardingCompleteKey] ?: false
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

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        const val DATASTORE_FILE = "inindy_user_prefs.preferences_pb"
    }
}
