package jr.brian.inindy.data.remote.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationSettingsRow(
    @SerialName("rsvp_reminder") val rsvpReminder: String? = null
)

@Serializable
data class NotificationSettingsUpsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("rsvp_reminder") val rsvpReminder: String
)
