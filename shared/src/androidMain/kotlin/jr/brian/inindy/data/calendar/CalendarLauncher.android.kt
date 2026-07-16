package jr.brian.inindy.data.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import jr.brian.inindy.data.media.ActivityProvider
import org.koin.mp.KoinPlatformTools

actual fun openCalendarEvent(
    title: String,
    description: String,
    location: String,
    startMs: Long,
    endMs: Long?
) {
    val koin = KoinPlatformTools.defaultContext().getOrNull() ?: return
    val activity = koin.get<ActivityProvider>().current()
    val context: Context = activity ?: koin.get()

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, description)
        putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
        putExtra(
            CalendarContract.EXTRA_EVENT_END_TIME,
            endMs ?: (startMs + 60L * 60L * 1000L)
        )
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
    }
    if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}
