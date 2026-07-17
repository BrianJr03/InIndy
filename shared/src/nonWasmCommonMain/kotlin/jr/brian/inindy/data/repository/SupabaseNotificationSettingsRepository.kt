package jr.brian.inindy.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import jr.brian.inindy.data.remote.settings.NotificationSettingsRow
import jr.brian.inindy.data.remote.settings.NotificationSettingsUpsertDto
import jr.brian.inindy.domain.CurrentUserProvider
import jr.brian.inindy.domain.model.RsvpReminderPref
import jr.brian.inindy.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseNotificationSettingsRepository(
    private val supabase: SupabaseClient,
    private val currentUserProvider: CurrentUserProvider,
) : NotificationSettingsRepository {

    // One-shot fetch flow — the value only changes from this device, so the
    // ViewModel maintains local state via optimistic updates on set.
    override fun observeRsvpReminder(): Flow<Result<RsvpReminderPref>> = flow {
        emit(runCatching {
            val userId = currentUserProvider.get().userId
                ?: return@runCatching RsvpReminderPref.DAY_OF
            val row = supabase.from(SETTINGS_TABLE)
                .select { filter { eq("user_id", userId) }; limit(1) }
                .decodeSingleOrNull<NotificationSettingsRow>()
            RsvpReminderPref.fromWire(row?.rsvpReminder)
        })
    }

    override suspend fun setRsvpReminder(pref: RsvpReminderPref): Result<Unit> = runCatching {
        val userId = currentUserProvider.get().userId ?: error("No signed-in user")
        supabase.from(SETTINGS_TABLE).upsert(
            NotificationSettingsUpsertDto(userId = userId, rsvpReminder = pref.wire)
        ) {
            onConflict = "user_id"
        }
    }.map { }

    private companion object {
        const val SETTINGS_TABLE = "notification_settings"
    }
}
