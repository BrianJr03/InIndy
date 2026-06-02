package jr.brian.inindy.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit
import java.security.KeyStore

actual class TokenStorage(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context)

    private fun createPrefs(context: Context): SharedPreferences = try {
        buildEncryptedPrefs(context)
    } catch (t: Throwable) {
        resetSecureStorage(context)
        buildEncryptedPrefs(context)
    }

    private fun buildEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun resetSecureStorage(context: Context) {
        context.deleteSharedPreferences(PREFS_NAME)
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
                if (containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                    deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                }
            }
        }
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
