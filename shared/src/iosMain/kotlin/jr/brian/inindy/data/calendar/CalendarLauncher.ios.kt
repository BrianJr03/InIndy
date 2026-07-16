package jr.brian.inindy.data.calendar

import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKitUI.EKEventEditViewAction
import platform.EventKitUI.EKEventEditViewController
import platform.EventKitUI.EKEventEditViewDelegateProtocol
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UIKit.UIApplication
import platform.darwin.NSObject

private val eventStore = EKEventStore()

// Held statically so ARC doesn't reclaim it before the sheet dismisses.
private var activeDelegate: CalendarEditDelegate? = null

private class CalendarEditDelegate : NSObject(), EKEventEditViewDelegateProtocol {
    override fun eventEditViewController(
        controller: EKEventEditViewController,
        didCompleteWithAction: EKEventEditViewAction
    ) {
        controller.dismissViewControllerAnimated(true, completion = null)
        if (activeDelegate === this) activeDelegate = null
    }
}

actual fun openCalendarEvent(
    title: String,
    description: String,
    location: String,
    startMs: Long,
    endMs: Long?
) {
    val end = endMs ?: (startMs + 60L * 60L * 1000L)
    val event = EKEvent.eventWithEventStore(eventStore).apply {
        setTitle(title)
        setNotes(description)
        setLocation(location)
        setStartDate(NSDate.dateWithTimeIntervalSince1970(startMs / 1000.0))
        setEndDate(NSDate.dateWithTimeIntervalSince1970(end / 1000.0))
    }
    val delegate = CalendarEditDelegate()
    activeDelegate = delegate
    val editVC = EKEventEditViewController().apply {
        setEventStore(eventStore)
        setEvent(event)
        setEditViewDelegate(delegate)
    }
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.presentViewController(editVC, animated = true, completion = null)
}
