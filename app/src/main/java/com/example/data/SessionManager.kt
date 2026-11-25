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

    fun saveSession(token: String, user: StoredUser, expiresAt: Long = 0L) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_DISPLAY_NAME, user.displayName)
            .putString(KEY_ROLE, user.role)
            .putBoolean(KEY_MUST_CHANGE_PASSWORD, user.mustChangePassword)
            .putString(KEY_STATUS, user.status)
            .putLong(KEY_CREATED_AT, user.createdAt)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun updatePasswordChanged() {
        prefs.edit().putBoolean(KEY_MUST_CHANGE_PASSWORD, false).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun getUser(): StoredUser? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val displayName = prefs.getString(KEY_DISPLAY_NAME, id) ?: id
        val role = prefs.getString(KEY_ROLE, "MEMBER") ?: "MEMBER"
        val mustChange = prefs.getBoolean(KEY_MUST_CHANGE_PASSWORD, false)
        val status = prefs.getString(KEY_STATUS, "ACTIVE") ?: "ACTIVE"
        val createdAt = prefs.getLong(KEY_CREATED_AT, 0L)
        return StoredUser(
            id = id,
            displayName = displayName,
            role = role,
            mustChangePassword = mustChange,
            status = status,
            createdAt = createdAt
        )
    }

    fun hasActiveSession(): Boolean {
        val token = getToken()
        val user = getUser()
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (token.isNullOrBlank() || user == null) return false
        if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            clearSession()
            return false
        }
        return true
    }

    fun clearSession() {
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

    fun getBackendUrl(): String {
        val saved = prefs.getString(KEY_BACKEND_URL, null)
        if (saved.isNullOrBlank() || !saved.startsWith("https://mc-app-ten.vercel.app")) {
            return DEFAULT_BACKEND_URL
        }
        return saved
    }

    fun setBackendUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_BACKEND_URL, normalized).apply()
    }
}
