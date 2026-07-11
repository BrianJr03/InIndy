package jr.brian.inindy.domain.model

// Stable sort: posts with more matching tags come first; ties keep incoming
// order, which the repository has already ordered by created_at DESC. An
// empty `interests` set makes every match count zero, so the stable sort is
// a no-op and recency wins — no special case needed.
fun rankPostsByInterests(
    posts: List<Post>,
    interests: Set<Interest>
): List<Post> = posts.sortedByDescending { post ->
    post.tags.count { it in interests }
}

// Single point that resolves "what order should this feed be in right now?"
// — the toggle gate + the "group feeds are recency-only" rule both live
// here so the feed emission handler and the reactive re-order in the
// ViewModel stay in sync.
fun orderPostsFor(
    filter: ExploreFilter,
    posts: List<Post>,
    interests: Set<Interest>,
    interestOrderingEnabled: Boolean
): List<Post> = when (filter) {
    is ExploreFilter.All,
    is ExploreFilter.Neighborhood ->
        if (interestOrderingEnabled) rankPostsByInterests(posts, interests) else posts
    is ExploreFilter.Group -> posts
}
