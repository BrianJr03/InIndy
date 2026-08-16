package jr.brian.inindy.util

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

actual fun initFirebase() {
    Firebase.initialize()
}
