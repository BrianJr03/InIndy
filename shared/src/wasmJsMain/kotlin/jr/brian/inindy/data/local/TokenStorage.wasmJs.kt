package jr.brian.inindy.data.local

import kotlinx.browser.localStorage

actual class TokenStorage {
    actual fun saveToken(token: String) {
        localStorage.setItem(KEY, token)
    }

    actual fun getToken(): String? = localStorage.getItem(KEY)

    actual fun clearToken() {
        localStorage.removeItem(KEY)
    }

    private companion object {
        const val KEY = "inindy_auth_token"
    }
}
