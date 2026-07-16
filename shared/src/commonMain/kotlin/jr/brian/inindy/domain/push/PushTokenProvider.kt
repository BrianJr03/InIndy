package jr.brian.inindy.domain.push

interface PushTokenProvider {
    val platform: String            // "android" | "ios"
    suspend fun currentToken(): String?
}
