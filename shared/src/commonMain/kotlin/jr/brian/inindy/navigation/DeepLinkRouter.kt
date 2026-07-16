package jr.brian.inindy.navigation

sealed class DeepLinkResult {
    data object Auth : DeepLinkResult()
    data class GroupInvite(val token: String) : DeepLinkResult()
    data class Post(val postId: String) : DeepLinkResult()
    data object Unknown : DeepLinkResult()
}

private const val INVITE_PREFIX = "inindy://invite/"
private const val POST_PREFIX = "inindy://post/"

fun parseDeepLink(url: String): DeepLinkResult = when {
    url.startsWith("inindy://auth") || url.contains("access_token") || url.contains("code=") ->
        DeepLinkResult.Auth
    url.startsWith(INVITE_PREFIX) -> {
        val token = url.removePrefix(INVITE_PREFIX).substringBefore('?').substringBefore('#').trim()
        if (token.isEmpty()) DeepLinkResult.Unknown
        else DeepLinkResult.GroupInvite(token = token)
    }
    url.startsWith(POST_PREFIX) -> {
        val postId = url.removePrefix(POST_PREFIX).substringBefore('?').substringBefore('#').trim()
        if (postId.isEmpty()) DeepLinkResult.Unknown
        else DeepLinkResult.Post(postId = postId)
    }
    else -> DeepLinkResult.Unknown
}
