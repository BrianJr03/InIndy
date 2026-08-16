package jr.brian.inindy.util

// FirebaseInitProvider (registered by the google-services plugin's manifest merge)
// runs before Application.onCreate, so Firebase is already up by the time Koin starts.
actual fun initFirebase() = Unit
