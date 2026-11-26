const { generateId } = require('../crypto-utils');

function setupTeamRoutes(router, db) {
  // 1. GET CURRENT USER'S TEAM
  router.get('/api/teams/my-team', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const membership = db.prepare(`
      SELECT tm.team_id, tm.role AS member_role, tm.joined_at,
             t.name AS team_name, t.description AS team_description, t.logo AS team_logo, t.status AS team_status,
             t.owner_id, t.created_at AS team_created_at,
             o.display_name AS owner_name
      FROM team_members tm
      JOIN teams t ON tm.team_id = t.id
      JOIN users o ON t.owner_id = o.id
      WHERE tm.user_id = ?
    `).get(req.user.id);

    if (!membership) {
      return res.json({ inTeam: false, team: null });
    }

    const members = db.prepare(`
      SELECT tm.user_id, tm.role, tm.joined_at, u.display_name, u.role AS app_role, u.status
      FROM team_members tm
      JOIN users u ON tm.user_id = u.id
      WHERE tm.team_id = ?
      ORDER BY CASE tm.role WHEN 'OWNER' THEN 1 ELSE 2 END, tm.joined_at ASC
    `).all(membership.team_id);

    // Pending invitations sent by this team (visible to owner)
    let pendingInvitations = [];
    if (membership.member_role === 'OWNER') {
      pendingInvitations = db.prepare(`
        SELECT ti.id, ti.invitee_id, ti.created_at, u.display_name AS invitee_name
        FROM team_invitations ti
        JOIN users u ON ti.invitee_id = u.id
        WHERE ti.team_id = ? AND ti.status = 'PENDING'
        ORDER BY ti.created_at DESC
      `).all(membership.team_id);
    }

    let totalKills = 0;
    let totalPlaytimeMinutes = 0;
    for (const m of members) {
      const stats = db.prepare('SELECT kills, playtime_seconds FROM minecraft_player_stats WHERE username = ? COLLATE NOCASE OR uuid = ?').get(m.user_id, m.user_id);
      if (stats) {
        totalKills += (stats.kills || 0);
        totalPlaytimeMinutes += Math.round((stats.playtime_seconds || 0) / 60);
      }
    }

    return res.json({
      inTeam: true,
      team: {
        id: membership.team_id,
        name: membership.team_name,
        description: membership.team_description,
        logo: membership.team_logo || 'shield',
        status: membership.team_status || 'ACTIVE',
        ownerId: membership.owner_id,
        ownerName: membership.owner_name,
        createdAt: membership.team_created_at,
        isCurrentUserOwner: membership.member_role === 'OWNER',
        currentUserRole: membership.member_role,
        members,
        pendingInvitations,
        totalKills,
        totalPlaytimeMinutes
      }
    });
  });

  // 1B. GET PUBLIC / BROWSE ALL TEAMS
  router.get('/api/teams', (req, res) => {
    const teams = db.prepare(`
      SELECT t.id, t.name, t.description, t.logo, t.status, t.owner_id, t.created_at,
             o.display_name AS owner_name,
             (SELECT COUNT(*) FROM team_members WHERE team_id = t.id) AS member_count,
             (SELECT COUNT(*) FROM team_invitations WHERE team_id = t.id AND status = 'PENDING') AS pending_invite_count
      FROM teams t
      JOIN users o ON t.owner_id = o.id
      ORDER BY t.created_at DESC
    `).all();

    const formattedTeams = teams.map(t => {
      const members = db.prepare(`
        SELECT u.display_name FROM team_members tm
        JOIN users u ON tm.user_id = u.id
        WHERE tm.team_id = ?
        ORDER BY CASE tm.role WHEN 'OWNER' THEN 1 ELSE 2 END
        LIMIT 4
      `).all(t.id).map(m => m.display_name);

      return {
        id: t.id,
        name: t.name,
        description: t.description,
        logo: t.logo || 'shield',
        status: t.status || 'ACTIVE',
        ownerId: t.owner_id,
        ownerName: t.owner_name,
        createdAt: t.created_at,
        memberCount: t.member_count,
        membersPreview: members,
        pendingInviteCount: t.pending_invite_count
      };
    });

    return res.json({ teams: formattedTeams });
  });

  // 1C. GET SINGLE TEAM DETAILS (For View Details modal/panel)
  router.get('/api/teams/details/:teamId', (req, res) => {
    const { teamId } = req.params;
    const t = db.prepare(`
      SELECT t.id, t.name, t.description, t.logo, t.status, t.owner_id, t.created_at,
             o.display_name AS owner_name
      FROM teams t
      JOIN users o ON t.owner_id = o.id
      WHERE t.id = ?
    `).get(teamId);

    if (!t) {
      return res.status(404).json({ error: 'Team not found.' });
    }

    const members = db.prepare(`
      SELECT tm.user_id, tm.role, tm.joined_at, u.display_name, u.role AS app_role, u.status
      FROM team_members tm
      JOIN users u ON tm.user_id = u.id
      WHERE tm.team_id = ?
      ORDER BY CASE tm.role WHEN 'OWNER' THEN 1 ELSE 2 END, tm.joined_at ASC
    `).all(teamId);

    let isCurrentUserOwner = false;
    let currentUserRole = 'NONE';
    if (req.user) {
      if (t.owner_id === req.user.id) {
        isCurrentUserOwner = true;
        currentUserRole = 'OWNER';
      } else {
        const mem = members.find(m => m.user_id === req.user.id);
        if (mem) currentUserRole = mem.role;
      }
    }

    let pendingInvitations = [];
    if (req.user && (isCurrentUserOwner || req.user.role === 'ADMIN' || req.user.role === 'MODERATOR')) {
      pendingInvitations = db.prepare(`
        SELECT ti.id, ti.invitee_id, ti.created_at, u.display_name AS invitee_name
        FROM team_invitations ti
        JOIN users u ON ti.invitee_id = u.id
        WHERE ti.team_id = ? AND ti.status = 'PENDING'
        ORDER BY ti.created_at DESC
      `).all(teamId);
    }

    let totalKills = 0;
    let totalPlaytimeMinutes = 0;
    for (const m of members) {
      const stats = db.prepare('SELECT kills, playtime_seconds FROM minecraft_player_stats WHERE username = ? COLLATE NOCASE OR uuid = ?').get(m.user_id, m.user_id);
      if (stats) {
        totalKills += (stats.kills || 0);
        totalPlaytimeMinutes += Math.round((stats.playtime_seconds || 0) / 60);
      }
    }

    return res.json({
      team: {
        id: t.id,
        name: t.name,
        description: t.description,
        logo: t.logo || 'shield',
        status: t.status || 'ACTIVE',
        ownerId: t.owner_id,
        ownerName: t.owner_name,
        createdAt: t.created_at,
        isCurrentUserOwner,
        currentUserRole,
        members,
        pendingInvitations,
        totalKills,
        totalPlaytimeMinutes
      }
    });
  });

  // 1D. SEARCH REGISTERED USERS TO INVITE
  router.get('/api/teams/search-members', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const q = (req.query.q || '').trim().toLowerCase();
    if (q.length < 1) {
      return res.json({ users: [] });
    }

    const users = db.prepare(`
      SELECT u.id, u.display_name, u.role, u.status,
             (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id) AS team_name
      FROM users u
      WHERE (u.id LIKE ? OR u.display_name LIKE ?) AND u.status = 'ACTIVE'
      ORDER BY u.display_name ASC
      LIMIT 15
    `).all(`%${q}%`, `%${q}%`);

    return res.json({
      users: users.map(u => ({
        id: u.id,
        displayName: u.display_name,
        role: u.role,
        status: u.status,
        alreadyInTeam: !!u.team_name,
        teamName: u.team_name
      }))
    });
  });

  // 2. CREATE TEAM
  router.post('/api/teams', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    if (req.user.status === 'SUSPENDED') {
      return res.status(403).json({ error: 'Account suspended.' });
    }

    // Check if user already in a team
    const inTeam = db.prepare('SELECT team_id FROM team_members WHERE user_id = ?').get(req.user.id);
    if (inTeam) {
      return res.status(400).json({ error: 'You are already in a team. Leave your current team before creating a new one.' });
    }

    const { name, description, logo, status } = req.body || {};
    if (!name || typeof name !== 'string' || name.trim().length < 3 || name.trim().length > 24) {
      return res.status(400).json({ error: 'Team name must be between 3 and 24 characters.' });
    }

    const cleanName = name.trim();
    const existing = db.prepare('SELECT id FROM teams WHERE name = ? COLLATE NOCASE').get(cleanName);
    if (existing) {
      return res.status(409).json({ error: 'A team with this name already exists.' });
    }

    const teamId = generateId('team');
    const now = Date.now();
    const cleanDesc = description && typeof description === 'string' ? description.trim() : '';
    const cleanLogo = logo && typeof logo === 'string' ? logo.trim() : 'shield';
    const cleanStatus = status && ['ACTIVE', 'RECRUITING', 'FULL', 'PRIVATE'].includes(status) ? status : 'ACTIVE';

    db.prepare(`
      INSERT INTO teams (id, name, description, logo, status, owner_id, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(teamId, cleanName, cleanDesc, cleanLogo, cleanStatus, req.user.id, now);

    db.prepare(`
      INSERT INTO team_members (team_id, user_id, role, joined_at)
      VALUES (?, ?, 'OWNER', ?)
    `).run(teamId, req.user.id, now);

    return res.status(201).json({
      success: true,
      message: `Team '${cleanName}' created successfully.`,
      teamId,
      team: {
        id: teamId,
        name: cleanName,
        description: cleanDesc,
        logo: cleanLogo,
        ownerId: req.user.id
      }
    });
  });

  // 2B. EDIT TEAM DETAILS (Owner only)
  router.put('/api/teams/my-team', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const team = db.prepare(`
      SELECT t.id FROM teams t
      JOIN team_members tm ON t.id = tm.team_id
      WHERE tm.user_id = ? AND tm.role = 'OWNER'
    `).get(req.user.id);

    if (!team) {
      return res.status(403).json({ error: 'Only team owners can edit team details.' });
    }

    const { description, logo, status } = req.body || {};
    const updates = [];
    const params = [];

    if (description !== undefined) {
      updates.push('description = ?');
      params.push(typeof description === 'string' ? description.trim() : '');
    }
    if (logo !== undefined) {
      updates.push('logo = ?');
      params.push(logo && typeof logo === 'string' ? logo.trim() : 'shield');
    }
    if (status !== undefined && ['ACTIVE', 'RECRUITING', 'FULL', 'PRIVATE'].includes(status)) {
      updates.push('status = ?');
      params.push(status);
    }

    if (updates.length === 0) {
      return res.status(400).json({ error: 'No valid fields provided to update.' });
    }

    params.push(team.id);
    db.prepare(`UPDATE teams SET ${updates.join(', ')} WHERE id = ?`).run(...params);

    const updated = db.prepare('SELECT id, name, description, logo, status, owner_id, created_at FROM teams WHERE id = ?').get(team.id);

    return res.json({
      success: true,
      message: 'Team details updated successfully.',
      team: updated
    });
  });

  // 2C. DISBAND / DELETE TEAM (Owner only)
  router.delete('/api/teams/my-team', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const team = db.prepare(`
      SELECT t.id, t.name FROM teams t
      JOIN team_members tm ON t.id = tm.team_id
      WHERE tm.user_id = ? AND tm.role = 'OWNER'
    `).get(req.user.id);

    if (!team) {
      return res.status(403).json({ error: 'Only team owners can disband their team.' });
    }

    db.prepare('DELETE FROM teams WHERE id = ?').run(team.id);
    return res.json({ success: true, message: `Team '${team.name}' has been disbanded.` });
  });

  // 3. INVITE MEMBER USING UNIQUE ID (Owner only)
  router.post('/api/teams/invite', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { inviteeId } = req.body || {};
    if (!inviteeId || typeof inviteeId !== 'string') {
      return res.status(400).json({ error: 'Invitee Unique ID is required.' });
    }

    const cleanInviteeId = inviteeId.toLowerCase().trim();

    // Verify current user owns a team
    const team = db.prepare(`
      SELECT t.id, t.name FROM teams t
      JOIN team_members tm ON t.id = tm.team_id
      WHERE tm.user_id = ? AND tm.role = 'OWNER'
    `).get(req.user.id);

    if (!team) {
      return res.status(403).json({ error: 'Only team owners can send invitations.' });
    }

    // Verify invitee exists
    const invitee = db.prepare('SELECT id, display_name, status FROM users WHERE id = ?').get(cleanInviteeId);
    if (!invitee) {
      return res.status(404).json({ error: `User '${cleanInviteeId}' not found.` });
    }

    if (invitee.status === 'SUSPENDED') {
      return res.status(400).json({ error: 'Cannot invite a suspended user.' });
    }

    // Verify invitee not already in a team
    const alreadyInTeam = db.prepare('SELECT team_id FROM team_members WHERE user_id = ?').get(cleanInviteeId);
    if (alreadyInTeam) {
      return res.status(400).json({ error: `User '${cleanInviteeId}' is already a member of a team.` });
    }

    // Verify no pending invitation exists
    const existingInvite = db.prepare(`
      SELECT id FROM team_invitations
      WHERE team_id = ? AND invitee_id = ? AND status = 'PENDING'
    `).get(team.id, cleanInviteeId);

    if (existingInvite) {
      return res.status(409).json({ error: 'An invitation is already pending for this user.' });
    }

    const inviteId = generateId('inv');
    const now = Date.now();

    db.prepare(`
      INSERT INTO team_invitations (id, team_id, inviter_id, invitee_id, status, created_at)
      VALUES (?, ?, ?, ?, 'PENDING', ?)
    `).run(inviteId, team.id, req.user.id, cleanInviteeId, now);

    return res.status(201).json({
      success: true,
      message: `Invitation sent to ${invitee.display_name} (${cleanInviteeId}).`,
      inviteId
    });
  });

  // 4. GET PENDING INVITATIONS FOR CURRENT USER
  router.get('/api/teams/invitations', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const invitations = db.prepare(`
      SELECT ti.id, ti.team_id, ti.inviter_id, ti.created_at, ti.status,
             t.name AS team_name, t.description AS team_description,
             u.display_name AS inviter_name
      FROM team_invitations ti
      JOIN teams t ON ti.team_id = t.id
      JOIN users u ON ti.inviter_id = u.id
      WHERE ti.invitee_id = ? AND ti.status = 'PENDING'
      ORDER BY ti.created_at DESC
    `).all(req.user.id);

    return res.json({ invitations });
  });

  // 5. RESPOND TO INVITATION (Accept or Reject)
  router.post('/api/teams/invitations/:inviteId/respond', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { inviteId } = req.params;
    const { action } = req.body || {}; // 'ACCEPT' or 'REJECT'

    if (!action || !['ACCEPT', 'REJECT'].includes(action)) {
      return res.status(400).json({ error: 'Action must be ACCEPT or REJECT.' });
    }

    const invite = db.prepare(`
      SELECT * FROM team_invitations WHERE id = ? AND invitee_id = ? AND status = 'PENDING'
    `).get(inviteId, req.user.id);

    if (!invite) {
      return res.status(404).json({ error: 'Pending invitation not found.' });
    }

    if (action === 'REJECT') {
      db.prepare("UPDATE team_invitations SET status = 'REJECTED' WHERE id = ?").run(inviteId);
      return res.json({ success: true, message: 'Invitation declined.' });
    }

    // Action is ACCEPT: Check user isn't in a team already
    const inTeam = db.prepare('SELECT team_id FROM team_members WHERE user_id = ?').get(req.user.id);
    if (inTeam) {
      return res.status(400).json({ error: 'You are already in a team. Leave your current team first.' });
    }

    const now = Date.now();
    db.prepare("UPDATE team_invitations SET status = 'ACCEPTED' WHERE id = ?").run(inviteId);
    // Cancel any other pending invites for this user
    db.prepare("UPDATE team_invitations SET status = 'CANCELLED' WHERE invitee_id = ? AND status = 'PENDING'").run(req.user.id);

    db.prepare(`
      INSERT INTO team_members (team_id, user_id, role, joined_at)
      VALUES (?, ?, 'MEMBER', ?)
    `).run(invite.team_id, req.user.id, now);

    return res.json({ success: true, message: 'You have joined the team!' });
  });

  // 6. LEAVE TEAM
  router.post('/api/teams/leave', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const membership = db.prepare('SELECT team_id, role FROM team_members WHERE user_id = ?').get(req.user.id);
    if (!membership) {
      return res.status(400).json({ error: 'You are not in any team.' });
    }

    if (membership.role === 'OWNER') {
      const otherMembers = db.prepare('SELECT COUNT(*) AS cnt FROM team_members WHERE team_id = ? AND user_id != ?')
        .get(membership.team_id, req.user.id).cnt;
      if (otherMembers > 0) {
        return res.status(400).json({
          error: 'Team owners must transfer ownership before leaving, or disband the team if empty.'
        });
      }
      // If owner is the only member, disband the team
      db.prepare('DELETE FROM teams WHERE id = ?').run(membership.team_id);
      return res.json({ success: true, message: 'You disbanded your team.' });
    }

    db.prepare('DELETE FROM team_members WHERE user_id = ?').run(req.user.id);
    return res.json({ success: true, message: 'You have left the team.' });
  });

  // 7. REMOVE MEMBER (Owner only)
  router.post('/api/teams/remove-member', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { memberId } = req.body || {};
    if (!memberId) {
      return res.status(400).json({ error: 'Member ID is required.' });
    }

    const cleanMemberId = memberId.toLowerCase().trim();
    if (cleanMemberId === req.user.id) {
      return res.status(400).json({ error: 'Cannot remove yourself. Use leave team.' });
    }

    const team = db.prepare(`
      SELECT t.id FROM teams t
      JOIN team_members tm ON t.id = tm.team_id
      WHERE tm.user_id = ? AND tm.role = 'OWNER'
    `).get(req.user.id);

    if (!team) {
      return res.status(403).json({ error: 'Only team owners can remove members.' });
    }

    const deleted = db.prepare('DELETE FROM team_members WHERE team_id = ? AND user_id = ?')
      .run(team.id, cleanMemberId);

    if (deleted.changes === 0) {
      return res.status(404).json({ error: 'Member not found in your team.' });
    }

    return res.json({ success: true, message: `Member ${cleanMemberId} removed from team.` });
  });

  // 8. TRANSFER OWNERSHIP (Owner only)
  router.post('/api/teams/transfer-ownership', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { newOwnerId } = req.body || {};
    if (!newOwnerId) {
      return res.status(400).json({ error: 'newOwnerId is required.' });
    }

    const cleanNewOwnerId = newOwnerId.toLowerCase().trim();
    const team = db.prepare(`
      SELECT t.id FROM teams t
      JOIN team_members tm ON t.id = tm.team_id
      WHERE tm.user_id = ? AND tm.role = 'OWNER'
    `).get(req.user.id);

    if (!team) {
      return res.status(403).json({ error: 'Only team owners can transfer ownership.' });
    }

    const targetMember = db.prepare('SELECT user_id FROM team_members WHERE team_id = ? AND user_id = ?')
      .get(team.id, cleanNewOwnerId);

    if (!targetMember) {
      return res.status(400).json({ error: 'Target user must be a member of your team.' });
    }

    db.prepare('UPDATE teams SET owner_id = ? WHERE id = ?').run(cleanNewOwnerId, team.id);
    db.prepare("UPDATE team_members SET role = 'MEMBER' WHERE team_id = ? AND user_id = ?").run(team.id, req.user.id);
    db.prepare("UPDATE team_members SET role = 'OWNER' WHERE team_id = ? AND user_id = ?").run(team.id, cleanNewOwnerId);

    return res.json({ success: true, message: `Ownership transferred to ${cleanNewOwnerId}.` });
  });

  // 9. CANCEL INVITATION (Owner only)
  router.delete('/api/teams/invitations/:inviteId', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { inviteId } = req.params;
    const invite = db.prepare('SELECT ti.*, tm.role FROM team_invitations ti JOIN team_members tm ON ti.team_id = tm.team_id WHERE ti.id = ? AND tm.user_id = ?').get(inviteId, req.user.id);

    if (!invite || invite.role !== 'OWNER') {
      return res.status(403).json({ error: 'Permission denied to cancel this invitation.' });
    }

    db.prepare("UPDATE team_invitations SET status = 'CANCELLED' WHERE id = ?").run(inviteId);
    return res.json({ success: true, message: 'Invitation cancelled.' });
  });

  // 10. ADMIN: VIEW ALL TEAMS & DISBAND
  router.get('/api/teams/all', (req, res) => {
    if (!req.user || (req.user.role !== 'ADMIN' && req.user.role !== 'MODERATOR')) {
      return res.status(403).json({ error: 'Staff access required.' });
    }

    const allTeams = db.prepare(`
      SELECT t.id, t.name, t.description, t.owner_id, t.created_at,
             o.display_name AS owner_name,
             (SELECT COUNT(*) FROM team_members WHERE team_id = t.id) AS member_count
      FROM teams t
      JOIN users o ON t.owner_id = o.id
      ORDER BY t.created_at DESC
    `).all();

    return res.json({ teams: allTeams });
  });

  router.delete('/api/teams/:id', (req, res) => {
    if (!req.user || req.user.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Admin access required.' });
    }

    const { id } = req.params;
    const deleted = db.prepare('DELETE FROM teams WHERE id = ?').run(id);
    if (deleted.changes === 0) {
      return res.status(404).json({ error: 'Team not found.' });
    }

    return res.json({ success: true, message: 'Team disbanded by admin.' });
  });

  // 11. PLUGIN INGESTION: SYNC TEAMS FROM MINECRAFT SERVER
  router.post('/api/minecraft/report-teams', (req, res) => {
    const pluginApiKey = req.headers['x-plugin-key'];
    const expectedKey = db.prepare("SELECT value FROM app_settings WHERE key = 'minecraft_plugin_api_key'").get()?.value;

    if (!pluginApiKey || pluginApiKey !== expectedKey) {
      return res.status(401).json({ error: 'Unauthorized: Invalid X-Plugin-Key header.' });
    }

    const { teams } = req.body || {};
    if (!Array.isArray(teams)) {
      return res.status(400).json({ error: 'Body must contain a "teams" array.' });
    }

    const processed = syncServerTeams(db, teams);
    return res.json({ success: true, processedCount: processed, timestamp: Date.now() });
  });
}

function syncServerTeams(db, teams) {
  let count = 0;
  const now = Date.now();

  for (const t of teams) {
    if (t.name && t.owner) {
      const cleanName = t.name.trim();
      const cleanOwner = t.owner.trim().toLowerCase();

      // Ensure owner user exists in database
      const existingOwner = db.prepare('SELECT id FROM users WHERE id = ?').get(cleanOwner);
      if (!existingOwner) {
        db.prepare(`
          INSERT OR IGNORE INTO users (id, display_name, password_hash, role, must_change_password, status, created_at)
          VALUES (?, ?, 'ingame_synced_user', 'MEMBER', 1, 'ACTIVE', ?)
        `).run(cleanOwner, cleanOwner, now);
      }

      // Check existing team by name
      const existingTeam = db.prepare('SELECT id FROM teams WHERE name = ? COLLATE NOCASE').get(cleanName);
      const teamId = existingTeam ? existingTeam.id : generateId('team');

      if (!existingTeam) {
        db.prepare(`
          INSERT INTO teams (id, name, description, owner_id, created_at)
          VALUES (?, ?, ?, ?, ?)
        `).run(teamId, cleanName, t.description || 'Synced from Minecraft SMP', cleanOwner, now);
      } else {
        db.prepare(`
          UPDATE teams SET description = ?, owner_id = ? WHERE id = ?
        `).run(t.description || 'Synced from Minecraft SMP', cleanOwner, teamId);
      }

      // Assign owner as OWNER
      db.prepare(`
        INSERT INTO team_members (team_id, user_id, role, joined_at)
        VALUES (?, ?, 'OWNER', ?)
        ON CONFLICT(user_id) DO UPDATE SET team_id = excluded.team_id, role = 'OWNER'
      `).run(teamId, cleanOwner, now);

      // Assign members
      if (Array.isArray(t.members)) {
        for (const m of t.members) {
          const cleanMember = (typeof m === 'string' ? m : m.username || m.id || '').trim().toLowerCase();
          if (cleanMember && cleanMember !== cleanOwner) {
            db.prepare(`
              INSERT OR IGNORE INTO users (id, display_name, password_hash, role, must_change_password, status, created_at)
              VALUES (?, ?, 'ingame_synced_user', 'MEMBER', 1, 'ACTIVE', ?)
            `).run(cleanMember, cleanMember, now);

            db.prepare(`
              INSERT INTO team_members (team_id, user_id, role, joined_at)
              VALUES (?, ?, 'MEMBER', ?)
              ON CONFLICT(user_id) DO UPDATE SET team_id = excluded.team_id, role = 'MEMBER'
            `).run(teamId, cleanMember, now);
          }
        }
      }
      count++;
    }
  }
  return count;
}

module.exports = setupTeamRoutes;
module.exports.syncServerTeams = syncServerTeams;
