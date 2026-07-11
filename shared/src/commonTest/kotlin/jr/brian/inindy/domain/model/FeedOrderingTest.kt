package jr.brian.inindy.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FeedOrderingTest {
    private val hiker = testPost(id = "hike", tags = listOf(Interest.HIKING))
    private val runnerHiker = testPost(
        id = "runHike",
        tags = listOf(Interest.RUNNING, Interest.HIKING)
    )
    private val coffee = testPost(id = "coffee", tags = listOf(Interest.COFFEE))
    // Repository already delivers newest-first; tests keep that intent by
    // naming the input in the recency order the repo would produce.
    private val recencyOrder = listOf(hiker, coffee, runnerHiker)

    // ── rankPostsByInterests ────────────────────────────────────────────────

    @Test
    fun `rank moves posts with more matching tags earlier and stably preserves ties`() {
        val ranked = rankPostsByInterests(
            posts = recencyOrder,
            interests = setOf(Interest.HIKING, Interest.RUNNING)
        )
        // runnerHiker: 2 matches; hiker: 1; coffee: 0. Ties (none here) would
        // keep incoming order since sortedByDescending is stable.
        assertEquals(listOf(runnerHiker, hiker, coffee), ranked)
    }

    @Test
    fun `rank with empty interests keeps the incoming order`() {
        val ranked = rankPostsByInterests(recencyOrder, emptySet())
        assertEquals(recencyOrder, ranked)
    }

    // ── orderPostsFor ───────────────────────────────────────────────────────

    @Test
    fun `neighborhood filter with ordering enabled ranks by interests`() {
        val result = orderPostsFor(
            filter = ExploreFilter.Neighborhood,
            posts = recencyOrder,
            interests = setOf(Interest.HIKING),
            interestOrderingEnabled = true
        )
        // hiker + runnerHiker both match; hiker is earlier in the input so wins the tie.
        assertEquals(listOf(hiker, runnerHiker, coffee), result)
    }

    @Test
    fun `All filter with ordering enabled also ranks by interests`() {
        val result = orderPostsFor(
            filter = ExploreFilter.All,
            posts = recencyOrder,
            interests = setOf(Interest.RUNNING),
            interestOrderingEnabled = true
        )
        assertEquals(listOf(runnerHiker, hiker, coffee), result)
    }

    @Test
    fun `ordering-disabled toggle preserves recency`() {
        val result = orderPostsFor(
            filter = ExploreFilter.Neighborhood,
            posts = recencyOrder,
            interests = setOf(Interest.HIKING),
            interestOrderingEnabled = false
        )
        assertEquals(recencyOrder, result)
    }

    @Test
    fun `empty interests with ordering enabled behaves as no-op`() {
        val result = orderPostsFor(
            filter = ExploreFilter.Neighborhood,
            posts = recencyOrder,
            interests = emptySet(),
            interestOrderingEnabled = true
        )
        assertEquals(recencyOrder, result)
    }

    @Test
    fun `group filter is always recency-only even with ordering enabled`() {
        val result = orderPostsFor(
            filter = ExploreFilter.Group(groupId = "g1", groupName = "G"),
            posts = recencyOrder,
            interests = setOf(Interest.HIKING, Interest.RUNNING),
            interestOrderingEnabled = true
        )
        assertEquals(recencyOrder, result)
    }
}
