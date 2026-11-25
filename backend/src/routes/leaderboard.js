const { queryMinecraftServer, queryBedrockServer } = require('../minecraft-query');
const { syncServerTeams } = require('./teams');

function setupLeaderboardRoutes(router, db) {
  // 1. GET MINECRAFT SERVER STATUS (Both Java 10010 and Bedrock 10011)
  router.get('/api/minecraft/status', async (req, res) => {
    const hostSetting = db.prepare("SELECT value FROM app_settings WHERE key = 'minecraft_server_host'").get()?.value || 'node2.vyroxsmp.online';
    const javaPort = parseInt(db.prepare("SELECT value FROM app_settings WHERE key = 'minecraft_server_port'").get()?.value || '10010', 10);
    const bedrockPort = parseInt(db.prepare("SELECT value FROM app_settings WHERE key = 'minecraft_bedrock_port'").get()?.value || '10011', 10);

    const [javaStatus, bedrockStatus] = await Promise.all([
      queryMinecraftServer(hostSetting, javaPort),
      queryBedrockServer(hostSetting, bedrockPort)
    ]);

    const lastUpdated = Date.now();
    return res.json({
      server: {
        host: hostSetting,
        port: javaPort,
        fullAddress: `${hostSetting}:${javaPort}`,
        ...javaStatus,
        online: Boolean(javaStatus?.isOnline),
        lastUpdated
      },
      java: {
        host: hostSetting,
        port: javaPort,
        fullAddress: `${hostSetting}:${javaPort}`,
        ...javaStatus,
        online: Boolean(javaStatus?.isOnline),
        lastUpdated
      },
      bedrock: {
        host: hostSetting,
        port: bedrockPort,
        fullAddress: `${hostSetting}:${bedrockPort}`,
        ...bedrockStatus,
        online: Boolean(bedrockStatus?.isOnline),
        lastUpdated
      }
    });
  });

  router.get('/api/minecraft/status/java', async (req, res) => {
    const hostSetting = db.prepare("SELECT value FROM app_settings WHERE key = 'minecraft_server_host'").get()?.value || 'node2.vyroxsmp.online';
    const javaPort = parseInt(db.prepare("SELECT value FROM app_settings WHERE key = 'minecraft_server_port'").get()?.value || '10010', 10);
    const status = await queryMinecraftServer(hostSetting, javaPort);
    return res.json({
      edition: 'JAVA',
      address: hostSetting,
      host: hostSetting,
      port: javaPort,
      fullAddress: `${hostSetting}:${javaPort}`,
      ...status,
      online: Boolean(status?.isOnline),
      lastUpdated: Date.now()
    });
  });

  router.get('/api/minecraft/status/bedrock', async (req, res) => {
    const hostSetting = db.prepare("SELECT value FROM app_settings WHERE key = 'minecraft_server_host'").get()?.value || 'node2.vyroxsmp.online';
    const bedrockPort = parseInt(db.prepare("SELECT value FROM app_settings WHERE key = 'minecraft_bedrock_port'").get()?.value || '10011', 10);
    const status = await queryBedrockServer(hostSetting, bedrockPort);
    return res.json({
      edition: 'BEDROCK',
      address: hostSetting,
      host: hostSetting,
      port: bedrockPort,
      fullAddress: `${hostSetting}:${bedrockPort}`,
      ...status,
      online: Boolean(status?.isOnline),
      lastUpdated: Date.now()
    });
  });

  // 2. SECURE PLUGIN INGESTION ENDPOINT
  // Receives authoritative stats from Minecraft server plugin:
  // UUID, username, playtime_seconds, kills, deaths, hearts, coins
  router.post('/api/minecraft/report-stats', (req, res) => {
    const pluginApiKey = req.headers['x-plugin-key'];
    const expectedKey = db.prepare("SELECT value FROM app_settings WHERE key = 'minecraft_plugin_api_key'").get()?.value;

    if (!pluginApiKey || pluginApiKey !== expectedKey) {
      return res.status(401).json({ error: 'Unauthorized: Invalid X-Plugin-Key header.' });
    }

    const { players } = req.body || {};
    if (!Array.isArray(players)) {
      return res.status(400).json({ error: 'Body must contain a "players" array.' });
    }

    const now = Date.now();
    const upsertStmt = db.prepare(`
      INSERT INTO minecraft_player_stats (uuid, username, playtime_seconds, kills, deaths, hearts, coins, last_seen)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(uuid) DO UPDATE SET
        username = excluded.username,
        playtime_seconds = excluded.playtime_seconds,
        kills = excluded.kills,
        deaths = excluded.deaths,
        hearts = excluded.hearts,
        coins = excluded.coins,
        last_seen = excluded.last_seen
    `);

    for (const p of players) {
      if (p.uuid && p.username) {
        upsertStmt.run(
          p.uuid,
          p.username,
          parseInt(p.playtime_seconds || 0, 10),
          parseInt(p.kills || 0, 10),
          parseInt(p.deaths || 0, 10),
          parseInt(p.hearts || 10, 10),
          parseInt(p.coins || 0, 10),
          now
        );
      }
    }

    if (Array.isArray(req.body?.teams)) {
      syncServerTeams(db, req.body.teams);
    }

    return res.json({ success: true, processedCount: players.length, timestamp: now });
  });

  // 3. GET LEADERBOARD (Real data from Minecraft plugin / server stats)
  router.get('/api/leaderboard', (req, res) => {
    const category = (req.query.category || 'playtime').toLowerCase();
    let orderColumn = 'playtime_seconds';
    if (category === 'kills') orderColumn = 'kills';
    if (category === 'hearts') orderColumn = 'hearts';
    if (category === 'coins') orderColumn = 'coins';

    const players = db.prepare(`
      SELECT uuid, username, playtime_seconds, kills, deaths, hearts, coins, last_seen,
             CASE WHEN deaths > 0 THEN ROUND(CAST(kills AS FLOAT) / deaths, 2) ELSE kills END AS kdr
      FROM minecraft_player_stats
      ORDER BY ${orderColumn} DESC, kills DESC
      LIMIT 100
    `).all();

    const formatted = players.map((p, index) => ({
      rank: index + 1,
      uuid: p.uuid,
      username: p.username,
      playtimeSeconds: p.playtime_seconds,
      playtimeFormatted: `${Math.floor(p.playtime_seconds / 3600)}h ${Math.floor((p.playtime_seconds % 3600) / 60)}m`,
      kills: p.kills,
      deaths: p.deaths,
      kdr: p.kdr,
      hearts: p.hearts,
      coins: p.coins,
      avatarUrl: `https://mc-heads.net/avatar/${encodeURIComponent(p.username)}/64`,
      lastSeen: p.last_seen
    }));

    return res.json({
      category,
      totalPlayersTracked: formatted.length,
      lastUpdated: Date.now(),
      leaderboard: formatted,
      hasRealData: formatted.length > 0,
      syncNote: formatted.length === 0
        ? 'Authoritative server stats are ready. Install the TGZ Minecraft Server Plugin or trigger /api/minecraft/report-stats to stream live telemetry.'
        : null
    });
  });
}

module.exports = setupLeaderboardRoutes;
