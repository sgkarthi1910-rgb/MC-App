package com.example.data

import android.content.Context
import android.content.SharedPreferences

data class StoredUser(
    val id: String,
    val displayName: String,
    val role: String,
    val mustChangePassword: Boolean,
    val status: String,
    val createdAt: Long
)

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var inMemoryToken: String? = null

    @Volatile
    private var inMemoryUser: StoredUser? = null

    @Volatile
    private var inMemoryExpiresAt: Long = 0L

    init {
        // Strictly remove any legacy persisted auth tokens/flags so app launch always requires login
        clearPersistedAuthPrefs()
    }

    companion object {
        private const val PREFS_NAME = "tgz_auth_session_prefs"
        private const val KEY_TOKEN = "session_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DISPLAY_NAME = "user_display_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_MUST_CHANGE_PASSWORD = "must_change_password"
        private const val KEY_STATUS = "user_status"
        private const val KEY_CREATED_AT = "user_created_at"
        private const val KEY_EXPIRES_AT = "session_expires_at"
        private const val KEY_BACKEND_URL = "custom_backend_url"
        const val DEFAULT_BACKEND_URL = "https://mc-app-ten.vercel.app/"
    }

    private fun clearPersistedAuthPrefs() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_ROLE)
            .remove(KEY_MUST_CHANGE_PASSWORD)
            .remove(KEY_STATUS)
            .remove(KEY_CREATED_AT)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    fun saveSession(token: String, user: StoredUser, expiresAt: Long = 0L) {
        inMemoryToken = token
        inMemoryUser = user
        inMemoryExpiresAt = expiresAt
    }

    fun saveUser(user: StoredUser) {
        inMemoryUser = user
    }

    fun updatePasswordChanged() {
        inMemoryUser = inMemoryUser?.copy(mustChangePassword = false)
    }

    fun getToken(): String? = inMemoryToken?.takeIf { it.isNotBlank() }

    fun getUser(): StoredUser? = inMemoryUser

    fun hasActiveSession(): Boolean {
        val token = getToken()
        val user = getUser()
        val expiresAt = inMemoryExpiresAt
        if (token.isNullOrBlank() || user == null) return false
        if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            clearSession()
            return false
        }
        return true
    }

    fun clearSession() {
        inMemoryToken = null
        inMemoryUser = null
        inMemoryExpiresAt = 0L
        clearPersistedAuthPrefs()
    }

    fun getBackendUrl(): String {
        val saved = prefs.getString(KEY_BACKEND_URL, null)
        if (saved.isNullOrBlank() || (!saved.startsWith("http://") && !saved.startsWith("https://"))) {
            return DEFAULT_BACKEND_URL
        }
        return saved
    }

    fun setBackendUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_BACKEND_URL, normalized).apply()
    }
}
