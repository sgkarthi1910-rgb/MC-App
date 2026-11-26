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

// Safe, zero-dependency environment loader for local and serverless execution
function loadEnvSafe() {
  const envCandidates = [
    path.join(__dirname, '..', '.env'),
    path.join(__dirname, '..', '..', '.env'),
    path.join(process.cwd(), '.env'),
    path.join(process.cwd(), 'backend', '.env')
  ];
  for (const envPath of envCandidates) {
    if (fs.existsSync(envPath)) {
      try {
        const content = fs.readFileSync(envPath, 'utf8');
        for (const line of content.split('\n')) {
          const trimmed = line.trim();
          if (!trimmed || trimmed.startsWith('#')) continue;
          const eq = trimmed.indexOf('=');
          if (eq > 0) {
            const k = trimmed.slice(0, eq).trim();
            const v = trimmed.slice(eq + 1).trim();
            if (process.env[k] === undefined) {
              process.env[k] = v;
            }
          }
        }
        break;
      } catch (e) {}
    }
  }
}
loadEnvSafe();

let dbInstance = null;

function getDatabase(dbPath) {
  if (dbInstance && !dbPath) return dbInstance;

  const repoDbPath = path.join(__dirname, '..', 'data', 'tgz_lifesteal.db');
  const repoStorePath = path.join(__dirname, '..', 'data', 'tgz_store.json');
  const vercelDbPath = path.join('/tmp', 'tgz_lifesteal.db');
  const vercelStorePath = path.join('/tmp', 'tgz_store.json');

  // Copy bundled seed database & store to /tmp on Vercel cold boot if not yet initialized
  if (process.env.VERCEL) {
    if (!fs.existsSync(vercelDbPath) && fs.existsSync(repoDbPath)) {
      try { fs.copyFileSync(repoDbPath, vercelDbPath); } catch (e) {}
    }
    if (!fs.existsSync(vercelStorePath) && fs.existsSync(repoStorePath)) {
      try { fs.copyFileSync(repoStorePath, vercelStorePath); } catch (e) {}
    }
  }

  const defaultDbPath = process.env.VERCEL ? vercelDbPath : repoDbPath;
  const storeFilePath = process.env.VERCEL ? vercelStorePath : repoStorePath;

  const resolvedPath = dbPath || process.env.DATABASE_PATH || defaultDbPath;
  const isExplicitMemory = (dbPath === ':memory:');
  
  if (resolvedPath !== ':memory:') {
    const dir = path.dirname(resolvedPath);
    if (!fs.existsSync(dir)) {
      try { fs.mkdirSync(dir, { recursive: true }); } catch (e) {}
    }
  }

  let db = null;
  const hasCloudKv = !!(process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL);
  const useCloudKvStore = !isExplicitMemory && (!!process.env.VERCEL || hasCloudKv);

  if (DatabaseSync && !useCloudKvStore) {
    try {
      db = new DatabaseSync(resolvedPath);
      try { db.exec('PRAGMA foreign_keys = ON;'); } catch (e) {}
      if (!process.env.VERCEL && resolvedPath !== ':memory:') {
        try { db.exec('PRAGMA journal_mode = WAL;'); } catch (e) {}
      }
    } catch (err) {
      if (isExplicitMemory) {
        try {
          db = new DatabaseSync(':memory:');
          try { db.exec('PRAGMA foreign_keys = ON;'); } catch (e) {}
        } catch (e2) {
          db = null;
        }
      } else {
        console.warn('[TGZ LIFESTEAL] SQLite open failed, using durable persistent store:', err?.message);
        db = null;
      }
    }
  }

  if (!db) {
    db = createInMemoryDatabase(isExplicitMemory ? null : storeFilePath);
  }

  try {
    initSchema(db);
    seedInitialData(db);
  } catch (err) {
    console.warn('[TGZ LIFESTEAL] Database initialization fallback to durable persistent store:', err?.message);
    db = createInMemoryDatabase(isExplicitMemory ? null : storeFilePath);
    initSchema(db);
    seedInitialData(db);
  }

  if (!dbPath) {
    dbInstance = db;
  }
  return db;
}

function initSchema(db) {
  db.exec(`
    CREATE TABLE IF NOT EXISTS schema_migrations (
      version INTEGER PRIMARY KEY,
      name TEXT NOT NULL,
      applied_at INTEGER NOT NULL
    );

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

    CREATE TABLE IF NOT EXISTS groups (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      description TEXT,
      avatar TEXT DEFAULT 'groups',
      owner_id TEXT NOT NULL REFERENCES users(id),
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS group_members (
      group_id TEXT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      role TEXT NOT NULL DEFAULT 'MEMBER' CHECK(role IN ('OWNER', 'MEMBER')),
      joined_at INTEGER NOT NULL,
      PRIMARY KEY (group_id, user_id)
    );

    CREATE TABLE IF NOT EXISTS group_messages (
      id TEXT PRIMARY KEY,
      group_id TEXT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
      sender_id TEXT NOT NULL REFERENCES users(id),
      content TEXT NOT NULL,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS group_message_reads (
      group_id TEXT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      last_read_at INTEGER NOT NULL,
      PRIMARY KEY (group_id, user_id)
    );

    CREATE TABLE IF NOT EXISTS audit_logs (
      id TEXT PRIMARY KEY,
      actor_id TEXT NOT NULL,
      actor_role TEXT NOT NULL,
      action TEXT NOT NULL,
      target_user_id TEXT,
      details TEXT,
      ip_address TEXT,
      created_at INTEGER NOT NULL
    );
  `);

  try { db.exec("ALTER TABLE teams ADD COLUMN logo TEXT DEFAULT 'shield'"); } catch (e) {}
  try { db.exec("ALTER TABLE teams ADD COLUMN status TEXT DEFAULT 'ACTIVE'"); } catch (e) {}
  try { db.exec("ALTER TABLE users ADD COLUMN avatar TEXT DEFAULT 'player'"); } catch (e) {}
}

const DEFAULT_ADMIN_IDS = ['sg_karthi', 'dhanush', 'lmtg', 'darksight'];

function getAdminUserIds() {
  const envAdminIds = process.env.ADMIN_USER_IDS
    ? process.env.ADMIN_USER_IDS.split(',').map(s => s.trim().toLowerCase()).filter(Boolean)
    : [];
  return Array.from(new Set([...DEFAULT_ADMIN_IDS, ...envAdminIds]));
}

function isAdminUserId(id) {
  if (!id || typeof id !== 'string') return false;
  return getAdminUserIds().includes(id.trim().toLowerCase());
}

function seedInitialData(db) {
  // 1. Initial Admin Accounts: sg_karthi, dhanush, lmtg, darksight (Idempotent Server-Side Provisioning)
  const adminIds = getAdminUserIds();
  const bootstrapPassword = process.env.ADMIN_BOOTSTRAP_PASSWORD || 'TgzAdminInitial2026!Secure';
  const now = Date.now();
  const forceChange = process.env.ADMIN_FORCE_PASSWORD_CHANGE === 'true';
  
  for (const adminId of adminIds) {
    const existingAdmin = db.prepare('SELECT id, role, must_change_password, password_hash FROM users WHERE id = ?').get(adminId);
    if (!existingAdmin) {
      const hash = hashPasswordSync(bootstrapPassword);
      db.prepare(`
        INSERT INTO users (id, display_name, password_hash, role, must_change_password, status, created_at)
        VALUES (?, ?, ?, 'ADMIN', ?, 'ACTIVE', ?)
      `).run(adminId, `Admin (${adminId})`, hash, forceChange ? 1 : 0, now);
    } else {
      // Idempotent safeguard: Ensure role remains ADMIN
      // CRITICAL PRODUCTION FIX: Once admin account exists, normal startup MUST NEVER reset its password!
      if (existingAdmin.role !== 'ADMIN') {
        db.prepare('UPDATE users SET role = ? WHERE id = ?').run('ADMIN', adminId);
      }
    }
  }

  // Record migrations in schema_migrations
  try {
    const m1 = db.prepare('SELECT version FROM schema_migrations WHERE version = 1').get();
    if (!m1) {
      db.prepare('INSERT INTO schema_migrations (version, name, applied_at) VALUES (1, ?, ?)').run('initial_schema', now);
    }
    const m2 = db.prepare('SELECT version FROM schema_migrations WHERE version = 2').get();
    if (!m2) {
      db.prepare('INSERT INTO schema_migrations (version, name, applied_at) VALUES (2, ?, ?)').run('teams_and_avatars', now);
    }
  } catch(e) {}

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
    { key: 'minecraft_bedrock_port', value: process.env.MINECRAFT_BEDROCK_PORT || '10011' },
    { key: 'discord_invite_url', value: process.env.DISCORD_INVITE_URL || 'https://discord.gg/J5P89mxXbc' },
    { key: 'session_expiration_days', value: process.env.SESSION_EXPIRATION_DAYS || '30' },
    { key: 'minecraft_plugin_api_key', value: process.env.MINECRAFT_PLUGIN_API_KEY || 'tgz_plugin_sec_99a8b7c6d5e4f3a2' },
    { key: 'allow_member_team_creation', value: 'true' },
    { key: 'motd', value: 'Welcome to TGZ Lifestyle SMP!' },
    { key: 'app_version_code', value: process.env.APP_VERSION_CODE || '5' },
    { key: 'app_version_name', value: process.env.APP_VERSION_NAME || '1.4' },
    { key: 'app_apk_url', value: process.env.APP_APK_URL || 'https://github.com/sgkarthi1910-rgb/MC-App/releases/download/v1.4/TGZ-Lifestyle-v1.4.apk' },
    { key: 'app_changelog', value: 'Release v1.4: Persistent DB password updates, HTTP 401 session fixes, strict role-based password permissions, and login-every-open security.' },
    { key: 'app_force_update', value: 'false' },
    { key: 'app_min_supported_version_code', value: '1' }
  ];

  for (const setting of defaultSettings) {
    const existing = db.prepare('SELECT key FROM app_settings WHERE key = ?').get(setting.key);
    if (!existing) {
      db.prepare('INSERT INTO app_settings (key, value) VALUES (?, ?)').run(setting.key, setting.value);
    }
  }

  // Ensure version code is at least 5 for live in-app OTA update rollout
  try {
    const curCodeRow = db.prepare("SELECT value FROM app_settings WHERE key = 'app_version_code'").get();
    const curApkRow = db.prepare("SELECT value FROM app_settings WHERE key = 'app_apk_url'").get();
    if (!curCodeRow || parseInt(curCodeRow.value, 10) < 5 || !curApkRow || curApkRow.value.includes('v1.1') || curApkRow.value.includes('v1.2') || curApkRow.value.includes('v1.3') || curApkRow.value.includes('releases/latest')) {
      db.prepare("INSERT INTO app_settings (key, value) VALUES ('app_version_code', '5') ON CONFLICT(key) DO UPDATE SET value = '5'").run();
      db.prepare("INSERT INTO app_settings (key, value) VALUES ('app_version_name', '1.4') ON CONFLICT(key) DO UPDATE SET value = '1.4'").run();
      db.prepare("INSERT INTO app_settings (key, value) VALUES ('app_apk_url', 'https://github.com/sgkarthi1910-rgb/MC-App/releases/download/v1.4/TGZ-Lifestyle-v1.4.apk') ON CONFLICT(key) DO UPDATE SET value = 'https://github.com/sgkarthi1910-rgb/MC-App/releases/download/v1.4/TGZ-Lifestyle-v1.4.apk'").run();
      db.prepare("INSERT INTO app_settings (key, value) VALUES ('app_changelog', 'Release v1.4: Persistent DB password updates, HTTP 401 session fixes, strict role-based password permissions, and login-every-open security.') ON CONFLICT(key) DO UPDATE SET value = 'Release v1.4: Persistent DB password updates, HTTP 401 session fixes, strict role-based password permissions, and login-every-open security.'").run();
    }
  } catch(e) {}

  // 4. Seed an initial welcome announcement if none exist
  const countAnnouncements = db.prepare('SELECT COUNT(*) as cnt FROM announcements').get().cnt;
  if (countAnnouncements === 0) {
    const now = Date.now();
    db.prepare(`
      INSERT INTO announcements (id, title, content, author_id, author_name, category, is_pinned, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, 'UPDATE', 1, ?, ?)
    `).run(
      'ann_welcome_01',
      'Welcome to TGZ Lifestyle SMP',
      'Welcome to the official TGZ Minecraft Lifestyle community! Join our server at node2.vyroxsmp.online (Java: 10010 / Bedrock: 10011), build your journey, earn playtime crates, connect with friends, and create teams.',
      adminIds[0] || 'sg_karthi',
      `Admin (${adminIds[0] || 'sg_karthi'})`,
      now,
      now
    );
  }
}

function provisionAdminAccount(db) {
  seedInitialData(db);
}

module.exports = {
  getDatabase,
  initSchema,
  seedInitialData,
  provisionAdminAccount,
  DEFAULT_ADMIN_IDS,
  getAdminUserIds,
  isAdminUserId
};
