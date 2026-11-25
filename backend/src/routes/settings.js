function setupSettingsRoutes(router, db) {
  // 1. MEMBER SETTINGS PROFILE
  router.get('/api/settings/member', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const sessionCount = db.prepare('SELECT COUNT(*) AS cnt FROM sessions WHERE user_id = ? AND is_revoked = 0')
      .get(req.user.id).cnt;

    return res.json({
      settings: {
        userId: req.user.id,
        displayName: req.user.display_name,
        role: req.user.role,
        status: req.user.status,
        createdAt: req.user.created_at,
        lastLoginAt: req.user.last_login_at,
        minecraftUuid: req.user.minecraft_uuid,
        minecraftUsername: req.user.minecraft_username,
        activeSessionsCount: sessionCount
      }
    });
  });

  // 2. ADMIN: GET APPLICATION SETTINGS
  router.get('/api/settings/admin', (req, res) => {
    if (!req.user || req.user.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Admin access required.' });
    }

    const rows = db.prepare('SELECT key, value FROM app_settings').all();
    const settings = {};
    for (const r of rows) {
      settings[r.key] = r.value;
    }

    return res.json({ settings });
  });

  // 3. ADMIN: UPDATE APPLICATION SETTINGS
  router.put('/api/settings/admin', (req, res) => {
    if (!req.user || req.user.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Admin access required.' });
    }

    const { settings } = req.body || {};
    if (!settings || typeof settings !== 'object') {
      return res.status(400).json({ error: 'Settings object is required.' });
    }

    const upsertStmt = db.prepare(`
      INSERT INTO app_settings (key, value) VALUES (?, ?)
      ON CONFLICT(key) DO UPDATE SET value = excluded.value
    `);

    for (const [key, value] of Object.entries(settings)) {
      if (typeof value === 'string' || typeof value === 'number') {
        upsertStmt.run(key, String(value));
      }
    }

    return res.json({ success: true, message: 'Settings updated successfully.' });
  });

  // 4. ADMIN: LIST USERS FOR ROLE MANAGEMENT
  router.get('/api/settings/admin/users', (req, res) => {
    if (!req.user || (req.user.role !== 'ADMIN' && req.user.role !== 'MODERATOR')) {
      return res.status(403).json({ error: 'Staff access required.' });
    }

    const users = db.prepare(`
      SELECT id, display_name, role, status, created_at, last_login_at,
             (SELECT COUNT(*) FROM sessions WHERE user_id = users.id AND is_revoked = 0) AS active_sessions
      FROM users
      ORDER BY 
        CASE role WHEN 'ADMIN' THEN 1 WHEN 'MODERATOR' THEN 2 ELSE 3 END,
        created_at DESC
    `).all();

    return res.json({ users });
  });

  // 5. ADMIN: PROMOTE / DEMOTE MODERATOR
  // Rule: Only admin can appoint or remove moderators.
  // Rule: Cannot create another admin, cannot modify admin permissions.
  router.post('/api/settings/admin/users/:userId/role', (req, res) => {
    if (!req.user || req.user.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Only admins can appoint or remove moderators.' });
    }

    const { userId } = req.params;
    const { role } = req.body || {};

    if (!role || !['MODERATOR', 'MEMBER'].includes(role)) {
      return res.status(400).json({ error: 'Target role must be MODERATOR or MEMBER.' });
    }

    const cleanUserId = userId.toLowerCase().trim();
    if (cleanUserId === req.user.id) {
      return res.status(400).json({ error: 'Cannot modify your own admin role.' });
    }

    const targetUser = db.prepare('SELECT id, role FROM users WHERE id = ?').get(cleanUserId);
    if (!targetUser) {
      return res.status(404).json({ error: 'User not found.' });
    }

    if (targetUser.role === 'ADMIN') {
      return res.status(400).json({ error: 'Admin accounts cannot be demoted through this endpoint.' });
    }

    db.prepare('UPDATE users SET role = ? WHERE id = ?').run(role, cleanUserId);

    return res.json({
      success: true,
      message: `User '${cleanUserId}' role changed to ${role}.`
    });
  });

  // 6. ADMIN: SUSPEND / REACTIVATE USER ACCOUNT
  router.post('/api/settings/admin/users/:userId/status', (req, res) => {
    if (!req.user || req.user.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Admin access required.' });
    }

    const { userId } = req.params;
    const { status } = req.body || {};

    if (!status || !['ACTIVE', 'SUSPENDED'].includes(status)) {
      return res.status(400).json({ error: 'Status must be ACTIVE or SUSPENDED.' });
    }

    const cleanUserId = userId.toLowerCase().trim();
    if (cleanUserId === req.user.id) {
      return res.status(400).json({ error: 'Cannot suspend your own admin account.' });
    }

    const targetUser = db.prepare('SELECT id, role FROM users WHERE id = ?').get(cleanUserId);
    if (!targetUser) {
      return res.status(404).json({ error: 'User not found.' });
    }

    if (targetUser.role === 'ADMIN') {
      return res.status(400).json({ error: 'Admin accounts cannot be suspended.' });
    }

    db.prepare('UPDATE users SET status = ? WHERE id = ?').run(status, cleanUserId);

    // If suspended, revoke all active sessions immediately
    if (status === 'SUSPENDED') {
      db.prepare('UPDATE sessions SET is_revoked = 1 WHERE user_id = ?').run(cleanUserId);
    }

    return res.json({
      success: true,
      message: `User '${cleanUserId}' status changed to ${status}.`
    });
  });
}

module.exports = setupSettingsRoutes;
