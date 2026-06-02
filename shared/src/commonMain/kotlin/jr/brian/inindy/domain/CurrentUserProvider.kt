package jr.brian.inindy.domain

import jr.brian.inindy.data.local.UserPreferences
import jr.brian.inindy.data.local.UserPreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CurrentUserProvider(
    private val userPreferencesStore: UserPreferencesStore
) {
    val user: Flow<UserPreferences> = userPreferencesStore.preferences

    suspend fun get(): UserPreferences = userPreferencesStore.preferences.first()
}
