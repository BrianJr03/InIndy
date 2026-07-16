package jr.brian.inindy.data.calendar

expect fun openCalendarEvent(
    title: String,
    description: String,
    location: String,
    startMs: Long,
    endMs: Long?
)
