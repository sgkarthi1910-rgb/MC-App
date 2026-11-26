/**
 * TGZ Lifesteal - Enterprise Managed PostgreSQL Database Adapter
 * Seamlessly connects to Supabase, Neon, Railway, or Vercel Postgres
 * when DATABASE_URL or POSTGRES_URL environment variables are configured.
 */

let pg = null;
try {
  pg = require('pg');
} catch (e) {
  pg = null;
}

let poolInstance = null;
let lastSyncStatus = 'NOT_INITIALIZED';
let lastSyncTime = 0;
let schemaInitialized = false;

function getPostgresUrl() {
  return (
    process.env.DATABASE_URL ||
    process.env.POSTGRES_URL ||
    process.env.SUPABASE_DATABASE_URL ||
    process.env.NEON_DATABASE_URL ||
    null
  );
}

function isPostgresConfigured() {
  return !!(pg && getPostgresUrl());
}

function getPostgresPool() {
  if (poolInstance) return poolInstance;
  const connectionString = getPostgresUrl();
  if (!connectionString || !pg) return null;

  try {
    const isRemote = !connectionString.includes('localhost') && !connectionString.includes('127.0.0.1');
    poolInstance = new pg.Pool({
      connectionString,
      ssl: isRemote ? { rejectUnauthorized: false } : false,
      max: 10,
      idleTimeoutMillis: 30000,
      connectionTimeoutMillis: 5000
    });

    poolInstance.on('error', (err) => {
      console.error('[TGZ POSTGRES] Pool idle client error:', err.message);
      lastSyncStatus = `POSTGRES_POOL_ERROR: ${err.message}`;
    });

    return poolInstance;
  } catch (err) {
    console.error('[TGZ POSTGRES] Initialization error:', err.message);
    lastSyncStatus = `POSTGRES_INIT_ERROR: ${err.message}`;
    return null;
  }
}

async function initPostgresSchema(pool) {
  if (schemaInitialized) return true;
  if (!pool) return false;

  const ddl = `
    CREATE TABLE IF NOT EXISTS schema_migrations (
      version INTEGER PRIMARY KEY,
      name TEXT NOT NULL,
      applied_at BIGINT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      display_name TEXT NOT NULL,
      password_hash TEXT NOT NULL,
      role TEXT NOT NULL CHECK(role IN ('ADMIN', 'MODERATOR', 'MEMBER')),
      must_change_password INTEGER NOT NULL DEFAULT 0,
      status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'SUSPENDED')),
      created_at BIGINT NOT NULL,
      last_login_at BIGINT,
      minecraft_uuid TEXT,
      minecraft_username TEXT,
      avatar TEXT DEFAULT 'player'
    );

    CREATE TABLE IF NOT EXISTS sessions (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      token_hash TEXT NOT NULL UNIQUE,
      device_info TEXT,
      ip_address TEXT,
      created_at BIGINT NOT NULL,
      last_active_at BIGINT NOT NULL,
      expires_at BIGINT NOT NULL,
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
      created_at BIGINT NOT NULL,
      updated_at BIGINT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS announcement_reads (
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      announcement_id TEXT NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
      read_at BIGINT NOT NULL,
      PRIMARY KEY (user_id, announcement_id)
    );

    CREATE TABLE IF NOT EXISTS conversations (
      id TEXT PRIMARY KEY,
      user1_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      user2_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      updated_at BIGINT NOT NULL,
      UNIQUE(user1_id, user2_id)
    );

    CREATE TABLE IF NOT EXISTS messages (
      id TEXT PRIMARY KEY,
      conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
      sender_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      recipient_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      content TEXT NOT NULL,
      is_read INTEGER NOT NULL DEFAULT 0,
      created_at BIGINT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS reward_configs (
      reward_id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      required_playtime_minutes INTEGER,
      description TEXT NOT NULL,
      badge TEXT
    );

    CREATE TABLE IF NOT EXISTS reward_claims (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      reward_id TEXT NOT NULL REFERENCES reward_configs(reward_id),
      playtime_at_claim INTEGER NOT NULL,
      claimed_at BIGINT NOT NULL,
      status TEXT NOT NULL DEFAULT 'APPROVED' CHECK(status IN ('APPROVED', 'PENDING_REVIEW', 'REVOKED')),
      reviewed_by TEXT REFERENCES users(id),
      reviewed_at BIGINT,
      UNIQUE(user_id, reward_id)
    );

    CREATE TABLE IF NOT EXISTS minecraft_player_stats (
      uuid TEXT PRIMARY KEY,
      username TEXT NOT NULL,
      playtime_seconds INTEGER NOT NULL DEFAULT 0,
      kills INTEGER NOT NULL DEFAULT 0,
      deaths INTEGER NOT NULL DEFAULT 0,
      hearts INTEGER NOT NULL DEFAULT 10,
      coins INTEGER NOT NULL DEFAULT 0,
      last_seen BIGINT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS teams (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL UNIQUE,
      description TEXT,
      logo TEXT DEFAULT 'shield',
      status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'RECRUITING', 'FULL', 'PRIVATE')),
      owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
      created_at BIGINT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS team_members (
      team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
      role TEXT NOT NULL DEFAULT 'MEMBER' CHECK(role IN ('OWNER', 'MEMBER')),
      joined_at BIGINT NOT NULL,
      PRIMARY KEY(team_id, user_id)
    );

    CREATE TABLE IF NOT EXISTS team_invitations (
      id TEXT PRIMARY KEY,
      team_id TEXT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
      inviter_id TEXT NOT NULL REFERENCES users(id),
      invitee_id TEXT NOT NULL REFERENCES users(id),
      status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
      created_at BIGINT NOT NULL
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
      created_at BIGINT NOT NULL,
      updated_at BIGINT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS group_members (
      group_id TEXT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      role TEXT NOT NULL DEFAULT 'MEMBER' CHECK(role IN ('OWNER', 'MEMBER')),
      joined_at BIGINT NOT NULL,
      PRIMARY KEY (group_id, user_id)
    );

    CREATE TABLE IF NOT EXISTS group_messages (
      id TEXT PRIMARY KEY,
      group_id TEXT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
      sender_id TEXT NOT NULL REFERENCES users(id),
      content TEXT NOT NULL,
      created_at BIGINT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS group_message_reads (
      group_id TEXT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      last_read_at BIGINT NOT NULL,
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
      created_at BIGINT NOT NULL
    );
  `;

  try {
    await pool.query(ddl);
    schemaInitialized = true;
    lastSyncStatus = 'POSTGRES_SCHEMA_INITIALIZED';
    return true;
  } catch (err) {
    console.error('[TGZ POSTGRES] Failed to initialize schema:', err.message);
    lastSyncStatus = `POSTGRES_SCHEMA_ERROR: ${err.message}`;
    return false;
  }
}

async function loadAllFromPostgres(pool) {
  if (!pool) return null;
  const tables = [
    'schema_migrations', 'users', 'sessions', 'announcements', 'announcement_reads',
    'conversations', 'messages', 'reward_configs', 'reward_claims',
    'minecraft_player_stats', 'teams', 'team_members', 'team_invitations',
    'app_settings', 'groups', 'group_members', 'group_messages', 'group_message_reads',
    'audit_logs'
  ];

  const result = {};
  for (const t of tables) {
    try {
      const res = await pool.query(`SELECT * FROM ${t}`);
      result[t] = res.rows || [];
    } catch (err) {
      result[t] = [];
    }
  }
  lastSyncTime = Date.now();
  lastSyncStatus = 'SYNCED_FROM_POSTGRES';
  return result;
}

async function migrateDataToPostgres(pool, memoryTables) {
  if (!pool || !memoryTables) return false;
  try {
    // Check if Postgres users table is already populated
    const checkRes = await pool.query('SELECT COUNT(*) AS cnt FROM users');
    const existingCount = parseInt(checkRes.rows[0]?.cnt || '0', 10);
    if (existingCount > 0) {
      // Postgres already contains data; do not overwrite
      return true;
    }

    console.log('[TGZ POSTGRES] Fresh database detected. Safely migrating existing records to permanent PostgreSQL...');

    // 1. Users
    if (Array.isArray(memoryTables.users)) {
      for (const u of memoryTables.users) {
        await pool.query(`
          INSERT INTO users (id, display_name, password_hash, role, must_change_password, status, created_at, last_login_at, minecraft_uuid, minecraft_username, avatar)
          VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
          ON CONFLICT (id) DO NOTHING
        `, [
          u.id, u.display_name, u.password_hash, u.role,
          u.must_change_password ? 1 : 0, u.status || 'ACTIVE',
          u.created_at || Date.now(), u.last_login_at || null,
          u.minecraft_uuid || null, u.minecraft_username || null,
          u.avatar || 'player'
        ]);
      }
    }

    // 2. Reward configs
    if (Array.isArray(memoryTables.reward_configs)) {
      for (const r of memoryTables.reward_configs) {
        await pool.query(`
          INSERT INTO reward_configs (reward_id, name, required_playtime_minutes, description, badge)
          VALUES ($1, $2, $3, $4, $5)
          ON CONFLICT (reward_id) DO NOTHING
        `, [r.reward_id, r.name, r.required_playtime_minutes || null, r.description, r.badge]);
      }
    }

    // 3. App Settings
    if (Array.isArray(memoryTables.app_settings)) {
      for (const s of memoryTables.app_settings) {
        await pool.query(`
          INSERT INTO app_settings (key, value)
          VALUES ($1, $2)
          ON CONFLICT (key) DO NOTHING
        `, [s.key, s.value]);
      }
    }

    // 4. Announcements
    if (Array.isArray(memoryTables.announcements)) {
      for (const a of memoryTables.announcements) {
        await pool.query(`
          INSERT INTO announcements (id, title, content, author_id, author_name, category, is_pinned, created_at, updated_at)
          VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
          ON CONFLICT (id) DO NOTHING
        `, [a.id, a.title, a.content, a.author_id, a.author_name, a.category, a.is_pinned ? 1 : 0, a.created_at, a.updated_at]);
      }
    }

    // 5. Teams
    if (Array.isArray(memoryTables.teams)) {
      for (const tm of memoryTables.teams) {
        await pool.query(`
          INSERT INTO teams (id, name, description, logo, status, owner_id, created_at)
          VALUES ($1, $2, $3, $4, $5, $6, $7)
          ON CONFLICT (id) DO NOTHING
        `, [tm.id, tm.name, tm.description, tm.logo || 'shield', tm.status || 'ACTIVE', tm.owner_id, tm.created_at]);
      }
    }

    // 6. Team members
    if (Array.isArray(memoryTables.team_members)) {
      for (const m of memoryTables.team_members) {
        await pool.query(`
          INSERT INTO team_members (team_id, user_id, role, joined_at)
          VALUES ($1, $2, $3, $4)
          ON CONFLICT (team_id, user_id) DO NOTHING
        `, [m.team_id, m.user_id, m.role, m.joined_at]);
      }
    }

    lastSyncStatus = 'MIGRATED_TO_POSTGRES';
    lastSyncTime = Date.now();
    console.log('[TGZ POSTGRES] Successfully migrated seed and backup records to PostgreSQL!');
    return true;
  } catch (err) {
    console.error('[TGZ POSTGRES] Error during data migration:', err.message);
    lastSyncStatus = `MIGRATION_ERROR: ${err.message}`;
    return false;
  }
}

async function syncTableToPostgres(pool, tableName, rows) {
  if (!pool || !schemaInitialized || !Array.isArray(rows)) return;
  try {
    for (const row of rows) {
      const keys = Object.keys(row);
      if (keys.length === 0) continue;
      const cols = keys.join(', ');
      const placeholders = keys.map((_, i) => `$${i + 1}`).join(', ');
      const values = keys.map(k => row[k]);

      // Conflict target is primary key ('id' for most, 'key' for app_settings, 'reward_id' for reward_configs, etc.)
      let pkey = 'id';
      if (tableName === 'app_settings') pkey = 'key';
      else if (tableName === 'reward_configs') pkey = 'reward_id';
      else if (tableName === 'schema_migrations') pkey = 'version';
      else if (tableName === 'announcement_reads') pkey = 'user_id, announcement_id';
      else if (tableName === 'team_members') pkey = 'team_id, user_id';
      else if (tableName === 'group_members') pkey = 'group_id, user_id';
      else if (tableName === 'group_message_reads') pkey = 'group_id, user_id';
      else if (tableName === 'reward_claims') pkey = 'user_id, reward_id';
      else if (tableName === 'minecraft_player_stats') pkey = 'uuid';

      const updateCols = keys.filter(k => !pkey.includes(k));
      let onConflict = '';
      if (updateCols.length > 0) {
        const updateSet = updateCols.map(c => `${c} = EXCLUDED.${c}`).join(', ');
        onConflict = `ON CONFLICT (${pkey}) DO UPDATE SET ${updateSet}`;
      } else {
        onConflict = `ON CONFLICT (${pkey}) DO NOTHING`;
      }

      const sql = `INSERT INTO ${tableName} (${cols}) VALUES (${placeholders}) ${onConflict}`;
      await pool.query(sql, values);
    }
    lastSyncTime = Date.now();
    lastSyncStatus = 'SYNCED_TO_POSTGRES';
  } catch (err) {
    lastSyncStatus = `POSTGRES_PUSH_ERROR: ${err.message}`;
  }
}

function getStatus() {
  return {
    isConfigured: isPostgresConfigured(),
    lastSyncStatus,
    lastSyncTime,
    schemaInitialized
  };
}

module.exports = {
  isPostgresConfigured,
  getPostgresPool,
  initPostgresSchema,
  loadAllFromPostgres,
  migrateDataToPostgres,
  syncTableToPostgres,
  getStatus
};
