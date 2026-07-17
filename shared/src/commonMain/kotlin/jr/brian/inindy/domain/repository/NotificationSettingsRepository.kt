package jr.brian.inindy.domain.repository

import jr.brian.inindy.domain.model.RsvpReminderPref
import kotlinx.coroutines.flow.Flow

interface NotificationSettingsRepository {
    fun observeRsvpReminder(): Flow<Result<RsvpReminderPref>>
    suspend fun setRsvpReminder(pref: RsvpReminderPref): Result<Unit>
}
