package jr.brian.inindy.domain.model

data class Post(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val startsAt: Long,
    val endsAt: Long?,
    val createdAt: Long,
    val tags: List<Interest>,
    val images: List<String>,
    val videos: List<VideoMedia>,
    val rsvpCount: Int,
    val author: User?,
    val neighborhoodId: String? = null,
    val neighborhoodName: String? = null,
    val groupId: String? = null,
    val maxAttendees: Int? = null,
    val moderationStatus: ModerationStatus = ModerationStatus.APPROVED,
    val previewAttendees: List<User> = emptyList()
)

/**
 * True when [userId] is the poster. A null [userId] (viewer not signed in)
 * always returns false because [Post.userId] is non-null.
 */
fun Post.isOwnedBy(userId: String?): Boolean = this.userId == userId

/**
 * Returns a copy of this post with [rsvpCount] shifted by [delta], clamped so
 * an optimistic decrement can't render as a negative attendee total.
 */
fun Post.withRsvpCountDelta(delta: Int): Post =
    copy(rsvpCount = (rsvpCount + delta).coerceAtLeast(0))

/**
 * Optimistically applies an RSVP change to an attendee list: adds [viewer] on a
 * positive delta (if not already present) and removes them on a negative delta.
 * A null viewer or a zero delta returns the list unchanged so the caller doesn't
 * need to branch. Single source of truth for optimistic RSVP UI updates —
 * mirror any change here across every attendee list a screen renders.
 */
fun List<User>.applyRsvpDelta(delta: Int, viewer: User?): List<User> {
    if (viewer == null) return this
    return when {
        delta > 0 && none { it.id == viewer.id } -> this + viewer
        delta < 0 -> filterNot { it.id == viewer.id }
        else -> this
    }
}
