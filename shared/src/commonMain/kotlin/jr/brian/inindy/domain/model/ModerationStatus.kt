package jr.brian.inindy.domain.model

// Client-side view of `posts.moderation_status`.
// Server-authoritative, but the enum is *fail-open on the client*: unknown or
// null values map to APPROVED so that adding this column (and shipping this
// enum before the moderation backend exists) never falsely hides a post or
// crashes deserialization.
enum class ModerationStatus {
    APPROVED,
    PENDING,
    REJECTED;

    companion object {
        // Server strings are lowercase (`approved` / `pending` / `rejected`).
        // Anything else — including null, empty, or a future value the client
        // doesn't know yet — is treated as APPROVED.
        fun fromServer(value: String?): ModerationStatus = when (value?.lowercase()) {
            "rejected" -> REJECTED
            "pending" -> PENDING
            else -> APPROVED
        }
    }
}
