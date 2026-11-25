package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SessionManager
import com.example.data.StoredUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionManagerTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager(context)
        sessionManager.clearSession()
    }

    @Test
    fun `test persistent session save and restore across instances`() {
        assertFalse(sessionManager.hasActiveSession())
        assertNull(sessionManager.getToken())
        assertNull(sessionManager.getUser())

        val user = StoredUser(
            id = "sg_karthi",
            displayName = "Admin Karthi",
            role = "ADMIN",
            mustChangePassword = true,
            status = "ACTIVE",
            createdAt = 1700000000L
        )

        sessionManager.saveSession(
            token = "test_token_abc_123",
            user = user,
            expiresAt = System.currentTimeMillis() + 86400000L
        )

        // Verify with same instance
        assertTrue(sessionManager.hasActiveSession())
        assertEquals("test_token_abc_123", sessionManager.getToken())
        assertEquals("sg_karthi", sessionManager.getUser()?.id)
        assertEquals("ADMIN", sessionManager.getUser()?.role)
        assertTrue(sessionManager.getUser()?.mustChangePassword == true)

        // Verify across newly created SessionManager instance (simulating app restart / revisit)
        val reloadedSessionManager = SessionManager(context)
        assertTrue(reloadedSessionManager.hasActiveSession())
        assertEquals("test_token_abc_123", reloadedSessionManager.getToken())
        assertEquals("sg_karthi", reloadedSessionManager.getUser()?.id)
        assertEquals("ADMIN", reloadedSessionManager.getUser()?.role)

        // Test update password changed flag
        reloadedSessionManager.updatePasswordChanged()
        assertFalse(reloadedSessionManager.getUser()?.mustChangePassword ?: true)

        // Test logout clearing session
        reloadedSessionManager.clearSession()
        assertFalse(reloadedSessionManager.hasActiveSession())
        assertNull(reloadedSessionManager.getToken())
        assertNull(reloadedSessionManager.getUser())
    }
}
