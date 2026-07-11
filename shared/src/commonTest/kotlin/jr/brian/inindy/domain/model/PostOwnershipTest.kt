package jr.brian.inindy.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostOwnershipTest {

    @Test
    fun `true when userId matches poster`() {
        val post = testPost(id = "1", userId = "alice")
        assertTrue(post.isOwnedBy("alice"))
    }

    @Test
    fun `false when userId is a different user`() {
        val post = testPost(id = "1", userId = "alice")
        assertFalse(post.isOwnedBy("bob"))
    }

    @Test
    fun `false when viewer is not signed in`() {
        val post = testPost(id = "1", userId = "alice")
        assertFalse(post.isOwnedBy(null))
    }
}
