const { generateId } = require('../crypto-utils');

function setupAnnouncementRoutes(router, db) {
  // 1. LIST ANNOUNCEMENTS
  router.get('/api/announcements', (req, res) => {
    const userId = req.user?.id || null;
    const announcements = db.prepare(`
      SELECT a.id, a.title, a.content, a.author_id, a.author_name, a.category, a.is_pinned, a.created_at, a.updated_at,
             CASE WHEN r.read_at IS NOT NULL THEN 1 ELSE 0 END AS is_read
      FROM announcements a
      LEFT JOIN announcement_reads r ON a.id = r.announcement_id AND r.user_id = ?
      ORDER BY a.is_pinned DESC, a.created_at DESC
    `).all(userId);

    return res.json({ announcements });
  });

  // 2. LATEST ANNOUNCEMENT (for dashboard card)
  router.get('/api/announcements/latest', (req, res) => {
    const userId = req.user?.id || null;
    const announcement = db.prepare(`
      SELECT a.id, a.title, a.content, a.author_id, a.author_name, a.category, a.is_pinned, a.created_at, a.updated_at,
             CASE WHEN r.read_at IS NOT NULL THEN 1 ELSE 0 END AS is_read
      FROM announcements a
      LEFT JOIN announcement_reads r ON a.id = r.announcement_id AND r.user_id = ?
      ORDER BY a.is_pinned DESC, a.created_at DESC
      LIMIT 1
    `).get(userId);

    return res.json({ announcement: announcement || null });
  });

  // 3. ANNOUNCEMENT DETAIL & MARK READ
  router.get('/api/announcements/:id', (req, res) => {
    const { id } = req.params;
    const announcement = db.prepare(`
      SELECT id, title, content, author_id, author_name, category, is_pinned, created_at, updated_at
      FROM announcements WHERE id = ?
    `).get(id);

    if (!announcement) {
      return res.status(404).json({ error: 'Announcement not found.' });
    }

    // Mark as read if user is logged in
    if (req.user) {
      const now = Date.now();
      db.prepare(`
        INSERT OR IGNORE INTO announcement_reads (user_id, announcement_id, read_at)
        VALUES (?, ?, ?)
      `).run(req.user.id, id, now);
    }

    return res.json({ announcement });
  });

  // 4. CREATE ANNOUNCEMENT (Admin and authorized Moderators)
  router.post('/api/announcements', (req, res) => {
    if (!req.user || (req.user.role !== 'ADMIN' && req.user.role !== 'MODERATOR')) {
      return res.status(403).json({ error: 'Only staff can publish announcements.' });
    }

    const { title, content, category, isPinned } = req.body || {};
    if (!title || !content) {
      return res.status(400).json({ error: 'Title and content are required.' });
    }

    const id = generateId('ann');
    const now = Date.now();
    const cleanCategory = (category && typeof category === 'string') ? category.toUpperCase().trim() : 'GENERAL';
    const pinnedVal = isPinned ? 1 : 0;

    db.prepare(`
      INSERT INTO announcements (id, title, content, author_id, author_name, category, is_pinned, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(id, title.trim(), content.trim(), req.user.id, req.user.display_name, cleanCategory, pinnedVal, now, now);

    return res.status(201).json({
      success: true,
      announcement: {
        id,
        title: title.trim(),
        content: content.trim(),
        authorId: req.user.id,
        authorName: req.user.display_name,
        category: cleanCategory,
        isPinned: Boolean(pinnedVal),
        createdAt: now,
        updatedAt: now
      }
    });
  });

  // 5. UPDATE ANNOUNCEMENT (Admin and author/moderator)
  router.put('/api/announcements/:id', (req, res) => {
    if (!req.user || (req.user.role !== 'ADMIN' && req.user.role !== 'MODERATOR')) {
      return res.status(403).json({ error: 'Permission denied.' });
    }

    const { id } = req.params;
    const existing = db.prepare('SELECT id, author_id FROM announcements WHERE id = ?').get(id);
    if (!existing) {
      return res.status(404).json({ error: 'Announcement not found.' });
    }

    const { title, content, category, isPinned } = req.body || {};
    const now = Date.now();

    db.prepare(`
      UPDATE announcements
      SET title = COALESCE(?, title),
          content = COALESCE(?, content),
          category = COALESCE(?, category),
          is_pinned = COALESCE(?, is_pinned),
          updated_at = ?
      WHERE id = ?
    `).run(
      title ? title.trim() : null,
      content ? content.trim() : null,
      category ? category.toUpperCase().trim() : null,
      typeof isPinned === 'boolean' ? (isPinned ? 1 : 0) : null,
      now,
      id
    );

    return res.json({ success: true, message: 'Announcement updated.' });
  });

  // 6. DELETE ANNOUNCEMENT (Admin and authorized Moderators)
  router.delete('/api/announcements/:id', (req, res) => {
    if (!req.user || (req.user.role !== 'ADMIN' && req.user.role !== 'MODERATOR')) {
      return res.status(403).json({ error: 'Permission denied.' });
    }

    const { id } = req.params;
    const deleted = db.prepare('DELETE FROM announcements WHERE id = ?').run(id);

    if (deleted.changes === 0) {
      return res.status(404).json({ error: 'Announcement not found.' });
    }

    return res.json({ success: true, message: 'Announcement deleted.' });
  });
}

module.exports = setupAnnouncementRoutes;
