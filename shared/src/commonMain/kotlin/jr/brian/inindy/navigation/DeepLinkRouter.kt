package jr.brian.inindy.navigation

sealed class DeepLinkResult {
    data object Auth : DeepLinkResult()
    data class GroupInvite(val token: String) : DeepLinkResult()
    data object Unknown : DeepLinkResult()
}

private const val INVITE_PREFIX = "inindy://invite/"

fun parseDeepLink(url: String): DeepLinkResult = when {
    url.startsWith("inindy://auth") || url.contains("access_token") || url.contains("code=") ->
        DeepLinkResult.Auth
    url.startsWith(INVITE_PREFIX) -> {
        val token = url.removePrefix(INVITE_PREFIX).substringBefore('?').substringBefore('#').trim()
        if (token.isEmpty()) DeepLinkResult.Unknown
        else DeepLinkResult.GroupInvite(token = token)
    }
    else -> DeepLinkResult.Unknown
}
