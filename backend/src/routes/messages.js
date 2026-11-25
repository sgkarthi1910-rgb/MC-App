const { generateId } = require('../crypto-utils');

function setupMessageRoutes(router, db) {
  // Helper to ensure canonical conversation id
  function getOrCreateConversation(userA, userB) {
    const [u1, u2] = [userA, userB].sort();
    let conv = db.prepare('SELECT id FROM conversations WHERE user1_id = ? AND user2_id = ?').get(u1, u2);
    if (!conv) {
      const convId = generateId('conv');
      const now = Date.now();
      db.prepare('INSERT INTO conversations (id, user1_id, user2_id, updated_at) VALUES (?, ?, ?, ?)').run(convId, u1, u2, now);
      return convId;
    }
    return conv.id;
  }

  // 1. LIST CONVERSATIONS FOR CURRENT USER
  router.get('/api/messages/conversations', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const currentUserId = req.user.id;
    const conversations = db.prepare(`
      SELECT c.id, c.updated_at,
             CASE WHEN c.user1_id = ? THEN c.user2_id ELSE c.user1_id END AS partner_id,
             u.display_name AS partner_name,
             u.role AS partner_role,
             u.status AS partner_status,
             (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id LIMIT 1) AS partner_team,
             (SELECT content FROM messages WHERE conversation_id = c.id ORDER BY created_at DESC LIMIT 1) AS last_message,
             (SELECT created_at FROM messages WHERE conversation_id = c.id ORDER BY created_at DESC LIMIT 1) AS last_message_time,
             (SELECT sender_id FROM messages WHERE conversation_id = c.id ORDER BY created_at DESC LIMIT 1) AS last_sender_id,
             (SELECT COUNT(*) FROM messages WHERE conversation_id = c.id AND recipient_id = ? AND is_read = 0) AS unread_count
      FROM conversations c
      JOIN users u ON u.id = (CASE WHEN c.user1_id = ? THEN c.user2_id ELSE c.user1_id END)
      WHERE c.user1_id = ? OR c.user2_id = ?
      ORDER BY c.updated_at DESC
    `).all(currentUserId, currentUserId, currentUserId, currentUserId, currentUserId);

    return res.json({ conversations });
  });

  // 1B. SEARCH USERS TO START A NEW CONVERSATION
  router.get('/api/messages/users/search', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const query = ((req.query && req.query.q) || '').trim().toLowerCase();
    const currentUserId = req.user.id;

    let users;
    if (!query) {
      // Return recent active members up to 20
      users = db.prepare(`
        SELECT u.id, u.display_name, u.role, u.status,
               (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id LIMIT 1) AS team_name
        FROM users u
        WHERE u.id != ? AND u.status != 'SUSPENDED'
        ORDER BY u.created_at DESC
        LIMIT 20
      `).all(currentUserId);
    } else {
      const pattern = `%${query}%`;
      users = db.prepare(`
        SELECT u.id, u.display_name, u.role, u.status,
               (SELECT t.name FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = u.id LIMIT 1) AS team_name
        FROM users u
        WHERE u.id != ? AND u.status != 'SUSPENDED'
          AND (u.id LIKE ? OR LOWER(u.display_name) LIKE ?)
        ORDER BY u.display_name ASC
        LIMIT 25
      `).all(currentUserId, pattern, pattern);
    }

    // Also get current user's team to check if shared
    const myTeam = db.prepare('SELECT team_id FROM team_members WHERE user_id = ?').get(currentUserId);
    const myTeamId = myTeam ? myTeam.team_id : null;

    const formatted = users.map(u => {
      let isSharedTeam = false;
      if (myTeamId) {
        const partnerTeam = db.prepare('SELECT team_id FROM team_members WHERE user_id = ?').get(u.id);
        if (partnerTeam && partnerTeam.team_id === myTeamId) {
          isSharedTeam = true;
        }
      }
      return {
        id: u.id,
        displayName: u.display_name,
        role: u.role,
        status: u.status,
        teamName: u.team_name || null,
        isSharedTeam
      };
    });

    return res.json({ users: formatted });
  });

  // 2. GET MESSAGES IN CONVERSATION WITH SPECIFIC USER
  router.get('/api/messages/conversations/:withUserId', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const currentUserId = req.user.id;
    const withUserId = req.params.withUserId.toLowerCase().trim();

    const partner = db.prepare('SELECT id, display_name, role, status FROM users WHERE id = ?').get(withUserId);
    if (!partner) {
      return res.status(404).json({ error: 'User not found.' });
    }

    const convId = getOrCreateConversation(currentUserId, withUserId);

    // Mark unread messages as read
    db.prepare('UPDATE messages SET is_read = 1 WHERE conversation_id = ? AND recipient_id = ?')
      .run(convId, currentUserId);

    const messages = db.prepare(`
      SELECT m.id, m.sender_id, m.recipient_id, m.content, m.is_read, m.created_at,
             u.display_name AS sender_name, u.role AS sender_role
      FROM messages m
      JOIN users u ON m.sender_id = u.id
      WHERE m.conversation_id = ?
      ORDER BY m.created_at ASC
    `).all(convId);

    // Check shared team
    const myTeam = db.prepare('SELECT team_id FROM team_members WHERE user_id = ?').get(currentUserId);
    const partnerTeam = db.prepare(`
      SELECT tm.team_id, t.name AS team_name
      FROM team_members tm
      JOIN teams t ON tm.team_id = t.id
      WHERE tm.user_id = ?
    `).get(withUserId);

    const isSharedTeam = Boolean(myTeam && partnerTeam && myTeam.team_id === partnerTeam.team_id);

    return res.json({
      conversationId: convId,
      partner: {
        id: partner.id,
        displayName: partner.display_name,
        role: partner.role,
        status: partner.status,
        teamName: partnerTeam ? partnerTeam.team_name : null,
        isSharedTeam
      },
      messages
    });
  });

  // 3. SEND MESSAGE
  router.post('/api/messages', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    if (req.user.status === 'SUSPENDED') {
      return res.status(403).json({ error: 'Your account is suspended. You cannot send messages.' });
    }

    const { recipientId, content } = req.body || {};
    if (!recipientId || !content || typeof content !== 'string' || content.trim().length === 0) {
      return res.status(400).json({ error: 'Recipient and content are required.' });
    }

    const cleanRecipientId = recipientId.toLowerCase().trim();
    if (cleanRecipientId === req.user.id) {
      return res.status(400).json({ error: 'You cannot message yourself.' });
    }

    const recipient = db.prepare('SELECT id, display_name, role, status FROM users WHERE id = ?').get(cleanRecipientId);
    if (!recipient) {
      return res.status(404).json({ error: 'Recipient user does not exist.' });
    }

    if (recipient.status === 'SUSPENDED') {
      return res.status(400).json({ error: 'Cannot message a suspended account.' });
    }

    const convId = getOrCreateConversation(req.user.id, cleanRecipientId);
    const msgId = generateId('msg');
    const now = Date.now();
    const cleanContent = content.trim();

    db.prepare(`
      INSERT INTO messages (id, conversation_id, sender_id, recipient_id, content, is_read, created_at)
      VALUES (?, ?, ?, ?, ?, 0, ?)
    `).run(msgId, convId, req.user.id, cleanRecipientId, cleanContent, now);

    db.prepare('UPDATE conversations SET updated_at = ? WHERE id = ?').run(now, convId);

    return res.status(201).json({
      success: true,
      message: {
        id: msgId,
        conversationId: convId,
        senderId: req.user.id,
        senderName: req.user.display_name,
        senderRole: req.user.role,
        recipientId: cleanRecipientId,
        content: cleanContent,
        isRead: false,
        createdAt: now
      }
    });
  });

  // 4. UNREAD MESSAGE COUNT (for dashboard badge)
  router.get('/api/messages/unread-count', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const unread = db.prepare('SELECT COUNT(*) AS count FROM messages WHERE recipient_id = ? AND is_read = 0')
      .get(req.user.id);

    return res.json({ unreadCount: unread.count });
  });

  // 5. MODERATION: Staff message overview and deletion
  router.get('/api/messages/moderation/recent', (req, res) => {
    if (!req.user || (req.user.role !== 'ADMIN' && req.user.role !== 'MODERATOR')) {
      return res.status(403).json({ error: 'Staff access required.' });
    }

    const recent = db.prepare(`
      SELECT m.id, m.conversation_id, m.sender_id, m.recipient_id, m.content, m.created_at,
             s.display_name AS sender_name, s.role AS sender_role,
             r.display_name AS recipient_name, r.role AS recipient_role
      FROM messages m
      JOIN users s ON m.sender_id = s.id
      JOIN users r ON m.recipient_id = r.id
      ORDER BY m.created_at DESC
      LIMIT 100
    `).all();

    return res.json({ messages: recent });
  });

  // 5. DELETE A SINGLE MESSAGE (Sender can delete own message, or Staff can moderate)
  router.delete('/api/messages/:id', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const { id } = req.params;
    const msg = db.prepare('SELECT id, sender_id FROM messages WHERE id = ?').get(id);
    if (!msg) {
      return res.status(404).json({ error: 'Message not found.' });
    }

    const isSender = msg.sender_id === req.user.id;
    const isStaff = req.user.role === 'ADMIN' || req.user.role === 'MODERATOR';

    if (!isSender && !isStaff) {
      return res.status(403).json({ error: 'You can only delete your own messages unless you are staff.' });
    }

    db.prepare('DELETE FROM messages WHERE id = ?').run(id);
    return res.json({ success: true, message: 'Message deleted successfully.' });
  });

  // 6. DELETE / CLEAR AN ENTIRE CONVERSATION
  router.delete('/api/messages/conversations/:withUserId', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const currentUserId = req.user.id;
    const withUserId = req.params.withUserId.toLowerCase().trim();

    const [u1, u2] = [currentUserId, withUserId].sort();
    const conv = db.prepare('SELECT id FROM conversations WHERE user1_id = ? AND user2_id = ?').get(u1, u2);

    if (conv) {
      db.prepare('DELETE FROM messages WHERE conversation_id = ?').run(conv.id);
      db.prepare('DELETE FROM conversations WHERE id = ?').run(conv.id);
    }

    return res.json({ success: true, message: 'Conversation deleted.' });
  });
}

module.exports = setupMessageRoutes;
