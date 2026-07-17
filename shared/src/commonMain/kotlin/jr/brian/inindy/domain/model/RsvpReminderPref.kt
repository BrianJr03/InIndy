package jr.brian.inindy.domain.model

// Wire values MUST match the CHECK constraint on notification_settings.rsvp_reminder
// (see 20260716000001_notification_settings.sql). Default here mirrors the SQL
// column default — flip both together if you change one.
enum class RsvpReminderPref(val wire: String) {
    NONE("none"),
    DAY_OF("day_of"),
    HOUR_BEFORE("hour_before");

    companion object {
        fun fromWire(v: String?): RsvpReminderPref =
            entries.firstOrNull { it.wire == v } ?: DAY_OF
    }
}
