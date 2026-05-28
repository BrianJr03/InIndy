package jr.brian.inindy.data.local

expect class TokenStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}
