package jr.brian.inindy.domain.model

// Public shareable prefix for group invite links. The Cloudflare Worker at this
// origin redirects browsers into the app via the inindy:// deep link scheme
// (see navigation/DeepLinkRouter.INVITE_PREFIX) or into a fallback web page.
const val GROUP_INVITE_URL_PREFIX = "https://in-indy-invite.thaballa79.workers.dev/invite/"

data class GroupInvite(
    val id: String,
    val groupId: String,
    val invitedBy: String,
    val token: String,
    val createdAt: Long,
    val expiresAt: Long
)

fun GroupInvite.toShareableUrl(): String = "$GROUP_INVITE_URL_PREFIX$token"
