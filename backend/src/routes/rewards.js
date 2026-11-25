const { generateId } = require('../crypto-utils');

function setupRewardRoutes(router, db) {
  // Helper to fetch authoritative playtime for a user
  function getUserPlaytimeMinutes(user) {
    if (!user) return 0;
    // Look up in minecraft_player_stats by uuid or username or user id
    const stats = db.prepare(`
      SELECT playtime_seconds FROM minecraft_player_stats
      WHERE (uuid IS NOT NULL AND uuid = ?)
         OR username = ?
         OR username = ?
    `).get(user.minecraft_uuid || '', user.minecraft_username || '', user.id);

    if (stats && stats.playtime_seconds > 0) {
      return Math.floor(stats.playtime_seconds / 60);
    }
    return 0;
  }

  // 1. GET ALL PLAYTIME CRATE REWARDS (EXACTLY 5 CRATES PER REQUIREMENT 7)
  // 1. Common Crate
  // 2. Monthly Crate
  // 3. Special Crate
  // 4. Arcane Crate
  // 5. Matrix Crate
  router.get('/api/rewards', (req, res) => {
    const userPlaytimeMinutes = req.user ? getUserPlaytimeMinutes(req.user) : 0;
    const userId = req.user?.id || null;

    const crates = db.prepare(`
      SELECT rc.reward_id, rc.name, rc.required_playtime_minutes, rc.description, rc.badge,
             cl.id AS claim_id, cl.claimed_at, cl.status AS claim_status
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
    `).all(userId);

    const formattedRewards = crates.map(crate => {
      const isClaimed = Boolean(crate.claim_id && crate.claim_status === 'APPROVED');
      const reqMinutes = crate.required_playtime_minutes; // NULL if unconfigured

      let status = 'UNCONFIGURED';
      let progressPercent = 0;

      if (isClaimed) {
        status = 'CLAIMED';
        progressPercent = 100;
      } else if (reqMinutes === null || reqMinutes === undefined) {
        status = 'PENDING_OFFICIAL_REQUIREMENT';
        progressPercent = 0;
      } else if (userPlaytimeMinutes >= reqMinutes) {
        status = 'AVAILABLE';
        progressPercent = 100;
      } else {
        status = 'LOCKED';
        progressPercent = Math.min(100, Math.round((userPlaytimeMinutes / reqMinutes) * 100));
      }

      return {
        id: crate.reward_id,
        name: crate.name,
        description: crate.description,
        badge: crate.badge,
        requiredPlaytimeMinutes: reqMinutes,
        requiredPlaytimeFormatted: reqMinutes === null ? 'Awaiting Official Config' : `${reqMinutes} mins (${(reqMinutes / 60).toFixed(1)} hrs)`,
        currentPlaytimeMinutes: userPlaytimeMinutes,
        currentPlaytimeFormatted: `${userPlaytimeMinutes} mins (${(userPlaytimeMinutes / 60).toFixed(1)} hrs)`,
        progressPercent,
        status,
        isClaimed,
        claimInfo: crate.claim_id ? {
          claimId: crate.claim_id,
          claimedAt: crate.claimed_at,
          status: crate.claim_status
        } : null
      };
    });

    return res.json({
      userPlaytimeMinutes,
      rewards: formattedRewards
    });
  });

  // 2. CLAIM ELIGIBLE REWARD
  router.post('/api/rewards/:rewardId/claim', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required to claim rewards.' });
    }

    const { rewardId } = req.params;
    const allowed = ['common_crate', 'monthly_crate', 'special_crate', 'arcane_crate', 'matrix_crate'];
    if (!allowed.includes(rewardId)) {
      return res.status(400).json({ error: 'Invalid reward item.' });
    }

    const rewardConfig = db.prepare('SELECT * FROM reward_configs WHERE reward_id = ?').get(rewardId);
    if (!rewardConfig) {
      return res.status(404).json({ error: 'Reward not found.' });
    }

    if (rewardConfig.required_playtime_minutes === null) {
      return res.status(400).json({
        error: 'Playtime requirements for this crate have not been officially configured by the administrator yet.'
      });
    }

    // Check duplicate claim
    const existingClaim = db.prepare('SELECT * FROM reward_claims WHERE user_id = ? AND reward_id = ?')
      .get(req.user.id, rewardId);

    if (existingClaim) {
      return res.status(409).json({ error: 'You have already claimed this reward. Duplicate claims are prohibited.' });
    }

    // Check playtime eligibility
    const currentPlaytime = getUserPlaytimeMinutes(req.user);
    if (currentPlaytime < rewardConfig.required_playtime_minutes) {
      return res.status(403).json({
        error: `Insufficient playtime. Required: ${rewardConfig.required_playtime_minutes} minutes, current: ${currentPlaytime} minutes.`
      });
    }

    const claimId = generateId('claim');
    const now = Date.now();

    db.prepare(`
      INSERT INTO reward_claims (id, user_id, reward_id, playtime_at_claim, claimed_at, status)
      VALUES (?, ?, ?, ?, ?, 'APPROVED')
    `).run(claimId, req.user.id, rewardId, currentPlaytime, now);

    return res.status(201).json({
      success: true,
      message: `Successfully claimed ${rewardConfig.name}! Your crate key voucher has been recorded.`,
      claim: {
        id: claimId,
        rewardId,
        rewardName: rewardConfig.name,
        claimedAt: now,
        playtimeAtClaim: currentPlaytime
      }
    });
  });

  // 3. USER CLAIM HISTORY
  router.get('/api/rewards/history', (req, res) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const history = db.prepare(`
      SELECT cl.id, cl.reward_id, rc.name AS reward_name, rc.badge, cl.playtime_at_claim,
             cl.claimed_at, cl.status, cl.reviewed_by, cl.reviewed_at
      FROM reward_claims cl
      JOIN reward_configs rc ON cl.reward_id = rc.reward_id
      WHERE cl.user_id = ?
      ORDER BY cl.claimed_at DESC
    `).all(req.user.id);

    return res.json({ history });
  });

  // 4. ADMIN & MODERATOR: REVIEW REWARD CLAIMS
  router.get('/api/rewards/admin/claims', (req, res) => {
    if (!req.user || (req.user.role !== 'ADMIN' && req.user.role !== 'MODERATOR')) {
      return res.status(403).json({ error: 'Staff access required.' });
    }

    const claims = db.prepare(`
      SELECT cl.id, cl.user_id, u.display_name, u.role AS user_role, cl.reward_id, rc.name AS reward_name,
             cl.playtime_at_claim, cl.claimed_at, cl.status, cl.reviewed_by, cl.reviewed_at
      FROM reward_claims cl
      JOIN users u ON cl.user_id = u.id
      JOIN reward_configs rc ON cl.reward_id = rc.reward_id
      ORDER BY cl.claimed_at DESC
    `).all();

    return res.json({ claims });
  });

  // 5. ADMIN & MODERATOR: UPDATE CLAIM STATUS (e.g., Revoke, Approve)
  router.put('/api/rewards/admin/claims/:claimId', (req, res) => {
    if (!req.user || (req.user.role !== 'ADMIN' && req.user.role !== 'MODERATOR')) {
      return res.status(403).json({ error: 'Staff access required.' });
    }

    const { claimId } = req.params;
    const { status } = req.body || {};
    if (!status || !['APPROVED', 'PENDING_REVIEW', 'REVOKED'].includes(status)) {
      return res.status(400).json({ error: 'Valid status is required: APPROVED, PENDING_REVIEW, REVOKED.' });
    }

    const now = Date.now();
    const updated = db.prepare(`
      UPDATE reward_claims
      SET status = ?, reviewed_by = ?, reviewed_at = ?
      WHERE id = ?
    `).run(status, req.user.id, now, claimId);

    if (updated.changes === 0) {
      return res.status(404).json({ error: 'Claim record not found.' });
    }

    return res.json({ success: true, message: `Claim status updated to ${status}.` });
  });

  // 6. ADMIN CONFIGURATION FOR REWARD PLAYTIME REQUIREMENTS
  router.get('/api/rewards/admin/config', (req, res) => {
    if (!req.user || req.user.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Admin access required.' });
    }

    const configs = db.prepare(`
      SELECT reward_id, name, required_playtime_minutes, description, badge
      FROM reward_configs
      ORDER BY 
        CASE reward_id
          WHEN 'common_crate' THEN 1
          WHEN 'monthly_crate' THEN 2
          WHEN 'special_crate' THEN 3
          WHEN 'arcane_crate' THEN 4
          WHEN 'matrix_crate' THEN 5
          ELSE 6
        END
    `).all();

    return res.json({ configs });
  });

  router.put('/api/rewards/admin/config', (req, res) => {
    if (!req.user || req.user.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Admin access required.' });
    }

    const { rewardId, requiredPlaytimeMinutes } = req.body || {};
    if (!rewardId) {
      return res.status(400).json({ error: 'rewardId is required.' });
    }

    const val = (requiredPlaytimeMinutes === null || requiredPlaytimeMinutes === undefined || requiredPlaytimeMinutes === '')
      ? null
      : parseInt(requiredPlaytimeMinutes, 10);

    if (val !== null && (isNaN(val) || val < 0)) {
      return res.status(400).json({ error: 'Required playtime must be a non-negative number of minutes or null.' });
    }

    db.prepare('UPDATE reward_configs SET required_playtime_minutes = ? WHERE reward_id = ?')
      .run(val, rewardId);

    return res.json({ success: true, message: `Playtime requirement for ${rewardId} updated to ${val} minutes.` });
  });
}

module.exports = setupRewardRoutes;
