const { generateId } = require('../crypto-utils');

function setupGroupRoutes(router, db) {
  function formatGroup(g) {
    if (!g) return null;
    const count = typeof g.member_count === 'number' ? g.member_count : (typeof g.memberCount === 'number' ? g.memberCount : 1);
    return {
      ...g,
      ownerId: g.owner_id || g.ownerId,
      owner_id: g.owner_id || g.ownerId,
      ownerName: g.owner_name || g.ownerName,
      owner_name: g.owner_name || g.ownerName,
      memberCount: count,
      member_count: count,
      createdAt: g.created_at || g.createdAt,
      created_at: g.created_at || g.createdAt,
      updatedAt: g.updated_at || g.updatedAt,
      updated_at: g.updated_at || g.updatedAt,
      unreadCount: g.unread_count || 0,
      unread_count: g.unread_count || 0,
      latestMessageTime: g.last_message_time || null,
      latestMessage: g.last_message ? {
        content: g.last_message,
        senderId: g.last_sender_id,
        sender_id: g.last_sender_id,
        senderName: g.last_sender_name,
        sender_name: g.last_sender_name,
        createdAt: g.last_message_time,
        created_at: g.last_message_time
      } : null
    };
  }

  function formatGroupMessage(m) {
    if (!m) return null;
    return {
      id: m.id,
      groupId: m.group_id || m.groupId,
      group_id: m.group_id || m.groupId,
      senderId: m.sender_id || m.senderId,
      sender_id: m.sender_id || m.senderId,
      senderName: m.sender_name || m.senderName || '',
      sender_name: m.sender_name || m.senderName || '',
      senderRole: m.sender_role || m.senderRole || 'MEMBER',
      sender_role: m.sender_role || m.senderRole || 'MEMBER',
      senderAvatar: m.sender_avatar || m.senderAvatar || 'player',
      sender_avatar: m.sender_avatar || m.senderAvatar || 'player',
      content: m.content,
      createdAt: m.created_at || m.createdAt,
      created_at: m.created_at || m.createdAt
    };
  }

  // 1. LIST GROUPS FOR CURRENT USER (OR ALL FOR ADMIN)
  router.get('/api/groups', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const currentUserId = req.user.id;
    const isAdmin = req.user.role === 'ADMIN';

    let groups;
    if (isAdmin) {
      // Admin can see all groups
      groups = db.prepare(`
        SELECT g.id, g.name, g.description, g.avatar, g.owner_id, g.created_at, g.updated_at,
               u.display_name AS owner_name,
               (SELECT COUNT(*) FROM group_members WHERE group_id = g.id) AS member_count,
               (SELECT role FROM group_members WHERE group_id = g.id AND user_id = ?) AS my_role,
               (SELECT content FROM group_messages WHERE group_id = g.id ORDER BY created_at DESC LIMIT 1) AS last_message,
               (SELECT created_at FROM group_messages WHERE group_id = g.id ORDER BY created_at DESC LIMIT 1) AS last_message_time,
               (SELECT sender_id FROM group_messages WHERE group_id = g.id ORDER BY created_at DESC LIMIT 1) AS last_sender_id,
               (SELECT u2.display_name FROM group_messages gm2 JOIN users u2 ON gm2.sender_id = u2.id WHERE gm2.group_id = g.id ORDER BY gm2.created_at DESC LIMIT 1) AS last_sender_name,
               (SELECT COUNT(*) FROM group_messages gm3 WHERE gm3.group_id = g.id AND gm3.created_at > COALESCE((SELECT last_read_at FROM group_message_reads WHERE group_id = g.id AND user_id = ?), 0)) AS unread_count
        FROM groups g
        JOIN users u ON g.owner_id = u.id
        ORDER BY g.updated_at DESC
      `).all(currentUserId, currentUserId);
    } else {
      // Members see groups they belong to
      groups = db.prepare(`
        SELECT g.id, g.name, g.description, g.avatar, g.owner_id, g.created_at, g.updated_at,
               u.display_name AS owner_name,
               (SELECT COUNT(*) FROM group_members WHERE group_id = g.id) AS member_count,
               gm.role AS my_role,
               (SELECT content FROM group_messages WHERE group_id = g.id ORDER BY created_at DESC LIMIT 1) AS last_message,
               (SELECT created_at FROM group_messages WHERE group_id = g.id ORDER BY created_at DESC LIMIT 1) AS last_message_time,
               (SELECT sender_id FROM group_messages WHERE group_id = g.id ORDER BY created_at DESC LIMIT 1) AS last_sender_id,
               (SELECT u2.display_name FROM group_messages gm2 JOIN users u2 ON gm2.sender_id = u2.id WHERE gm2.group_id = g.id ORDER BY gm2.created_at DESC LIMIT 1) AS last_sender_name,
               (SELECT COUNT(*) FROM group_messages gm3 WHERE gm3.group_id = g.id AND gm3.created_at > COALESCE((SELECT last_read_at FROM group_message_reads WHERE group_id = g.id AND user_id = ?), 0)) AS unread_count
        FROM groups g
        JOIN group_members gm ON g.id = gm.group_id AND gm.user_id = ?
        JOIN users u ON g.owner_id = u.id
        ORDER BY g.updated_at DESC
      `).all(currentUserId, currentUserId);
    }

    return res.json({ groups: groups.map(formatGroup) });
  });

  // 2. CREATE A NEW GROUP
  router.post('/api/groups', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { name, description, avatar, member_ids, memberIds } = req.body || {};
    const candidateMembers = (Array.isArray(member_ids) ? member_ids : (Array.isArray(memberIds) ? memberIds : []));

    if (!name || typeof name !== 'string' || name.trim().length < 2 || name.trim().length > 50) {
      return res.status(400).json({ error: 'Group name must be between 2 and 50 characters.' });
    }

    const cleanName = name.trim();
    const cleanDesc = typeof description === 'string' ? description.trim().slice(0, 300) : '';
    const cleanAvatar = typeof avatar === 'string' && avatar.trim() ? avatar.trim() : '👥';

    const groupId = generateId('grp');
    const now = Date.now();

    db.prepare(`
      INSERT INTO groups (id, name, description, avatar, owner_id, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(groupId, cleanName, cleanDesc, cleanAvatar, req.user.id, now, now);

    // Creator is owner
    db.prepare(`
      INSERT INTO group_members (group_id, user_id, role, joined_at)
      VALUES (?, ?, 'OWNER', ?)
    `).run(groupId, req.user.id, now);

    // Add selected members if provided
    for (const mId of candidateMembers) {
      if (!mId || typeof mId !== 'string') continue;
      const cleanMId = mId.trim().toLowerCase();
      if (cleanMId === req.user.id.toLowerCase()) continue;

      const target = db.prepare("SELECT id, status FROM users WHERE id = ?").get(cleanMId);
      if (target && target.status !== 'SUSPENDED' && target.status !== 'BANNED') {
        try {
          db.prepare(`
            INSERT INTO group_members (group_id, user_id, role, joined_at)
            VALUES (?, ?, 'MEMBER', ?)
          `).run(groupId, cleanMId, now);
        } catch (e) {}
      }
    }

    // Initialize read state for creator
    try {
      db.prepare(`
        INSERT INTO group_message_reads (group_id, user_id, last_read_at)
        VALUES (?, ?, ?)
      `).run(groupId, req.user.id, now);
    } catch (e) {}

    const createdGroup = db.prepare(`
      SELECT g.id, g.name, g.description, g.avatar, g.owner_id, g.created_at, g.updated_at,
             u.display_name AS owner_name,
             (SELECT COUNT(*) FROM group_members WHERE group_id = g.id) AS member_count,
             'OWNER' AS my_role
      FROM groups g
      JOIN users u ON g.owner_id = u.id
      WHERE g.id = ?
    `).get(groupId);

    return res.status(201).json({
      success: true,
      group: formatGroup(createdGroup)
    });
  });

  // 3. GET GROUP DETAILS & PARTICIPANTS
  router.get('/api/groups/:groupId', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { groupId } = req.params;
    const group = db.prepare(`
      SELECT g.id, g.name, g.description, g.avatar, g.owner_id, g.created_at, g.updated_at,
             u.display_name AS owner_name
      FROM groups g
      JOIN users u ON g.owner_id = u.id
      WHERE g.id = ?
    `).get(groupId);

    if (!group) {
      return res.status(404).json({ error: 'Group not found.' });
    }

    const membership = db.prepare('SELECT role FROM group_members WHERE group_id = ? AND user_id = ?')
      .get(groupId, req.user.id);

    const isAdmin = req.user.role === 'ADMIN';
    if (!membership && !isAdmin) {
      return res.status(403).json({ error: 'You are not a member of this group.' });
    }

    const members = db.prepare(`
      SELECT u.id, u.display_name, u.status,
             COALESCE(u.avatar, 'player') AS avatar,
             gm.role, gm.joined_at,
             (SELECT COUNT(*) FROM sessions s WHERE s.user_id = u.id AND s.is_revoked = 0 AND s.expires_at > ? AND s.last_active_at > ?) > 0 AS is_online
      FROM group_members gm
      JOIN users u ON gm.user_id = u.id
      WHERE gm.group_id = ?
      ORDER BY CASE gm.role WHEN 'OWNER' THEN 1 ELSE 2 END, gm.joined_at ASC
    `).all(Date.now(), Date.now() - 300000, groupId);

    return res.json({
      group: formatGroup({
        ...group,
        member_count: members.length,
        my_role: membership?.role || (isAdmin ? 'ADMIN' : 'NONE')
      }),
      members
    });
  });

  // 4. GET GROUP MESSAGES
  router.get('/api/groups/:groupId/messages', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { groupId } = req.params;
    const membership = db.prepare('SELECT role FROM group_members WHERE group_id = ? AND user_id = ?')
      .get(groupId, req.user.id);

    const isAdmin = req.user.role === 'ADMIN';
    if (!membership && !isAdmin) {
      return res.status(403).json({ error: 'You are not a member of this group.' });
    }

    const now = Date.now();
    // Update last_read_at
    try {
      const existingRead = db.prepare('SELECT last_read_at FROM group_message_reads WHERE group_id = ? AND user_id = ?').get(groupId, req.user.id);
      if (existingRead) {
        db.prepare('UPDATE group_message_reads SET last_read_at = ? WHERE group_id = ? AND user_id = ?').run(now, groupId, req.user.id);
      } else {
        db.prepare('INSERT INTO group_message_reads (group_id, user_id, last_read_at) VALUES (?, ?, ?)').run(groupId, req.user.id, now);
      }
    } catch (e) {}

    const messages = db.prepare(`
      SELECT gm.id, gm.group_id, gm.sender_id, gm.content, gm.created_at,
             u.display_name AS sender_name, u.role AS sender_role,
             COALESCE(u.avatar, 'player') AS sender_avatar
      FROM group_messages gm
      JOIN users u ON gm.sender_id = u.id
      WHERE gm.group_id = ?
      ORDER BY gm.created_at ASC
      LIMIT 150
    `).all(groupId);

    return res.json({ messages: messages.map(formatGroupMessage) });
  });

  // 5. SEND MESSAGE IN GROUP
  router.post('/api/groups/:groupId/messages', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { groupId } = req.params;
    const { content } = req.body || {};

    if (!content || typeof content !== 'string' || !content.trim()) {
      return res.status(400).json({ error: 'Message content cannot be empty.' });
    }

    const cleanContent = content.trim().slice(0, 2000);

    const membership = db.prepare('SELECT role FROM group_members WHERE group_id = ? AND user_id = ?')
      .get(groupId, req.user.id);

    const isAdmin = req.user.role === 'ADMIN';
    if (!membership && !isAdmin) {
      return res.status(403).json({ error: 'You are not a member of this group.' });
    }

    const msgId = generateId('gmsg');
    const now = Date.now();

    db.prepare(`
      INSERT INTO group_messages (id, group_id, sender_id, content, created_at)
      VALUES (?, ?, ?, ?, ?)
    `).run(msgId, groupId, req.user.id, cleanContent, now);

    // Update group updated_at
    db.prepare('UPDATE groups SET updated_at = ? WHERE id = ?').run(now, groupId);

    // Update read state for sender
    try {
      const existingRead = db.prepare('SELECT last_read_at FROM group_message_reads WHERE group_id = ? AND user_id = ?').get(groupId, req.user.id);
      if (existingRead) {
        db.prepare('UPDATE group_message_reads SET last_read_at = ? WHERE group_id = ? AND user_id = ?').run(now, groupId, req.user.id);
      } else {
        db.prepare('INSERT INTO group_message_reads (group_id, user_id, last_read_at) VALUES (?, ?, ?)').run(groupId, req.user.id, now);
      }
    } catch (e) {}

    const rawMsg = {
      id: msgId,
      group_id: groupId,
      sender_id: req.user.id,
      sender_name: req.user.display_name,
      sender_role: req.user.role,
      sender_avatar: req.user.avatar || 'player',
      content: cleanContent,
      created_at: now
    };

    return res.status(201).json({
      success: true,
      message: formatGroupMessage(rawMsg)
    });
  });

  // 6. UPDATE GROUP SETTINGS (OWNER OR ADMIN)
  router.put('/api/groups/:groupId', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { groupId } = req.params;
    const group = db.prepare('SELECT id, owner_id FROM groups WHERE id = ?').get(groupId);
    if (!group) {
      return res.status(404).json({ error: 'Group not found.' });
    }

    const isOwner = group.owner_id === req.user.id;
    const isAdmin = req.user.role === 'ADMIN';
    if (!isOwner && !isAdmin) {
      return res.status(403).json({ error: 'Only the group owner or administrator can edit this group.' });
    }

    const { name, description, avatar } = req.body || {};
    const now = Date.now();

    if (name) {
      const cleanName = name.trim().slice(0, 50);
      if (cleanName.length >= 2) {
        db.prepare('UPDATE groups SET name = ?, updated_at = ? WHERE id = ?').run(cleanName, now, groupId);
      }
    }

    if (description !== undefined) {
      const cleanDesc = typeof description === 'string' ? description.trim().slice(0, 300) : '';
      db.prepare('UPDATE groups SET description = ?, updated_at = ? WHERE id = ?').run(cleanDesc, now, groupId);
    }

    if (avatar) {
      db.prepare('UPDATE groups SET avatar = ?, updated_at = ? WHERE id = ?').run(avatar.trim(), now, groupId);
    }

    const updatedGroup = db.prepare(`
      SELECT g.id, g.name, g.description, g.avatar, g.owner_id, g.created_at, g.updated_at,
             u.display_name AS owner_name,
             (SELECT COUNT(*) FROM group_members WHERE group_id = g.id) AS member_count,
             (SELECT role FROM group_members WHERE group_id = g.id AND user_id = ?) AS my_role
      FROM groups g
      JOIN users u ON g.owner_id = u.id
      WHERE g.id = ?
    `).get(req.user.id, groupId);

    return res.json({
      success: true,
      group: formatGroup(updatedGroup),
      message: 'Group updated successfully.'
    });
  });

  // 7. INVITE / ADD MEMBERS TO GROUP (OWNER OR ADMIN)
  router.post('/api/groups/:groupId/members', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { groupId } = req.params;
    const group = db.prepare('SELECT id, owner_id FROM groups WHERE id = ?').get(groupId);
    if (!group) {
      return res.status(404).json({ error: 'Group not found.' });
    }

    const isOwner = group.owner_id === req.user.id;
    const isAdmin = req.user.role === 'ADMIN';
    if (!isOwner && !isAdmin) {
      return res.status(403).json({ error: 'Only the group owner or administrator can invite members.' });
    }

    const { user_ids, userId } = req.body || {};
    const targetIds = Array.isArray(user_ids) ? user_ids : (userId ? [userId] : []);

    if (!targetIds.length) {
      return res.status(400).json({ error: 'No member IDs specified.' });
    }

    const now = Date.now();
    let addedCount = 0;

    for (const rawId of targetIds) {
      if (!rawId || typeof rawId !== 'string') continue;
      const cleanId = rawId.trim().toLowerCase();
      const user = db.prepare("SELECT id, status FROM users WHERE id = ?").get(cleanId);
      if (user && user.status !== 'SUSPENDED' && user.status !== 'BANNED') {
        const existing = db.prepare('SELECT user_id FROM group_members WHERE group_id = ? AND user_id = ?').get(groupId, cleanId);
        if (!existing) {
          db.prepare("INSERT INTO group_members (group_id, user_id, role, joined_at) VALUES (?, ?, 'MEMBER', ?)").run(groupId, cleanId, now);
          addedCount++;
        }
      }
    }

    db.prepare('UPDATE groups SET updated_at = ? WHERE id = ?').run(now, groupId);

    return res.json({
      success: true,
      message: `Successfully added ${addedCount} member(s).`
    });
  });

  // 8. REMOVE MEMBER / LEAVE GROUP
  router.delete('/api/groups/:groupId/members/:userId', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { groupId, userId } = req.params;
    const cleanUserId = userId.trim().toLowerCase();

    const group = db.prepare('SELECT id, owner_id FROM groups WHERE id = ?').get(groupId);
    if (!group) {
      return res.status(404).json({ error: 'Group not found.' });
    }

    const isSelf = req.user.id.toLowerCase() === cleanUserId;
    const isOwner = group.owner_id.toLowerCase() === req.user.id.toLowerCase();
    const isAdmin = req.user.role === 'ADMIN';

    if (!isSelf && !isOwner && !isAdmin) {
      return res.status(403).json({ error: 'You do not have permission to remove this member.' });
    }

    if (isSelf && isOwner) {
      const memberCount = db.prepare('SELECT COUNT(*) AS cnt FROM group_members WHERE group_id = ?').get(groupId)?.cnt || 0;
      if (memberCount > 1) {
        return res.status(400).json({ error: 'Owner must transfer group ownership before leaving.' });
      }
      // If owner is only member, delete group
      db.prepare('DELETE FROM groups WHERE id = ?').run(groupId);
      return res.json({ success: true, message: 'Group deleted as owner left.' });
    }

    db.prepare('DELETE FROM group_members WHERE group_id = ? AND user_id = ?').run(groupId, cleanUserId);
    db.prepare('UPDATE groups SET updated_at = ? WHERE id = ?').run(Date.now(), groupId);

    return res.json({
      success: true,
      message: isSelf ? 'You have left the group.' : `User '${cleanUserId}' was removed from the group.`
    });
  });

  // 9. TRANSFER OWNERSHIP (OWNER ONLY)
  router.post('/api/groups/:groupId/transfer', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { groupId } = req.params;
    const { new_owner_id, newOwnerId } = req.body || {};
    const targetNewOwner = newOwnerId || new_owner_id;

    if (!targetNewOwner || typeof targetNewOwner !== 'string') {
      return res.status(400).json({ error: 'New owner ID is required.' });
    }

    const cleanNewOwner = targetNewOwner.trim().toLowerCase();

    const group = db.prepare('SELECT id, owner_id FROM groups WHERE id = ?').get(groupId);
    if (!group) {
      return res.status(404).json({ error: 'Group not found.' });
    }

    const isOwner = group.owner_id.toLowerCase() === req.user.id.toLowerCase();
    const isAdmin = req.user.role === 'ADMIN';
    if (!isOwner && !isAdmin) {
      return res.status(403).json({ error: 'Only the group owner or administrator can transfer ownership.' });
    }

    const targetMembership = db.prepare('SELECT role FROM group_members WHERE group_id = ? AND user_id = ?').get(groupId, cleanNewOwner);
    if (!targetMembership) {
      return res.status(400).json({ error: 'Target user must already be a member of the group.' });
    }

    const now = Date.now();
    db.prepare('UPDATE groups SET owner_id = ?, updated_at = ? WHERE id = ?').run(cleanNewOwner, now, groupId);
    db.prepare("UPDATE group_members SET role = 'OWNER' WHERE group_id = ? AND user_id = ?").run(groupId, cleanNewOwner);
    db.prepare("UPDATE group_members SET role = 'MEMBER' WHERE group_id = ? AND user_id = ?").run(groupId, req.user.id);

    const updatedGroup = db.prepare(`
      SELECT g.id, g.name, g.description, g.avatar, g.owner_id, g.created_at, g.updated_at,
             u.display_name AS owner_name,
             (SELECT COUNT(*) FROM group_members WHERE group_id = g.id) AS member_count,
             (SELECT role FROM group_members WHERE group_id = g.id AND user_id = ?) AS my_role
      FROM groups g
      JOIN users u ON g.owner_id = u.id
      WHERE g.id = ?
    `).get(req.user.id, groupId);

    return res.json({
      success: true,
      group: formatGroup(updatedGroup),
      message: `Ownership successfully transferred to '${cleanNewOwner}'.`
    });
  });

  // 10. DELETE GROUP (OWNER OR ADMIN)
  router.delete('/api/groups/:groupId', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { groupId } = req.params;
    const group = db.prepare('SELECT id, owner_id FROM groups WHERE id = ?').get(groupId);
    if (!group) {
      return res.status(404).json({ error: 'Group not found.' });
    }

    const isOwner = group.owner_id.toLowerCase() === req.user.id.toLowerCase();
    const isAdmin = req.user.role === 'ADMIN';
    if (!isOwner && !isAdmin) {
      return res.status(403).json({ error: 'Only the group owner or administrator can delete this group.' });
    }

    db.prepare('DELETE FROM group_messages WHERE group_id = ?').run(groupId);
    db.prepare('DELETE FROM group_members WHERE group_id = ?').run(groupId);
    db.prepare('DELETE FROM group_message_reads WHERE group_id = ?').run(groupId);
    db.prepare('DELETE FROM groups WHERE id = ?').run(groupId);

    return res.json({
      success: true,
      message: 'Group deleted successfully.'
    });
  });
}

module.exports = setupGroupRoutes;
