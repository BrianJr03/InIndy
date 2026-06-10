package jr.brian.inindy.data.remote.post

import io.github.jan.supabase.exceptions.RestException

fun Throwable.toPostError(): String = when (this) {
    is RestException -> when (statusCode) {
        401 -> "You need to sign in to do that"
        403 -> "You don't have permission to do that"
        404 -> "We couldn't find that post"
        409 -> "That post already exists"
        413 -> "That image is too large"
        in 500..599 -> "Server hiccup — please try again"
        else -> message ?: "Something went wrong — please try again"
    }
    else -> {
        val msg = message.orEmpty()
        when {
            msg.contains("timeout", ignoreCase = true) ->
                "Request timed out — check your connection"
            msg.contains("network", ignoreCase = true) ||
                msg.contains("unable to resolve host", ignoreCase = true) ->
                "Can't reach the server — check your connection"
            else -> "Something went wrong — please try again"
        }
    }
}
