package jr.brian.inindy.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InterestParsingTest {

    @Test
    fun `maps known names in order`() {
        val parsed = Interest.fromStorageNames(listOf("HIKING", "COFFEE", "MUSIC"))
        assertEquals(listOf(Interest.HIKING, Interest.COFFEE, Interest.MUSIC), parsed)
    }

    @Test
    fun `silently drops unknown names`() {
        val parsed = Interest.fromStorageNames(listOf("HIKING", "NOT_A_REAL_INTEREST", "COFFEE"))
        assertEquals(listOf(Interest.HIKING, Interest.COFFEE), parsed)
    }

    @Test
    fun `empty input returns empty list`() {
        assertTrue(Interest.fromStorageNames(emptyList()).isEmpty())
    }

    @Test
    fun `preserves duplicates so callers control dedup`() {
        val parsed = Interest.fromStorageNames(listOf("HIKING", "HIKING"))
        assertEquals(listOf(Interest.HIKING, Interest.HIKING), parsed)
    }
}
