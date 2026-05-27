package jr.brian.inindy.util

object DateUtil {
    private val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun formatEventDate(epochMs: Long): String {
        val totalDays = epochMs / 86_400_000L
        val (_, m, d) = epochDaysToYmd(totalDays)
        val hour = ((epochMs % 86_400_000L) / 3_600_000L).toInt()
        val minute = ((epochMs % 3_600_000L) / 60_000L).toInt()
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val monthName = months[(m - 1).coerceIn(0, 11)]
        return "$monthName $d at $displayHour:${minute.toString().padStart(2, '0')} $amPm"
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
                val (_, m, d) = epochDaysToYmd(epochMs / 86_400_000L)
                "${months[(m - 1).coerceIn(0, 11)]} $d"
            }
        }
    }

    // Howard Hinnant's civil calendar algorithm — epoch days to (year, month, day)
    private fun epochDaysToYmd(days: Long): Triple<Int, Int, Int> {
        val z = days + 719468L
        val era = if (z >= 0) z / 146097L else (z - 146096L) / 146097L
        val doe = (z - era * 146097L).toInt()
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = (yoe + era * 400L).toInt()
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val year = if (m <= 2) y + 1 else y
        return Triple(year, m, d)
    }
}
