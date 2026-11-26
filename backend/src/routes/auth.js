const { hashPassword, hashPasswordSync, verifyPassword, generateSessionToken, hashToken, generateId } = require('../crypto-utils');
const { provisionAdminAccount, getAdminUserIds, isAdminUserId } = require('../db');

function setupAuthRoutes(router, db) {
  // 0. SAFE AUTHENTICATION DIAGNOSTICS (Zero Secrets Expose)
  router.get('/api/auth/diagnostics', (req, res) => {
    let dbConnected = false;
    let adminProvisioned = false;
    let adminMustChangePassword = false;
    let userCount = 0;
    try {
      const adminIds = getAdminUserIds();
      const admins = db.prepare("SELECT id, role, must_change_password FROM users WHERE role = 'ADMIN'").all();
      adminProvisioned = adminIds.length > 0 && adminIds.every(id => admins.some(a => a.id === id));
      adminMustChangePassword = admins.some(a => Boolean(a.must_change_password));
      const countRow = db.prepare('SELECT COUNT(*) as cnt FROM users').get();
      userCount = countRow?.cnt || 0;
      dbConnected = true;
    } catch (err) {
      dbConnected = false;
    }

    return res.json({
      status: dbConnected ? 'ok' : 'error',
      databaseConnected: dbConnected,
      databaseType: process.env.VERCEL ? 'persisted-serverless-store' : 'sqlite-file',
      adminProvisioned,
      adminMustChangePassword,
      adminUserIds: getAdminUserIds(),
      totalUsers: userCount,
      serverTime: Date.now()
    });
  });
  // 1. SIGNUP: Public registration creates only MEMBER accounts
  router.post('/api/auth/signup', async (req, res) => {
    try {
      const { password, confirmPassword, displayName, deviceInfo } = req.body || {};
      const id = req.body?.id || req.body?.uniqueId;

      if (!id || typeof id !== 'string' || id.trim().length < 3 || id.trim().length > 32) {
        return res.status(400).json({ error: 'Unique ID must be between 3 and 32 characters.' });
      }

      // Check unique ID formatting (alphanumeric and underscores)
      const cleanId = id.trim().toLowerCase();
      if (!/^[a-z0-9_]+$/.test(cleanId)) {
        return res.status(400).json({ error: 'Unique ID may only contain lowercase letters, numbers, and underscores.' });
      }

      // Prevent registration of reserved names
      if (isAdminUserId(cleanId) || cleanId === 'admin' || cleanId === 'root' || cleanId === 'moderator') {
        return res.status(409).json({ error: 'This Unique ID is already registered or reserved.' });
      }

      // Check duplicate ID
      const existing = db.prepare('SELECT id FROM users WHERE id = ?').get(cleanId);
      if (existing) {
        return res.status(409).json({ error: 'Unique ID already taken. Please choose another.' });
      }

      // Validate password strength
      if (!password || typeof password !== 'string' || password.length < 8) {
        return res.status(400).json({ error: 'Password must be at least 8 characters long.' });
      }

      if (password !== confirmPassword) {
        return res.status(400).json({ error: 'Passwords do not match.' });
      }

      // Secure password hash
      const passwordHash = await hashPassword(password);
      const now = Date.now();
      const userDisplayName = (displayName && typeof displayName === 'string' && displayName.trim().length > 0)
        ? displayName.trim()
        : cleanId;

      // Always create as MEMBER - public registration cannot choose Admin or Moderator
      db.prepare(`
        INSERT INTO users (id, display_name, password_hash, role, must_change_password, status, created_at, last_login_at)
        VALUES (?, ?, ?, 'MEMBER', 0, 'ACTIVE', ?, ?)
      `).run(cleanId, userDisplayName, passwordHash, now, now);

      // Create persistent session
      const { rawToken, tokenHash } = generateSessionToken();
      const sessionId = generateId('sess');
      const expirationDays = parseInt(process.env.SESSION_EXPIRATION_DAYS || '30', 10);
      const expiresAt = now + (expirationDays * 24 * 60 * 60 * 1000);
      const clientIp = req.socket?.remoteAddress || 'unknown';

      db.prepare(`
        INSERT INTO sessions (id, user_id, token_hash, device_info, ip_address, created_at, last_active_at, expires_at, is_revoked)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
      `).run(sessionId, cleanId, tokenHash, deviceInfo || 'TGZ App', clientIp, now, now, expiresAt);

      // Set HttpOnly cookie
      const isHttps = req.headers['x-forwarded-proto'] === 'https' || !!process.env.VERCEL;
      const secureFlag = isHttps ? '; Secure' : '';
      res.setHeader('Set-Cookie', `tgz_session=${rawToken}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${expirationDays * 86400}${secureFlag}`);

      return res.status(201).json({
        success: true,
        message: 'Registration successful',
        sessionToken: rawToken,
        sessionId,
        expiresAt,
        user: {
          id: cleanId,
          displayName: userDisplayName,
          role: 'MEMBER',
          mustChangePassword: false,
          status: 'ACTIVE',
          createdAt: now
        }
      });
    } catch (err) {
      console.error('Signup error:', err);
      return res.status(500).json({ error: 'Internal server error during registration.' });
    }
  });

  // 2. LOGIN: Authenticate using Unique ID and Password
  router.post('/api/auth/login', async (req, res) => {
    try {
      const { password, deviceInfo } = req.body || {};
      const id = req.body?.id || req.body?.uniqueId;

      if (!id || !password) {
        return res.status(400).json({
          success: false,
          error: 'Unique ID and password are required.',
          code: 'MISSING_CREDENTIALS',
          diagnostics: { field: !id ? 'id' : 'password' }
        });
      }

      const cleanId = id.trim().toLowerCase();
      let user = db.prepare(`
        SELECT id, display_name, password_hash, role, must_change_password, status, created_at
        FROM users WHERE id = ?
      `).get(cleanId);

      // Idempotent server-side provisioning: If admin account is requested but not yet in DB, provision safely
      if (!user && isAdminUserId(cleanId)) {
        provisionAdminAccount(db);
        user = db.prepare(`
          SELECT id, display_name, password_hash, role, must_change_password, status, created_at
          FROM users WHERE id = ?
        `).get(cleanId);
      }

      if (!user) {
        const isTargetAdmin = isAdminUserId(cleanId);
        return res.status(401).json({
          success: false,
          error: isTargetAdmin
            ? 'Administrator account has not been provisioned in database.'
            : 'Invalid Unique ID or password.',
          code: isTargetAdmin ? 'ADMIN_ACCOUNT_NOT_FOUND' : 'INVALID_CREDENTIALS',
          diagnostics: {
            stage: 'ACCOUNT_LOOKUP',
            target: cleanId,
            isAdminTarget: isTargetAdmin
          }
        });
      }

      if (user.status === 'SUSPENDED' || user.status === 'BANNED') {
        return res.status(403).json({
          success: false,
          error: user.status === 'BANNED' ? 'Account has been banned. Contact server administration.' : 'Account has been suspended. Contact server administration.',
          code: user.status === 'BANNED' ? 'ACCOUNT_BANNED' : 'ACCOUNT_SUSPENDED'
        });
      }

      const cleanPass = typeof password === 'string' ? password.trim() : '';
      const isValid = await verifyPassword(cleanPass, user.password_hash);

      if (!isValid) {
        return res.status(401).json({
          success: false,
          error: 'Invalid Unique ID or password.',
          code: 'INVALID_CREDENTIALS',
          diagnostics: {
            stage: 'PASSWORD_VERIFICATION',
            target: cleanId
          }
        });
      }

      const now = Date.now();
      db.prepare('UPDATE users SET last_login_at = ? WHERE id = ?').run(now, cleanId);

      // Create persistent session
      let rawToken, tokenHash, sessionId, expiresAt;
      try {
        const tokenObj = generateSessionToken();
        rawToken = tokenObj.rawToken;
        tokenHash = tokenObj.tokenHash;
        sessionId = generateId('sess');
        const expirationDays = parseInt(process.env.SESSION_EXPIRATION_DAYS || '30', 10);
        expiresAt = now + (expirationDays * 24 * 60 * 60 * 1000);
        const clientIp = req.socket?.remoteAddress || 'unknown';

        db.prepare(`
          INSERT INTO sessions (id, user_id, token_hash, device_info, ip_address, created_at, last_active_at, expires_at, is_revoked)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
        `).run(sessionId, cleanId, tokenHash, deviceInfo || 'TGZ App', clientIp, now, now, expiresAt);
      } catch (sessErr) {
        console.error('Session creation failed:', sessErr);
        return res.status(500).json({
          success: false,
          error: 'Failed to create active login session.',
          code: 'SESSION_CREATION_FAILED'
        });
      }

      const isHttps = req.headers['x-forwarded-proto'] === 'https' || !!process.env.VERCEL;
      const secureFlag = isHttps ? '; Secure' : '';
      const expirationDays = parseInt(process.env.SESSION_EXPIRATION_DAYS || '30', 10);
      res.setHeader('Set-Cookie', `tgz_session=${rawToken}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${expirationDays * 86400}${secureFlag}`);

      return res.json({
        success: true,
        message: 'Login successful',
        sessionToken: rawToken,
        sessionId,
        expiresAt,
        user: {
          id: user.id,
          displayName: user.display_name,
          role: user.role,
          mustChangePassword: (user.must_change_password === 1 || user.must_change_password === '1' || user.must_change_password === true),
          status: user.status,
          createdAt: user.created_at
        }
      });
    } catch (err) {
      console.error('Login error:', err);
      return res.status(500).json({
        success: false,
        error: 'Internal server error during login.',
        code: 'INTERNAL_SERVER_ERROR'
      });
    }
  });

  // 3. LOGOUT: Explicitly terminate active session
  router.post('/api/auth/logout', (req, res) => {
    if (req.session) {
      db.prepare('UPDATE sessions SET is_revoked = 1 WHERE id = ?').run(req.session.id);
    }
    const isHttps = req.headers['x-forwarded-proto'] === 'https' || !!process.env.VERCEL;
    const secureFlag = isHttps ? '; Secure' : '';
    res.setHeader('Set-Cookie', `tgz_session=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0${secureFlag}`);
    return res.json({ success: true, message: 'Logged out successfully.' });
  });

  // 4. CURRENT USER INFO
  router.get('/api/auth/me', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Not authenticated.' });
    }

    // Refresh last_active_at timestamp for session
    const now = Date.now();
    if (req.session) {
      db.prepare('UPDATE sessions SET last_active_at = ? WHERE id = ?').run(now, req.session.id);
    }

    return res.json({
      user: {
        id: req.user.id,
        displayName: req.user.display_name,
        role: req.user.role,
        mustChangePassword: (req.user.must_change_password === 1 || req.user.must_change_password === '1' || req.user.must_change_password === true),
        status: req.user.status,
        createdAt: req.user.created_at,
        minecraftUuid: req.user.minecraft_uuid,
        minecraftUsername: req.user.minecraft_username
      },
      session: req.session ? {
        id: req.session.id,
        createdAt: req.session.created_at,
        expiresAt: req.session.expires_at,
        deviceInfo: req.session.device_info
      } : null
    });
  });

  // 5. CHANGE PASSWORD (Authenticated users may ONLY change their own password)
  router.post('/api/auth/change-password', async (req, res) => {
    try {
      if (!req.user) {
        return res.status(401).json({
          success: false,
          error: 'Authentication required. Please log in to change your password.',
          code: 'UNAUTHENTICATED'
        });
      }

      const { userId, targetUserId, username, currentPassword, newPassword, confirmNewPassword } = req.body || {};

      // Enforce strict ownership: reject attempts to target another user's account
      const requestedTarget = (targetUserId || userId || username || (typeof req.body?.id === 'string' ? req.body.id : '')).trim().toLowerCase();
      if (requestedTarget && requestedTarget !== req.user.id.toLowerCase()) {
        return res.status(403).json({
          success: false,
          error: 'Permission denied. You can only change your own password.',
          code: 'FORBIDDEN_PASSWORD_TARGET'
        });
      }

      // Always derive the target user from the verified authenticated identity
      const user = db.prepare('SELECT id, display_name, password_hash, role, must_change_password, status FROM users WHERE id = ?').get(req.user.id);
      if (!user) {
        return res.status(404).json({
          success: false,
          error: 'Authenticated user record not found in database.',
          code: 'USER_NOT_FOUND'
        });
      }

      if (!currentPassword || typeof currentPassword !== 'string') {
        return res.status(400).json({ success: false, error: 'Current password is required.' });
      }

      if (!newPassword || typeof newPassword !== 'string' || newPassword.length < 8) {
        return res.status(400).json({ success: false, error: 'New password must be at least 8 characters long.' });
      }

      if (newPassword !== confirmNewPassword) {
        return res.status(400).json({ success: false, error: 'New passwords do not match.' });
      }

      const isCurrentValid = await verifyPassword(currentPassword.trim(), user.password_hash);
      if (!isCurrentValid) {
        return res.status(401).json({ success: false, error: 'Current password is incorrect.', code: 'INVALID_CURRENT_PASSWORD' });
      }

      if (newPassword === currentPassword) {
        return res.status(400).json({ success: false, error: 'New password must be different from current password.' });
      }

      const newHash = await hashPassword(newPassword);
      const updateResult = db.prepare('UPDATE users SET password_hash = ?, must_change_password = 0 WHERE id = ?')
        .run(newHash, user.id);

      if (!updateResult || updateResult.changes !== 1) {
        return res.status(500).json({
          success: false,
          error: 'Database update failed. Password was not changed.',
          code: 'DB_UPDATE_FAILED'
        });
      }

      // Confirm the new hash is actually persisted in the database before claiming success
      const verifiedRow = db.prepare('SELECT password_hash FROM users WHERE id = ?').get(user.id);
      if (!verifiedRow || verifiedRow.password_hash !== newHash) {
        return res.status(500).json({
          success: false,
          error: 'Database verification failed after password update.',
          code: 'DB_VERIFICATION_FAILED'
        });
      }

      // Invalidate all active sessions so the user must log in again with the new password
      db.prepare('UPDATE sessions SET is_revoked = 1 WHERE user_id = ?').run(user.id);

      const isHttps = req.headers['x-forwarded-proto'] === 'https' || !!process.env.VERCEL;
      const secureFlag = isHttps ? '; Secure' : '';
      res.setHeader('Set-Cookie', `tgz_session=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0${secureFlag}`);

      return res.json({
        success: true,
        requiresReLogin: true,
        message: 'Password updated successfully. Please log in again with your new password.',
        user: {
          id: user.id,
          displayName: user.display_name,
          role: user.role,
          mustChangePassword: false
        }
      });
    } catch (err) {
      console.error('Change password error:', err);
      return res.status(500).json({
        success: false,
        error: 'Database or server error while updating password. Password was not changed.',
        code: 'INTERNAL_SERVER_ERROR'
      });
    }
  });

  // 5B. ADMIN-ONLY PASSWORD RESET (Separate protected endpoint with audit logging)
  router.post('/api/admin/users/:userId/reset-password', async (req, res) => {
    try {
      if (!req.user) {
        return res.status(401).json({ success: false, error: 'Authentication required.' });
      }
      if (req.user.role !== 'ADMIN') {
        return res.status(403).json({ success: false, error: 'Admin access required to reset another user\'s password.' });
      }

      const targetId = (req.params?.userId || '').trim().toLowerCase();
      if (!targetId) {
        return res.status(400).json({ success: false, error: 'Target user ID is required.' });
      }

      const targetUser = db.prepare('SELECT id, role FROM users WHERE id = ?').get(targetId);
      if (!targetUser) {
        return res.status(404).json({ success: false, error: 'Target user not found.' });
      }

      const { newPassword, confirmNewPassword } = req.body || {};
      if (!newPassword || typeof newPassword !== 'string' || newPassword.length < 8) {
        return res.status(400).json({ success: false, error: 'New password must be at least 8 characters long.' });
      }
      if (newPassword !== confirmNewPassword) {
        return res.status(400).json({ success: false, error: 'New passwords do not match.' });
      }

      const newHash = await hashPassword(newPassword);
      const updateResult = db.prepare('UPDATE users SET password_hash = ?, must_change_password = 0 WHERE id = ?')
        .run(newHash, targetUser.id);

      if (!updateResult || updateResult.changes !== 1) {
        return res.status(500).json({ success: false, error: 'Database update failed. Password was not reset.' });
      }

      const verifiedRow = db.prepare('SELECT password_hash FROM users WHERE id = ?').get(targetUser.id);
      if (!verifiedRow || verifiedRow.password_hash !== newHash) {
        return res.status(500).json({ success: false, error: 'Database verification failed after password reset.' });
      }

      // Revoke target user's active sessions
      db.prepare('UPDATE sessions SET is_revoked = 1 WHERE user_id = ?').run(targetUser.id);

      // Record in audit log
      const now = Date.now();
      const clientIp = req.socket?.remoteAddress || 'unknown';
      db.prepare(`
        INSERT INTO audit_logs (id, actor_id, actor_role, action, target_user_id, details, ip_address, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      `).run(
        generateId('audit'),
        req.user.id,
        req.user.role,
        'ADMIN_PASSWORD_RESET',
        targetUser.id,
        `Admin ${req.user.id} reset password for ${targetUser.id}`,
        clientIp,
        now
      );

      return res.json({
        success: true,
        message: `Password reset successfully for ${targetUser.id}.`
      });
    } catch (err) {
      console.error('Admin password reset error:', err);
      return res.status(500).json({ success: false, error: 'Database or server error during password reset.' });
    }
  });

  // 6. LIST ACTIVE SESSIONS
  router.get('/api/auth/sessions', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const now = Date.now();
    let sessions;

    // Admin can view all active sessions across application
    if (req.user.role === 'ADMIN' && req.query.all === 'true') {
      sessions = db.prepare(`
        SELECT s.id, s.user_id, u.display_name, u.role, s.device_info, s.ip_address,
               s.created_at, s.last_active_at, s.expires_at, s.is_revoked
        FROM sessions s
        JOIN users u ON s.user_id = u.id
        WHERE s.is_revoked = 0 AND s.expires_at > ?
        ORDER BY s.last_active_at DESC
      `).all(now);
    } else {
      // Users view their own active sessions
      sessions = db.prepare(`
        SELECT id, user_id, device_info, ip_address, created_at, last_active_at, expires_at, is_revoked
        FROM sessions
        WHERE user_id = ? AND is_revoked = 0 AND expires_at > ?
        ORDER BY last_active_at DESC
      `).all(req.user.id, now);
    }

    return res.json({
      sessions: sessions.map(s => ({
        ...s,
        isCurrent: req.session ? s.id === req.session.id : false
      }))
    });
  });

  // 7. REVOKE SESSION
  router.post('/api/auth/sessions/revoke', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { sessionId } = req.body || {};
    if (!sessionId) {
      return res.status(400).json({ error: 'Session ID is required.' });
    }

    const targetSession = db.prepare('SELECT id, user_id FROM sessions WHERE id = ?').get(sessionId);
    if (!targetSession) {
      return res.status(404).json({ error: 'Session not found.' });
    }

    // Only owner or Admin can revoke session
    if (targetSession.user_id !== req.user.id && req.user.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Permission denied to revoke this session.' });
    }

    db.prepare('UPDATE sessions SET is_revoked = 1 WHERE id = ?').run(sessionId);

    return res.json({ success: true, message: 'Session revoked successfully.' });
  });
}

module.exports = setupAuthRoutes;
