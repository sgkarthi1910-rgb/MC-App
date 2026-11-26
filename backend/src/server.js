const http = require('node:http');
const url = require('node:url');
const { getDatabase } = require('./db');
const { hashToken } = require('./crypto-utils');

const setupAuthRoutes = require('./routes/auth');
const setupAnnouncementRoutes = require('./routes/announcements');
const setupMessageRoutes = require('./routes/messages');
const setupGroupRoutes = require('./routes/groups');
const setupRewardRoutes = require('./routes/rewards');
const setupLeaderboardRoutes = require('./routes/leaderboard');
const setupTeamRoutes = require('./routes/teams');
const setupSettingsRoutes = require('./routes/settings');
const setupDashboardRoutes = require('./routes/dashboard');

// Simple Rate Limiter for Login/Signup
const rateLimitMap = new Map();
function checkRateLimit(ip, limit = 20, windowMs = 15 * 60 * 1000) {
  const now = Date.now();
  let record = rateLimitMap.get(ip);
  if (!record || now - record.startTime > windowMs) {
    record = { startTime: now, count: 1 };
    rateLimitMap.set(ip, record);
    return true;
  }
  record.count++;
  if (record.count > limit) {
    return false;
  }
  return true;
}

// Router Implementation
class SimpleRouter {
  constructor() {
    this.routes = [];
  }

  add(method, pathPattern, handler) {
    // Convert /api/users/:id to regex
    const paramNames = [];
    const regexPath = pathPattern.replace(/:([a-zA-Z0-9_]+)/g, (_, name) => {
      paramNames.push(name);
      return '([^/]+)';
    });
    const regex = new RegExp(`^${regexPath}$`);
    this.routes.push({ method: method.toUpperCase(), regex, paramNames, handler });
  }

  get(path, handler) { this.add('GET', path, handler); }
  post(path, handler) { this.add('POST', path, handler); }
  put(path, handler) { this.add('PUT', path, handler); }
  delete(path, handler) { this.add('DELETE', path, handler); }

  match(method, pathname) {
    for (const route of this.routes) {
      if (route.method === method.toUpperCase()) {
        const match = pathname.match(route.regex);
        if (match) {
          const params = {};
          route.paramNames.forEach((name, i) => {
            params[name] = decodeURIComponent(match[i + 1]);
          });
          return { handler: route.handler, params };
        }
      }
    }
    return null;
  }
}

function parseCookies(cookieHeader) {
  const list = {};
  if (!cookieHeader) return list;
  cookieHeader.split(';').forEach(cookie => {
    let [name, ...rest] = cookie.split('=');
    name = name?.trim();
    if (!name) return;
    const value = rest.join('=').trim();
    list[name] = decodeURIComponent(value);
  });
  return list;
}

function createServer(dbPath) {
  const db = getDatabase(dbPath);
  const router = new SimpleRouter();

  // Attach all feature route groups
  setupAuthRoutes(router, db);
  setupAnnouncementRoutes(router, db);
  setupMessageRoutes(router, db);
  setupGroupRoutes(router, db);
  setupRewardRoutes(router, db);
  setupLeaderboardRoutes(router, db);
  setupTeamRoutes(router, db);
  setupSettingsRoutes(router, db);
  setupDashboardRoutes(router, db);

  // Healthcheck & Root endpoints
  router.get('/', (req, res) => {
    res.json({
      status: 'ok',
      service: 'TGZ Lifesteal API',
      version: '1.0.0',
      timestamp: Date.now(),
      endpoints: [
        '/api/health',
        '/api/auth/login',
        '/api/auth/signup',
        '/api/announcements',
        '/api/messages',
        '/api/rewards',
        '/api/leaderboard',
        '/api/teams',
        '/api/settings',
        '/api/dashboard/summary'
      ]
    });
  });

  router.get('/api', (req, res) => {
    res.json({
      status: 'ok',
      service: 'TGZ Lifesteal API',
      version: '1.0.0',
      timestamp: Date.now()
    });
  });

  router.get('/api/health', (req, res) => {
    res.json({ status: 'ok', service: 'TGZ Lifesteal API', timestamp: Date.now() });
  });

  router.get('/health', (req, res) => {
    res.json({ status: 'ok', service: 'TGZ Lifesteal API', timestamp: Date.now() });
  });

  const handleRequest = async (req, res) => {
    // Decorate res with json helper
    res.json = function(data) {
      const payload = JSON.stringify(data);
      this.setHeader('Content-Type', 'application/json; charset=utf-8');
      this.end(payload);
    };

    res.status = function(code) {
      this.statusCode = code;
      return this;
    };

    // CORS Headers
    const origin = req.headers.origin || '*';
    res.setHeader('Access-Control-Allow-Origin', origin);
    res.setHeader('Access-Control-Allow-Credentials', 'true');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Plugin-Key');
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
    res.setHeader('Pragma', 'no-cache');
    res.setHeader('Expires', '0');

    if (req.method === 'OPTIONS') {
      res.statusCode = 204;
      res.end();
      return;
    }

    const parsedUrl = new URL(req.url, 'http://localhost');
    req.pathname = parsedUrl.pathname;
    req.query = Object.fromEntries(parsedUrl.searchParams.entries());
    req.cookies = parseCookies(req.headers.cookie);

    if (typeof db.ensureSynced === 'function') {
      await db.ensureSynced();
    }

    // Rate Limiting on Login and Signup
    const clientIp = req.socket?.remoteAddress || 'unknown';
    if ((req.pathname === '/api/auth/login' || req.pathname === '/api/auth/signup') && req.method === 'POST') {
      if (!checkRateLimit(clientIp, 25, 15 * 60 * 1000)) {
        return res.status(429).json({ error: 'Too many requests. Please wait a few minutes before trying again.' });
      }
    }

    // Authenticate Session
    let rawToken = null;
    const authHeader = req.headers['authorization'];
    if (authHeader && authHeader.startsWith('Bearer ')) {
      rawToken = authHeader.substring(7).trim();
    } else if (req.cookies['tgz_session']) {
      rawToken = req.cookies['tgz_session'];
    }

    req.user = null;
    req.session = null;

    if (rawToken) {
      const tokenHash = hashToken(rawToken);
      const now = Date.now();
      let sessionRow = db.prepare(`
        SELECT id, user_id, created_at, expires_at, last_active_at, device_info, is_revoked
        FROM sessions
        WHERE token_hash = ? AND is_revoked = 0 AND expires_at > ?
      `).get(tokenHash, now);

      if (!sessionRow && typeof db.ensureSynced === 'function') {
        await db.ensureSynced(true);
        sessionRow = db.prepare(`
          SELECT id, user_id, created_at, expires_at, last_active_at, device_info, is_revoked
          FROM sessions
          WHERE token_hash = ? AND is_revoked = 0 AND expires_at > ?
        `).get(tokenHash, now);
      }

      if (sessionRow) {
        const userRow = db.prepare(`
          SELECT id, display_name, role, must_change_password, status, created_at,
                 password_hash, minecraft_uuid, minecraft_username
          FROM users
          WHERE id = ?
        `).get(sessionRow.user_id);

        if (userRow && userRow.status === 'ACTIVE') {
          req.session = {
            id: sessionRow.id,
            userId: sessionRow.user_id,
            createdAt: sessionRow.created_at,
            expiresAt: sessionRow.expires_at,
            deviceInfo: sessionRow.device_info
          };
          req.user = {
            id: userRow.id,
            display_name: userRow.display_name,
            role: userRow.role,
            must_change_password: userRow.must_change_password,
            status: userRow.status,
            password_hash: userRow.password_hash,
            created_at: userRow.created_at,
            minecraft_uuid: userRow.minecraft_uuid,
            minecraft_username: userRow.minecraft_username
          };
        }
      }
    }

    // Read Body for POST/PUT (Compatible with both Node standalone and Vercel serverless)
    if (req.method === 'POST' || req.method === 'PUT') {
      if (req.body !== undefined && req.body !== null && typeof req.body === 'object' && Object.keys(req.body).length > 0) {
        // Pre-parsed by Vercel serverless environment
        dispatchRoute(router, req, res);
      } else if (req.readableEnded) {
        if (typeof req.body === 'string') {
          try { req.body = JSON.parse(req.body); } catch (e) { req.body = {}; }
        } else if (!req.body) {
          req.body = {};
        }
        dispatchRoute(router, req, res);
      } else {
        let bodyData = '';
        req.on('data', chunk => {
          bodyData += chunk;
          if (bodyData.length > 2 * 1024 * 1024) { // 2MB protection
            req.destroy();
          }
        });

        req.on('end', () => {
          try {
            req.body = bodyData ? JSON.parse(bodyData) : (req.body || {});
          } catch (e) {
            req.body = req.body || {};
          }
          dispatchRoute(router, req, res);
        });
      }
    } else {
      req.body = req.body || {};
      dispatchRoute(router, req, res);
    }
  };

  const server = http.createServer(handleRequest);

  return { server, db, router, handleRequest };
}

function dispatchRoute(router, req, res) {
  const match = router.match(req.method, req.pathname);
  if (!match) {
    return res.status(404).json({ error: `Cannot ${req.method} ${req.pathname}` });
  }
  req.params = match.params;
  try {
    match.handler(req, res);
  } catch (err) {
    console.error('Unhandled route error:', err);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
}

// Start standalone server if invoked directly
if (require.main === module) {
  const PORT = parseInt(process.env.PORT || '3000', 10);
  const HOST = process.env.HOST || '0.0.0.0';
  const { server } = createServer();
  server.listen(PORT, HOST, () => {
    console.log(`[TGZ LIFESTEAL] Backend running at http://${HOST}:${PORT}`);
    console.log(`[TGZ LIFESTEAL] Minecraft Server configured: ${process.env.MINECRAFT_SERVER_HOST || 'node2.vyroxsmp.online'}:${process.env.MINECRAFT_SERVER_PORT || '10010'}`);
  });
}

module.exports = {
  createServer
};
