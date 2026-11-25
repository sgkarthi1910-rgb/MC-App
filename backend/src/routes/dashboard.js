const { queryMinecraftServer } = require('../minecraft-query');

function setupDashboardRoutes(router, db) {
  // 1. ROLE-BASED DASHBOARD SUMMARY
  router.get('/api/dashboard/summary', async (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const role = req.user.role;
    const now = Date.now();

    // Common server status
    const host = db.prepare("SELECT value FROM app_settings WHERE key = 'minecraft_server_host'").get()?.value || 'node2.vyroxsmp.online';
    const port = parseInt(db.prepare("SELECT value FROM app_settings WHERE key = 'minecraft_server_port'").get()?.value || '10010', 10);
    const mcStatus = await queryMinecraftServer(host, port);

    // Latest announcement for dashboard
    const latestAnnouncement = db.prepare(`
      SELECT id, title, content, author_name, category, created_at
      FROM announcements ORDER BY is_pinned DESC, created_at DESC LIMIT 1
    `).get() || null;

    if (role === 'ADMIN') {
      const totalMembers = db.prepare("SELECT COUNT(*) AS cnt FROM users WHERE role = 'MEMBER'").get().cnt;
      const totalModerators = db.prepare("SELECT COUNT(*) AS cnt FROM users WHERE role = 'MODERATOR'").get().cnt;
      const activeMembers = db.prepare("SELECT COUNT(*) AS cnt FROM users WHERE status = 'ACTIVE'").get().cnt;
      const activeSessions = db.prepare('SELECT COUNT(*) AS cnt FROM sessions WHERE is_revoked = 0 AND expires_at > ?').get(now).cnt;
      const totalTeams = db.prepare('SELECT COUNT(*) AS cnt FROM teams').get().cnt;
      const pendingClaims = db.prepare("SELECT COUNT(*) AS cnt FROM reward_claims WHERE status = 'PENDING_REVIEW'").get().cnt;
      const recentUsers = db.prepare(`
        SELECT id, display_name, role, status, created_at
        FROM users ORDER BY created_at DESC LIMIT 5
      `).all();

      return res.json({
        role: 'ADMIN',
        adminStats: {
          totalMembers,
          activeMembers,
          totalModerators,
          activeSessions,
          totalTeams,
          pendingClaims
        },
        recentRegistrations: recentUsers,
        serverStatus: mcStatus,
        latestAnnouncement
      });
    }

    if (role === 'MODERATOR') {
      const totalMembers = db.prepare("SELECT COUNT(*) AS cnt FROM users WHERE role = 'MEMBER'").get().cnt;
      const activeSessions = db.prepare('SELECT COUNT(*) AS cnt FROM sessions WHERE is_revoked = 0 AND expires_at > ?').get(now).cnt;
      const totalTeams = db.prepare('SELECT COUNT(*) AS cnt FROM teams').get().cnt;
      const pendingClaims = db.prepare("SELECT COUNT(*) AS cnt FROM reward_claims WHERE status = 'PENDING_REVIEW'").get().cnt;
      const recentMessages = db.prepare(`
        SELECT m.id, m.sender_id, m.recipient_id, m.content, m.created_at,
               s.display_name AS sender_name, r.display_name AS recipient_name
        FROM messages m
        JOIN users s ON m.sender_id = s.id
        JOIN users r ON m.recipient_id = r.id
        ORDER BY m.created_at DESC LIMIT 5
      `).all();

      return res.json({
        role: 'MODERATOR',
        moderatorStats: {
          totalMembers,
          activeSessions,
          totalTeams,
          pendingClaims
        },
        recentMessages,
        serverStatus: mcStatus,
        latestAnnouncement
      });
    }

    // Role is MEMBER
    const unreadMessages = db.prepare('SELECT COUNT(*) AS cnt FROM messages WHERE recipient_id = ? AND is_read = 0')
      .get(req.user.id).cnt;

    // Playtime stats
    const stats = db.prepare(`
      SELECT playtime_seconds, kills, deaths, hearts, coins
      FROM minecraft_player_stats
      WHERE (uuid IS NOT NULL AND uuid = ?) OR username = ? OR username = ?
    `).get(req.user.minecraft_uuid || '', req.user.minecraft_username || '', req.user.id);

    const playtimeSeconds = stats?.playtime_seconds || 0;
    const playtimeMinutes = Math.floor(playtimeSeconds / 60);

    // Reward progress for the 5 crates
    const crates = db.prepare(`
      SELECT rc.reward_id, rc.name, rc.required_playtime_minutes, rc.badge,
             cl.id AS claim_id, cl.status AS claim_status
      FROM reward_configs rc
      LEFT JOIN reward_claims cl ON rc.reward_id = cl.reward_id AND cl.user_id = ?
      WHERE rc.reward_id IN ('common_crate', 'monthly_crate', 'special_crate', 'arcane_crate', 'matrix_crate')
      ORDER BY 
        CASE rc.reward_id
          WHEN 'common_crate' THEN 1
          WHEN 'monthly_crate' THEN 2
          WHEN 'special_crate' THEN 3
          WHEN 'arcane_crate' THEN 4
          WHEN 'matrix_crate' THEN 5
          ELSE 6
        END
    `).all(req.user.id);

    const rewardProgress = crates.map(c => {
      const isClaimed = Boolean(c.claim_id && c.claim_status === 'APPROVED');
      const reqM = c.required_playtime_minutes;
      let status = 'UNCONFIGURED';
      let pct = 0;
      if (isClaimed) {
        status = 'CLAIMED';
        pct = 100;
      } else if (reqM === null) {
        status = 'AWAITING_CONFIG';
        pct = 0;
      } else if (playtimeMinutes >= reqM) {
        status = 'AVAILABLE';
        pct = 100;
      } else {
        status = 'LOCKED';
        pct = Math.min(100, Math.round((playtimeMinutes / reqM) * 100));
      }
      return {
        id: c.reward_id,
        name: c.name,
        badge: c.badge,
        status,
        progressPercent: pct
      };
    });

    // Team summary
    const team = db.prepare(`
      SELECT t.id, t.name, tm.role
      FROM team_members tm
      JOIN teams t ON tm.team_id = t.id
      WHERE tm.user_id = ?
    `).get(req.user.id) || null;

    // Leaderboard position
    let rank = null;
    if (playtimeSeconds > 0) {
      const higherCount = db.prepare('SELECT COUNT(*) AS cnt FROM minecraft_player_stats WHERE playtime_seconds > ?')
        .get(playtimeSeconds).cnt;
      rank = higherCount + 1;
    }

    return res.json({
      role: 'MEMBER',
      profile: {
        id: req.user.id,
        displayName: req.user.display_name,
        role: req.user.role,
        status: req.user.status,
        createdAt: req.user.created_at
      },
      serverStatus: mcStatus,
      playtime: {
        seconds: playtimeSeconds,
        minutes: playtimeMinutes,
        formatted: `${Math.floor(playtimeMinutes / 60)}h ${playtimeMinutes % 60}m`
      },
      kills: stats?.kills || 0,
      deaths: stats?.deaths || 0,
      hearts: stats?.hearts || 10,
      coins: stats?.coins || 0,
      leaderboardRank: rank,
      rewardProgress,
      latestAnnouncement,
      unreadMessagesCount: unreadMessages,
      team
    });
  });
}

module.exports = setupDashboardRoutes;
