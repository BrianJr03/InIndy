package jr.brian.inindy.data.media

import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

class ActivityProvider {

    private var ref: WeakReference<ComponentActivity>? = null

    fun attach(activity: ComponentActivity) {
        ref = WeakReference(activity)
    }

    fun detach(activity: ComponentActivity) {
        if (ref?.get() === activity) ref = null
    }

    fun current(): ComponentActivity? = ref?.get()
}
