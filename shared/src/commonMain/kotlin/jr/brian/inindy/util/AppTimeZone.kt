package jr.brian.inindy.util

import kotlinx.datetime.TimeZone

// InIndy events are always Indianapolis-local regardless of the viewing device's
// zone. Every conversion between a user-picked wall clock and the real UTC
// instant stored in posts.starts_at / posts.ends_at must go through this zone.
val AppTimeZone: TimeZone = TimeZone.of("America/Indiana/Indianapolis")
