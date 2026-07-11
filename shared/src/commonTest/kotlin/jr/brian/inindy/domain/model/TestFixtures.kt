package jr.brian.inindy.domain.model

/**
 * Compact [Post] factory for tests — fills every non-nullable field with a
 * throwaway default so callers override only what the assertion cares about.
 */
internal fun testPost(
    id: String,
    userId: String = "poster-$id",
    tags: List<Interest> = emptyList(),
    rsvpCount: Int = 0,
    previewAttendees: List<User> = emptyList()
): Post = Post(
    id = id,
    userId = userId,
    title = "Post $id",
    description = "",
    latitude = 0.0,
    longitude = 0.0,
    address = "",
    startsAt = 0L,
    endsAt = null,
    createdAt = 0L,
    tags = tags,
    images = emptyList(),
    videos = emptyList(),
    rsvpCount = rsvpCount,
    author = null,
    previewAttendees = previewAttendees
)

internal fun testUser(id: String, fullName: String? = id): User =
    User(id = id, fullName = fullName, avatarUrl = null)
