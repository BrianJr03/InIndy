package jr.brian.inindy.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

actual class TokenStorage(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    actual fun saveToken(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    actual fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    actual fun clearToken() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    private companion object {
        const val PREFS_NAME = "inindy_secure_prefs"
        const val KEY_TOKEN = "auth_token"
    }
}
