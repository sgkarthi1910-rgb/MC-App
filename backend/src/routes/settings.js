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

  // 4. ADMIN: LIST REGISTERED MEMBERS WITH SEARCH, FILTER, SORT & PAGINATION
  router.get('/api/settings/admin/users', (req, res) => {
    if (!req.user || (req.user.role !== 'ADMIN' && req.user.role !== 'MODERATOR')) {
      return res.status(403).json({ error: 'Staff access required.' });
    }

    const {
      search,
      role,
      status,
      sort = 'created_desc',
      page,
      limit
    } = req.query || {};

    const now = Date.now();
    const activeCutoff = now - (5 * 60 * 1000); // 5 minutes threshold for true online presence

    let baseQuery = `
      SELECT u.id, u.display_name, u.role, u.status, u.created_at, u.last_login_at,
             u.minecraft_username, u.minecraft_uuid,
             COALESCE(u.avatar, 'player') AS avatar,
             (SELECT COUNT(*) FROM sessions s WHERE s.user_id = u.id AND s.is_revoked = 0) AS active_sessions,
             (SELECT COUNT(*) FROM sessions s WHERE s.user_id = u.id AND s.is_revoked = 0 AND s.expires_at > ? AND s.last_active_at > ?) > 0 AS is_online,
             (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id LIMIT 1) AS team_name,
             (SELECT tm.team_id FROM team_members tm WHERE tm.user_id = u.id LIMIT 1) AS team_id,
             (SELECT COUNT(*) FROM reward_claims rc WHERE rc.user_id = u.id AND rc.status = 'APPROVED') AS claims_count
      FROM users u
      WHERE 1=1
    `;
    const params = [now, activeCutoff];

    if (search && search.trim()) {
      const q = `%${search.trim().toLowerCase()}%`;
      baseQuery += ` AND (LOWER(u.id) LIKE ? OR LOWER(u.display_name) LIKE ? OR LOWER(COALESCE(u.minecraft_username, '')) LIKE ?)`;
      params.push(q, q, q);
    }

    if (role && role !== 'ALL') {
      baseQuery += ` AND u.role = ?`;
      params.push(role.toUpperCase());
    }

    if (status && status !== 'ALL') {
      baseQuery += ` AND u.status = ?`;
      params.push(status.toUpperCase());
    }

    // Sort order
    if (sort === 'created_asc') {
      baseQuery += ` ORDER BY u.created_at ASC`;
    } else if (sort === 'last_login_desc') {
      baseQuery += ` ORDER BY u.last_login_at DESC NULLS LAST, u.created_at DESC`;
    } else if (sort === 'id_asc') {
      baseQuery += ` ORDER BY u.id ASC`;
    } else {
      // Default: admin first, then newest
      baseQuery += ` ORDER BY CASE u.role WHEN 'ADMIN' THEN 1 WHEN 'MODERATOR' THEN 2 ELSE 3 END, u.created_at DESC`;
    }

    const allMatching = db.prepare(baseQuery).all(...params);
    const total = allMatching.length;

    let users = allMatching;
    const pageNum = parseInt(page, 10);
    const limitNum = parseInt(limit, 10);

    if (pageNum > 0 && limitNum > 0) {
      const offset = (pageNum - 1) * limitNum;
      users = allMatching.slice(offset, offset + limitNum);
    }

    const pagination = {
      total,
      page: pageNum > 0 ? pageNum : 1,
      limit: limitNum > 0 ? limitNum : total,
      pages: limitNum > 0 ? Math.ceil(total / limitNum) : 1,
      hasMore: limitNum > 0 ? (pageNum * limitNum < total) : false
    };

    return res.json({
      users,
      pagination,
      total,
      page: pagination.page,
      limit: pagination.limit,
      totalPages: pagination.pages,
      hasMore: pagination.hasMore
    });
  });

  // 4B. ADMIN: GET COMPLETE REGISTERED MEMBER DETAILS
  router.get('/api/settings/admin/users/:userId/details', (req, res) => {
    if (!req.user || req.user.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Admin access required.' });
    }

    const { userId } = req.params;
    const cleanUserId = userId.toLowerCase().trim();

    const now = Date.now();
    const activeCutoff = now - (5 * 60 * 1000);

    const user = db.prepare(`
      SELECT u.id, u.display_name, u.role, u.status, u.created_at, u.last_login_at,
             u.minecraft_username, u.minecraft_uuid,
             COALESCE(u.avatar, 'player') AS avatar,
             (SELECT COUNT(*) FROM sessions s WHERE s.user_id = u.id AND s.is_revoked = 0) AS active_sessions,
             (SELECT COUNT(*) FROM sessions s WHERE s.user_id = u.id AND s.is_revoked = 0 AND s.expires_at > ? AND s.last_active_at > ?) > 0 AS is_online
      FROM users u
      WHERE u.id = ?
    `).get(now, activeCutoff, cleanUserId);

    if (!user) {
      return res.status(404).json({ error: 'User not found.' });
    }

    // Team information
    const team = db.prepare(`
      SELECT t.id, t.name, t.description, t.logo, tm.role AS member_role, tm.joined_at,
             (SELECT COUNT(*) FROM team_members WHERE team_id = t.id) AS member_count
      FROM team_members tm
      JOIN teams t ON tm.team_id = t.id
      WHERE tm.user_id = ?
    `).get(cleanUserId) || null;

    // Reward claims
    const rewardClaims = db.prepare(`
      SELECT rc.id, rc.reward_id, cfg.name AS reward_name, cfg.badge, rc.playtime_at_claim,
             rc.claimed_at, rc.status, rc.reviewed_by, rc.reviewed_at
      FROM reward_claims rc
      LEFT JOIN reward_configs cfg ON rc.reward_id = cfg.reward_id
      WHERE rc.user_id = ?
      ORDER BY rc.claimed_at DESC
    `).all(cleanUserId);

    // Activity stats
    const totalSessions = db.prepare('SELECT COUNT(*) AS cnt FROM sessions WHERE user_id = ?').get(cleanUserId)?.cnt || 0;
    const recentSessions = db.prepare(`
      SELECT id, device_info, ip_address, created_at, last_active_at, is_revoked
      FROM sessions WHERE user_id = ?
      ORDER BY last_active_at DESC LIMIT 5
    `).all(cleanUserId);

    const sentMessagesCount = db.prepare('SELECT COUNT(*) AS cnt FROM messages WHERE sender_id = ?').get(cleanUserId)?.cnt || 0;
    const groupMessagesSent = db.prepare('SELECT COUNT(*) AS cnt FROM group_messages WHERE sender_id = ?').get(cleanUserId)?.cnt || 0;
    const announcementsReadCount = db.prepare('SELECT COUNT(*) AS cnt FROM announcement_reads WHERE user_id = ?').get(cleanUserId)?.cnt || 0;

    // Minecraft telemetry if available
    const mcStats = db.prepare(`
      SELECT playtime_seconds, kills, deaths, hearts, coins, last_seen
      FROM minecraft_player_stats
      WHERE (uuid IS NOT NULL AND uuid = ?) OR username = ? OR username = ?
    `).get(user.minecraft_uuid || '', user.minecraft_username || '', user.id);

    const profile = {
      id: user.id,
      displayName: user.display_name,
      display_name: user.display_name,
      role: user.role,
      status: user.status,
      avatar: user.avatar,
      minecraftUsername: user.minecraft_username,
      minecraft_username: user.minecraft_username,
      createdAt: user.created_at,
      created_at: user.created_at,
      lastLoginAt: user.last_login_at,
      last_login_at: user.last_login_at,
      isOnline: Boolean(user.is_online),
      is_online: Boolean(user.is_online)
    };

    const teamDto = team ? {
      id: team.id,
      name: team.name,
      tag: team.tag || team.name.slice(0, 4).toUpperCase(),
      role: team.member_role || 'MEMBER',
      joinedAt: team.joined_at,
      joined_at: team.joined_at,
      description: team.description,
      logo: team.logo,
      memberCount: team.member_count,
      member_count: team.member_count
    } : null;

    const rewardsDto = {
      totalClaims: rewardClaims.length,
      total_claims: rewardClaims.length,
      lastClaimed: rewardClaims.length > 0 ? rewardClaims[0].claimed_at : null,
      last_claimed: rewardClaims.length > 0 ? rewardClaims[0].claimed_at : null,
      history: rewardClaims
    };

    const activityDto = {
      totalSessions,
      total_sessions: totalSessions,
      activeSessions: user.active_sessions || 0,
      active_sessions: user.active_sessions || 0,
      recentSessions,
      recent_sessions: recentSessions,
      directMessagesSent: sentMessagesCount,
      direct_messages_sent: sentMessagesCount,
      groupMessagesSent,
      group_messages_sent: groupMessagesSent,
      sentMessagesCount,
      announcementsReadCount,
      mcStats: mcStats || null
    };

    return res.json({
      user: profile,
      profile,
      team: teamDto,
      rewards: rewardsDto,
      rewardClaims,
      activity: activityDto
    });
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
      role,
      message: `User '${cleanUserId}' role changed to ${role}.`
    });
  });

  // 6. ADMIN: SUSPEND / REACTIVATE / BAN USER ACCOUNT
  router.post('/api/settings/admin/users/:userId/status', (req, res) => {
    if (!req.user || req.user.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Admin access required.' });
    }

    const { userId } = req.params;
    const { status } = req.body || {};

    if (!status || !['ACTIVE', 'SUSPENDED', 'BANNED'].includes(status)) {
      return res.status(400).json({ error: 'Status must be ACTIVE, SUSPENDED, or BANNED.' });
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

    // If suspended or banned, revoke all active sessions immediately
    if (status === 'SUSPENDED' || status === 'BANNED') {
      db.prepare('UPDATE sessions SET is_revoked = 1 WHERE user_id = ?').run(cleanUserId);
    }

    return res.json({
      success: true,
      status,
      message: `User '${cleanUserId}' status changed to ${status}.`
    });
  });
}

module.exports = setupSettingsRoutes;
