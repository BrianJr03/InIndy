package jr.brian.inindy.data.remote.post

import jr.brian.inindy.domain.model.CreatePostRequest
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.PostAudience
import jr.brian.inindy.domain.model.User
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

fun PostDto.toDomain(): Post {
    val coords = location?.extractLngLat()
    return Post(
        id = id,
        userId = userId,
        title = title,
        description = description.orEmpty(),
        latitude = coords?.second ?: 0.0,
        longitude = coords?.first ?: 0.0,
        address = address.orEmpty(),
        startsAt = startsAt.toEpochMillis(),
        endsAt = endsAt?.toEpochMillis(),
        createdAt = createdAt.toEpochMillis(),
        tags = tags.mapNotNull { runCatching { Interest.valueOf(it.tag) }.getOrNull() },
        images = images.sortedBy { it.sortOrder }.map { it.storageUrl },
        videos = emptyList(),
        rsvpCount = rsvpCount,
        author = author?.toDomain(),
        neighborhoodId = neighborhoodId,
        neighborhoodName = neighborhood?.name,
        groupId = groupId
    )
}

fun UserDto.toDomain(): User = User(
    id = id,
    fullName = fullName,
    avatarUrl = avatarUrl
)

fun CreatePostRequest.toDto(userId: String, neighborhoodId: String): CreatePostDto = CreatePostDto(
    userId = userId,
    groupId = (audience as? PostAudience.GroupAudience)?.groupId,
    neighborhoodId = neighborhoodId,
    title = title,
    description = description,
    location = wktPoint(longitude, latitude),
    address = address,
    startsAt = startsAt.toIsoString(),
    endsAt = endsAt?.toIsoString(),
    maxAttendees = maxAttendees
)

private fun wktPoint(longitude: Double, latitude: Double): String =
    "SRID=4326;POINT($longitude $latitude)"

private fun JsonElement.extractLngLat(): Pair<Double, Double>? {
    val obj = this as? JsonObject ?: return null
    val coords = (obj["coordinates"] as? JsonArray) ?: return null
    if (coords.size < 2) return null
    val lng = (coords[0] as? JsonPrimitive)?.doubleOrNull ?: return null
    val lat = (coords[1] as? JsonPrimitive)?.doubleOrNull ?: return null
    return lng to lat
}

// ── ISO 8601 ↔ epoch millis ────────────────────────────────────────────────
// Domain timestamps are Long millis. Postgres TIMESTAMPTZ values arrive as
// ISO 8601 strings via PostgREST (e.g. "2026-06-09T12:34:56.123456+00:00").
// We parse/format here so the domain layer never sees strings.

fun String.toEpochMillis(): Long {
    val s = this.trim()
    if (s.isEmpty()) return 0L

    val tIdx = s.indexOf('T').let { if (it < 0) s.indexOf(' ') else it }
    if (tIdx <= 0) return 0L

    val datePart = s.substring(0, tIdx)
    val rest = s.substring(tIdx + 1)

    val dateBits = datePart.split('-')
    if (dateBits.size < 3) return 0L
    val year = dateBits[0].toIntOrNull() ?: return 0L
    val month = dateBits[1].toIntOrNull() ?: return 0L
    val day = dateBits[2].toIntOrNull() ?: return 0L

    val tzStart = findTzStart(rest)
    val timePart = if (tzStart >= 0) rest.substring(0, tzStart) else rest
    val tzPart = if (tzStart >= 0) rest.substring(tzStart) else ""

    val timeBits = timePart.split(':')
    val hour = timeBits.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = timeBits.getOrNull(1)?.toIntOrNull() ?: 0
    val secAndFraction = timeBits.getOrNull(2) ?: "0"
    val dotIdx = secAndFraction.indexOf('.')
    val seconds = (if (dotIdx >= 0) secAndFraction.substring(0, dotIdx) else secAndFraction)
        .toIntOrNull() ?: 0
    val fractionStr = if (dotIdx >= 0) secAndFraction.substring(dotIdx + 1) else ""
    val millisFraction = parseFractionToMillis(fractionStr)

    val tzOffsetMinutes = parseTzOffsetMinutes(tzPart)

    val epochDays = ymdToEpochDays(year, month, day)
    val timeMs = hour * 3_600_000L + minute * 60_000L + seconds * 1_000L + millisFraction
    val localMs = epochDays * 86_400_000L + timeMs
    return localMs - tzOffsetMinutes * 60_000L
}

fun Long.toIsoString(): String {
    val totalDays = floorDiv(this, 86_400_000L)
    val remainder = this - totalDays * 86_400_000L
    val (year, month, day) = epochDaysToYmd(totalDays)
    val hour = (remainder / 3_600_000L).toInt()
    val minute = ((remainder % 3_600_000L) / 60_000L).toInt()
    val second = ((remainder % 60_000L) / 1_000L).toInt()
    val ms = (remainder % 1_000L).toInt()
    val y = year.toString().padStart(4, '0')
    val mo = month.toString().padStart(2, '0')
    val d = day.toString().padStart(2, '0')
    val h = hour.toString().padStart(2, '0')
    val mi = minute.toString().padStart(2, '0')
    val s = second.toString().padStart(2, '0')
    val msPart = ms.toString().padStart(3, '0')
    return "$y-$mo-${d}T$h:$mi:$s.${msPart}Z"
}

private fun findTzStart(time: String): Int {
    for (i in time.indices) {
        val c = time[i]
        if (c == 'Z' || c == 'z' || (i > 0 && (c == '+' || c == '-'))) return i
    }
    return -1
}

private fun parseTzOffsetMinutes(tz: String): Long {
    if (tz.isEmpty() || tz.equals("Z", ignoreCase = true)) return 0L
    val sign = if (tz[0] == '-') -1L else 1L
    val body = tz.substring(1)
    val parts = body.split(':')
    val hours = parts.getOrNull(0)?.toLongOrNull() ?: 0L
    val mins = parts.getOrNull(1)?.toLongOrNull() ?: 0L
    return sign * (hours * 60L + mins)
}

private fun parseFractionToMillis(fraction: String): Long {
    if (fraction.isEmpty()) return 0L
    val padded = (fraction + "000").substring(0, 3)
    return padded.toLongOrNull() ?: 0L
}

private fun floorDiv(a: Long, b: Long): Long {
    var q = a / b
    if ((a xor b) < 0 && q * b != a) q -= 1
    return q
}

// Howard Hinnant civil calendar — both directions.
private fun ymdToEpochDays(y: Int, m: Int, d: Int): Long {
    val year = if (m <= 2) y - 1 else y
    val era = (if (year >= 0) year else year - 399) / 400
    val yoe = (year - era * 400).toLong()
    val mp = if (m > 2) m - 3 else m + 9
    val doy = (153L * mp + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era.toLong() * 146097L + doe - 719468L
}

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
