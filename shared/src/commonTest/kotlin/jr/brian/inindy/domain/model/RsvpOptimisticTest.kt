package jr.brian.inindy.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RsvpOptimisticTest {

    private val alice = testUser("alice")
    private val bob = testUser("bob")

    // ── Post.withRsvpCountDelta ─────────────────────────────────────────────

    @Test
    fun `withRsvpCountDelta increments count`() {
        val updated = testPost(id = "1", rsvpCount = 3).withRsvpCountDelta(1)
        assertEquals(4, updated.rsvpCount)
    }

    @Test
    fun `withRsvpCountDelta decrements count`() {
        val updated = testPost(id = "1", rsvpCount = 3).withRsvpCountDelta(-1)
        assertEquals(2, updated.rsvpCount)
    }

    @Test
    fun `withRsvpCountDelta clamps at zero on decrement`() {
        // Stale count in state after a race — never render negative attendees.
        val updated = testPost(id = "1", rsvpCount = 0).withRsvpCountDelta(-1)
        assertEquals(0, updated.rsvpCount)
    }

    // ── List<User>.applyRsvpDelta ───────────────────────────────────────────

    @Test
    fun `applyRsvpDelta adds viewer on positive delta when absent`() {
        val next = listOf(bob).applyRsvpDelta(delta = 1, viewer = alice)
        assertEquals(listOf(bob, alice), next)
    }

    @Test
    fun `applyRsvpDelta is idempotent on positive delta when already present`() {
        val next = listOf(alice, bob).applyRsvpDelta(delta = 1, viewer = alice)
        assertEquals(listOf(alice, bob), next)
    }

    @Test
    fun `applyRsvpDelta removes viewer on negative delta`() {
        val next = listOf(alice, bob).applyRsvpDelta(delta = -1, viewer = alice)
        assertEquals(listOf(bob), next)
    }

    @Test
    fun `applyRsvpDelta with null viewer is a no-op`() {
        val list = listOf(alice, bob)
        assertEquals(list, list.applyRsvpDelta(delta = 1, viewer = null))
        assertEquals(list, list.applyRsvpDelta(delta = -1, viewer = null))
    }

    @Test
    fun `applyRsvpDelta with zero delta is a no-op`() {
        val list = listOf(alice, bob)
        assertEquals(list, list.applyRsvpDelta(delta = 0, viewer = alice))
    }
}
