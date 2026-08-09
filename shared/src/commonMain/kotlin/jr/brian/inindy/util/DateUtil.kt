package jr.brian.inindy.util

import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

expect fun currentTimeMillis(): Long

object DateUtil {
    private val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    // startsAt / endsAt are real UTC epoch millis. Convert to the app's fixed
    // Indianapolis zone before extracting calendar components so the label
    // matches what the poster meant, not what the viewer's device thinks.
    fun formatEventDate(epochMs: Long): String {
        val local = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(AppTimeZone)
        val hour = local.hour
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val monthName = months[(local.monthNumber - 1).coerceIn(0, 11)]
        return "$monthName ${local.dayOfMonth} at $displayHour:${local.minute.toString().padStart(2, '0')} $amPm"
    }

    fun formatRelativeDate(epochMs: Long, nowMs: Long): String {
        val diffMs = nowMs - epochMs
        val diffMinutes = diffMs / 60_000L
        val diffHours = diffMs / 3_600_000L
        val diffDays = diffMs / 86_400_000L
        return when {
            diffMinutes < 1 -> "just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays < 7 -> "${diffDays}d ago"
            else -> {
                val local = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(AppTimeZone)
                "${months[(local.monthNumber - 1).coerceIn(0, 11)]} ${local.dayOfMonth}"
            }
        }
    }
}
