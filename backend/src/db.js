let DatabaseSync = null;
try {
  DatabaseSync = require('node:sqlite').DatabaseSync;
} catch (e) {
  DatabaseSync = null;
}
const path = require('node:path');
const fs = require('node:fs');
const { hashPasswordSync } = require('./crypto-utils');
const { createInMemoryDatabase } = require('./in-memory-db');

let dbInstance = null;

function getDatabase(dbPath) {
  if (dbInstance) return dbInstance;

  const repoDbPath = path.join(__dirname, '..', 'data', 'tgz_lifesteal.db');
  // On Vercel serverless, in-memory SQLite provides zero-latency and 100% reliability
  const defaultDbPath = process.env.VERCEL ? ':memory:' : repoDbPath;

  const resolvedPath = dbPath || process.env.DATABASE_PATH || defaultDbPath;
  
  if (resolvedPath !== ':memory:') {
    const dir = path.dirname(resolvedPath);
    if (!fs.existsSync(dir)) {
      try { fs.mkdirSync(dir, { recursive: true }); } catch (e) {}
    }
  }

  let db = null;
  if (DatabaseSync) {
    try {
      db = new DatabaseSync(resolvedPath);
      try { db.exec('PRAGMA foreign_keys = ON;'); } catch (e) {}
      if (!process.env.VERCEL) {
        try { db.exec('PRAGMA journal_mode = WAL;'); } catch (e) {}
      }
    } catch (err) {
      console.warn('[TGZ LIFESTEAL] File SQLite open failed, falling back to in-memory SQLite:', err?.message);
      try {
        db = new DatabaseSync(':memory:');
        try { db.exec('PRAGMA foreign_keys = ON;'); } catch (e) {}
      } catch (err2) {
        db = null;
      }
    }
  }

  if (!db) {
    console.warn('[TGZ LIFESTEAL] node:sqlite is not available on this platform. Running with zero-dependency high-speed in-memory store.');
    db = createInMemoryDatabase();
  }

  initSchema(db);
  seedInitialData(db);

  dbInstance = db;
  return dbInstance;
}

function initSchema(db) {
  db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      display_name TEXT NOT NULL,
      password_hash TEXT NOT NULL,
      role TEXT NOT NULL CHECK(role IN ('ADMIN', 'MODERATOR', 'MEMBER')),
      must_change_password INTEGER NOT NULL DEFAULT 0,
      status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'SUSPENDED')),
      created_at INTEGER NOT NULL,
      last_login_at INTEGER,
      minecraft_uuid TEXT,
      minecraft_username TEXT
    );

    CREATE TABLE IF NOT EXISTS sessions (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      token_hash TEXT NOT NULL UNIQUE,
      device_info TEXT,
      ip_address TEXT,
      created_at INTEGER NOT NULL,
      last_active_at INTEGER NOT NULL,
      expires_at INTEGER NOT NULL,
      is_revoked INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS announcements (
      id TEXT PRIMARY KEY,
      title TEXT NOT NULL,
      content TEXT NOT NULL,
      author_id TEXT NOT NULL REFERENCES users(id),
      author_name TEXT NOT NULL,
      category TEXT NOT NULL DEFAULT 'GENERAL',
      is_pinned INTEGER NOT NULL DEFAULT 0,
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS announcement_reads (
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      announcement_id TEXT NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
      read_at INTEGER NOT NULL,
      PRIMARY KEY (user_id, announcement_id)
    );

    CREATE TABLE IF NOT EXISTS conversations (
      id TEXT PRIMARY KEY,
      user1_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      user2_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      updated_at INTEGER NOT NULL,
      UNIQUE(user1_id, user2_id)
    );

    CREATE TABLE IF NOT EXISTS messages (
      id TEXT PRIMARY KEY,
      conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
      sender_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      recipient_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      content TEXT NOT NULL,
      is_read INTEGER NOT NULL DEFAULT 0,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS reward_configs (
      reward_id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      required_playtime_minutes INTEGER DEFAULT NULL,
      description TEXT NOT NULL,
      badge TEXT
    );

    CREATE TABLE IF NOT EXISTS reward_claims (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      reward_id TEXT NOT NULL REFERENCES reward_configs(reward_id),
      playtime_at_claim INTEGER NOT NULL,
      claimed_at INTEGER NOT NULL,
      status TEXT NOT NULL DEFAULT 'APPROVED' CHECK(status IN ('APPROVED', 'PENDING_REVIEW', 'REVOKED')),
      reviewed_by TEXT REFERENCES users(id),
      reviewed_at INTEGER,
      UNIQUE(user_id, reward_id)
    );

    CREATE TABLE IF NOT EXISTS minecraft_player_stats (
      uuid TEXT PRIMARY KEY,
      username TEXT NOT NULL COLLATE NOCASE,
      playtime_seconds INTEGER NOT NULL DEFAULT 0,
      kills INTEGER NOT NULL DEFAULT 0,
      deaths INTEGER NOT NULL DEFAULT 0,
      hearts INTEGER NOT NULL DEFAULT 10,
      coins INTEGER NOT NULL DEFAULT 0,
      last_seen INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS teams (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL UNIQUE COLLATE NOCASE,
      description TEXT,
      logo TEXT DEFAULT 'shield',
      status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'RECRUITING', 'FULL', 'PRIVATE')),
      owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS team_members (
      team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
      role TEXT NOT NULL DEFAULT 'MEMBER' CHECK(role IN ('OWNER', 'MEMBER')),
      joined_at INTEGER NOT NULL,
      PRIMARY KEY(team_id, user_id)
    );

    CREATE TABLE IF NOT EXISTS team_invitations (
      id TEXT PRIMARY KEY,
      team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
      inviter_id TEXT NOT NULL REFERENCES users(id),
      invitee_id TEXT NOT NULL REFERENCES users(id),
      status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS app_settings (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL
    );
  `);

  try { db.exec("ALTER TABLE teams ADD COLUMN logo TEXT DEFAULT 'shield'"); } catch (e) {}
  try { db.exec("ALTER TABLE teams ADD COLUMN status TEXT DEFAULT 'ACTIVE'"); } catch (e) {}
}

function seedInitialData(db) {
  // 1. Initial Admin Account: sg_karthi
  const adminId = 'sg_karthi';
  const existingAdmin = db.prepare('SELECT id FROM users WHERE id = ?').get(adminId);
  
  if (!existingAdmin) {
    const bootstrapPassword = process.env.ADMIN_BOOTSTRAP_PASSWORD || 'admin123';
    const hash = hashPasswordSync(bootstrapPassword);
    const now = Date.now();

    db.prepare(`
      INSERT INTO users (id, display_name, password_hash, role, must_change_password, status, created_at)
      VALUES (?, ?, ?, 'ADMIN', 1, 'ACTIVE', ?)
    `).run(adminId, 'Admin (sg_karthi)', hash, now);
  }

  // 2. Initial Playtime Rewards: EXACTLY 5 Crates per requirement 7:
  // 1. Common Crate
  // 2. Monthly Crate
  // 3. Special Crate
  // 4. Arcane Crate
  // 5. Matrix Crate
  // Playtime requirement left unconfigured (NULL) until official requirement provided!
  const crateRewards = [
    {
      id: 'common_crate',
      name: 'Common Crate',
      desc: 'Tier 1 survival supply crate key with basic enchantments and starting supplies.',
      badge: 'TIER 1'
    },
    {
      id: 'monthly_crate',
      name: 'Monthly Crate',
      desc: 'Exclusive monthly crate key with rare seasonal cosmetics and bonus items.',
      badge: 'MONTHLY'
    },
    {
      id: 'special_crate',
      name: 'Special Crate',
      desc: 'Special event crate containing heart fragments and claim protection blocks.',
      badge: 'SPECIAL'
    },
    {
      id: 'arcane_crate',
      name: 'Arcane Crate',
      desc: 'Mystical Arcane crate key infused with powerful custom enchantments and relics.',
      badge: 'MYTHIC'
    },
    {
      id: 'matrix_crate',
      name: 'Matrix Crate',
      desc: 'Ultimate endgame Matrix crate with supreme Netherite equipment and revive tokens.',
      badge: 'LEGENDARY'
    }
  ];

  for (const crate of crateRewards) {
    const existing = db.prepare('SELECT reward_id FROM reward_configs WHERE reward_id = ?').get(crate.id);
    if (!existing) {
      db.prepare(`
        INSERT INTO reward_configs (reward_id, name, required_playtime_minutes, description, badge)
        VALUES (?, ?, NULL, ?, ?)
      `).run(crate.id, crate.name, crate.desc, crate.badge);
    }
  }

  // Clean up any legacy crate rewards if present
  db.prepare(`
    DELETE FROM reward_configs WHERE reward_id NOT IN ('common_crate', 'monthly_crate', 'special_crate', 'arcane_crate', 'matrix_crate')
  `).run();

  // 3. Default App Settings
  const defaultSettings = [
    { key: 'minecraft_server_host', value: process.env.MINECRAFT_SERVER_HOST || 'node2.vyroxsmp.online' },
    { key: 'minecraft_server_port', value: process.env.MINECRAFT_SERVER_PORT || '10010' },
    { key: 'discord_invite_url', value: process.env.DISCORD_INVITE_URL || 'https://discord.gg/J5P89mxXbc' },
    { key: 'session_expiration_days', value: process.env.SESSION_EXPIRATION_DAYS || '30' },
    { key: 'minecraft_plugin_api_key', value: process.env.MINECRAFT_PLUGIN_API_KEY || 'tgz_plugin_sec_99a8b7c6d5e4f3a2' },
    { key: 'allow_member_team_creation', value: 'true' },
    { key: 'motd', value: 'Welcome to TGZ Lifesteal SMP!' }
  ];

  for (const setting of defaultSettings) {
    const existing = db.prepare('SELECT key FROM app_settings WHERE key = ?').get(setting.key);
    if (!existing) {
      db.prepare('INSERT INTO app_settings (key, value) VALUES (?, ?)').run(setting.key, setting.value);
    }
  }

  // 4. Seed an initial welcome announcement if none exist
  const countAnnouncements = db.prepare('SELECT COUNT(*) as cnt FROM announcements').get().cnt;
  if (countAnnouncements === 0) {
    const now = Date.now();
    db.prepare(`
      INSERT INTO announcements (id, title, content, author_id, author_name, category, is_pinned, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, 'UPDATE', 1, ?, ?)
    `).run(
      'ann_welcome_01',
      'Welcome to TGZ Lifesteal SMP',
      'Welcome to the official TGZ Lifesteal community! Join our Minecraft SMP server at node2.vyroxsmp.online:10010, claim hearts, earn playtime crates, and join clans.',
      adminId,
      'Admin (sg_karthi)',
      now,
      now
    );
  }
}

module.exports = {
  getDatabase
};
