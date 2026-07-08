package jr.brian.inindy.data.chat

/**
 * Instant-UX profanity gate for chat send. The authoritative moderation runs
 * server-side via the `moderate-message` edge function, so this list is small
 * on purpose — its only job is to give the sender a "please rephrase" nudge
 * before the message goes out.
 */
class ProfanityFilter(
    additional: Set<String> = emptySet()
) {
    private val words: Set<String> = (BASE_WORDS + additional.map { it.lowercase() }).toSet()

    fun matches(body: String): Boolean {
        if (body.isBlank()) return false
        val tokens = body.lowercase().split(NON_WORD_CHARS)
        return tokens.any { it.isNotEmpty() && it in words }
    }

    private companion object {
        val NON_WORD_CHARS = Regex("[^a-z0-9']+")
        val BASE_WORDS = setOf(
            "fuck", "fucker", "fucking", "shit", "bitch", "asshole", "bastard",
            "cunt", "dick", "piss", "slut", "whore", "faggot", "nigger", "retard"
        )
    }
}
