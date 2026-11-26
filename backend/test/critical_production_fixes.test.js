const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const path = require('node:path');
const fs = require('node:fs');
const { createServer } = require('../src/server');

function makeRequest(server, options, payload) {
  return new Promise((resolve, reject) => {
    const address = server.address();
    const req = http.request({
      hostname: '127.0.0.1',
      port: address.port,
      path: options.path,
      method: options.method || 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      }
    }, (res) => {
      let data = '';
      res.on('data', chunk => { data += chunk; });
      res.on('end', () => {
        let body = null;
        try { body = data ? JSON.parse(data) : null; } catch (e) { body = data; }
        resolve({ statusCode: res.statusCode, headers: res.headers, body });
      });
    });
    req.on('error', reject);
    if (payload) {
      req.write(typeof payload === 'string' ? payload : JSON.stringify(payload));
    }
    req.end();
  });
}

test('CRITICAL PRODUCTION FIXES VERIFICATION SUITE', async (t) => {
  const tempStorePath = path.join(__dirname, 'temp_test_store.json');
  if (fs.existsSync(tempStorePath)) fs.unlinkSync(tempStorePath);

  process.env.ADMIN_BOOTSTRAP_PASSWORD = 'OriginalBootstrapPassword2026!';
  process.env.DATABASE_PATH = tempStorePath;

  let testServerObj = createServer(tempStorePath);
  await new Promise(r => testServerObj.server.listen(0, '127.0.0.1', r));

  t.after(() => {
    try { testServerObj.server.close(); } catch(e){}
    try { if (fs.existsSync(tempStorePath)) fs.unlinkSync(tempStorePath); } catch(e){}
  });

  let adminSessionToken = null;

  // TEST 1: Initial Login with Bootstrap Password
  await t.test('1. Initial admin provisioning allows bootstrap login for sg_karthi', async () => {
    const res = await makeRequest(testServerObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: 'OriginalBootstrapPassword2026!'
    });
    assert.equal(res.statusCode, 200);
    assert.equal(res.body.user.id, 'sg_karthi');
    assert.equal(res.body.user.role, 'ADMIN');
    assert.ok(res.body.sessionToken);
    adminSessionToken = res.body.sessionToken;
  });

  // TEST 2: Change Admin Password Permanently
  await t.test('2. Admin changes password to a new permanent password', async () => {
    const res = await makeRequest(testServerObj.server, {
      method: 'POST',
      path: '/api/auth/change-password',
      headers: { Authorization: `Bearer ${adminSessionToken}` }
    }, {
      currentPassword: 'OriginalBootstrapPassword2026!',
      newPassword: 'MyNewPermanentAdminPassword2026!#',
      confirmNewPassword: 'MyNewPermanentAdminPassword2026!#'
    });
    assert.equal(res.statusCode, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.user.mustChangePassword, false);
  });

  // TEST 3: Old Bootstrap Password Is Now REJECTED
  await t.test('3. Bootstrap password is immediately REJECTED after password change', async () => {
    const res = await makeRequest(testServerObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: 'OriginalBootstrapPassword2026!'
    });
    assert.equal(res.statusCode, 401, 'Bootstrap password must not be accepted after change');
    assert.equal(res.body.code, 'INVALID_CREDENTIALS');
  });

  // TEST 4: New Password SUCCEEDS
  await t.test('4. Login with newly changed password succeeds', async () => {
    const res = await makeRequest(testServerObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: 'MyNewPermanentAdminPassword2026!#'
    });
    assert.equal(res.statusCode, 200);
    assert.equal(res.body.user.id, 'sg_karthi');
    assert.equal(res.body.user.mustChangePassword, false);
    adminSessionToken = res.body.sessionToken;
  });

  // TEST 5: Create Production Data (Member, Announcement, Team, Message)
  await t.test('5. Create critical production records in storage', async () => {
    // 5a. Signup member
    const signupRes = await makeRequest(testServerObj.server, {
      method: 'POST',
      path: '/api/auth/signup'
    }, {
      id: 'persistent_member_01',
      displayName: 'Permanent Member',
      password: 'MemberSecurePass2026!',
      confirmPassword: 'MemberSecurePass2026!'
    });
    assert.equal(signupRes.statusCode, 201);

    // 5b. Publish announcement
    const annRes = await makeRequest(testServerObj.server, {
      method: 'POST',
      path: '/api/announcements',
      headers: { Authorization: `Bearer ${adminSessionToken}` }
    }, {
      title: 'Persistent Server Update Notice',
      content: 'This announcement must survive container restarts and deployments.',
      category: 'IMPORTANT',
      isPinned: true
    });
    assert.equal(annRes.statusCode, 201);

    // 5c. Create team
    const teamRes = await makeRequest(testServerObj.server, {
      method: 'POST',
      path: '/api/teams',
      headers: { Authorization: `Bearer ${adminSessionToken}` }
    }, {
      name: 'Alpha Titans',
      description: 'The invincible clan'
    });
    assert.equal(teamRes.statusCode, 201);
  });

  // TEST 6: SERVER COLD RESTART / SIMULATED DEPLOYMENT
  await t.test('6. SIMULATE SERVER RESTART / REDEPLOYMENT: Verify changed password and data PERSIST', async () => {
    // Gracefully shut down existing server instance
    await new Promise(r => testServerObj.server.close(r));

    // Spin up completely new server instance from the persistent store file
    const restartedServerObj = createServer(tempStorePath);
    await new Promise(r => restartedServerObj.server.listen(0, '127.0.0.1', r));

    try {
      // 6a. Verify changed password STILL WORKS on restarted server
      const loginNewRes = await makeRequest(restartedServerObj.server, {
        method: 'POST',
        path: '/api/auth/login'
      }, {
        id: 'sg_karthi',
        password: 'MyNewPermanentAdminPassword2026!#'
      });
      assert.equal(loginNewRes.statusCode, 200, 'Changed password must survive server restart!');
      assert.equal(loginNewRes.body.user.id, 'sg_karthi');
      assert.equal(loginNewRes.body.user.mustChangePassword, false);
      const newAdminToken = loginNewRes.body.sessionToken;

      // 6b. Verify bootstrap password STILL REJECTED on restarted server
      const loginOldRes = await makeRequest(restartedServerObj.server, {
        method: 'POST',
        path: '/api/auth/login'
      }, {
        id: 'sg_karthi',
        password: 'OriginalBootstrapPassword2026!'
      });
      assert.equal(loginOldRes.statusCode, 401, 'Bootstrap password must NEVER be accepted after restart!');

      // 6c. Verify member account survived restart
      const memberLoginRes = await makeRequest(restartedServerObj.server, {
        method: 'POST',
        path: '/api/auth/login'
      }, {
        id: 'persistent_member_01',
        password: 'MemberSecurePass2026!'
      });
      assert.equal(memberLoginRes.statusCode, 200, 'Registered member must survive restart!');

      // 6d. Verify announcements survived restart
      const annListRes = await makeRequest(restartedServerObj.server, {
        method: 'GET',
        path: '/api/announcements'
      });
      assert.equal(annListRes.statusCode, 200);
      assert.ok(annListRes.body.announcements.some(a => a.title === 'Persistent Server Update Notice'), 'Announcement must survive restart!');

      // 6e. Verify teams survived restart
      const teamListRes = await makeRequest(restartedServerObj.server, {
        method: 'GET',
        path: '/api/teams'
      });
      assert.equal(teamListRes.statusCode, 200);
      assert.ok(teamListRes.body.teams.some(t => t.name.toLowerCase() === 'alpha titans'), 'Team must survive restart!');

      // 6f. Verify Admin Diagnostics panel endpoint
      const diagRes = await makeRequest(restartedServerObj.server, {
        method: 'GET',
        path: '/api/admin/diagnostics',
        headers: { Authorization: `Bearer ${newAdminToken}` }
      });
      assert.equal(diagRes.statusCode, 200);
      assert.equal(diagRes.body.databaseConnectionStatus, 'CONNECTED');
      assert.equal(diagRes.body.adminUniqueId, 'sg_karthi');
      assert.equal(diagRes.body.adminAccountStatus, 'PROVISIONED');
      assert.ok(diagRes.body.applicationVersion);
      assert.ok(diagRes.body.databaseMigrationStatus);
      assert.ok(diagRes.body.totalUsers >= 2);

      // 6g. Verify App Version check (Automatic Updater)
      const verRes = await makeRequest(restartedServerObj.server, {
        method: 'GET',
        path: '/api/app/version'
      });
      assert.equal(verRes.statusCode, 200);
      assert.equal(verRes.body.versionCode, 6);
      assert.equal(verRes.body.versionName, '1.5');
      assert.ok(verRes.body.apkUrl);
      assert.equal(verRes.body.forceUpdate, false);
      assert.equal(verRes.body.webVersion, '1.1.0');

      // 6h. Verify Database Export Backup endpoint
      const exportRes = await makeRequest(restartedServerObj.server, {
        method: 'GET',
        path: '/api/admin/database/export',
        headers: { Authorization: `Bearer ${newAdminToken}` }
      });
      assert.equal(exportRes.statusCode, 200);
      assert.ok(exportRes.body.data.users.length >= 2);
      assert.ok(exportRes.body.data.announcements.length >= 1);

      // 6i. Verify strict auth & role-based dashboard flow for Admin, Moderator, and Member
      // Unauthenticated dashboard & me requests must be rejected
      const unauthDash = await makeRequest(restartedServerObj.server, {
        method: 'GET',
        path: '/api/dashboard/summary'
      });
      assert.equal(unauthDash.statusCode, 401);

      // ID-only login (missing password) must be rejected
      const idOnlyRes = await makeRequest(restartedServerObj.server, {
        method: 'POST',
        path: '/api/auth/login'
      }, { id: 'sg_karthi' });
      assert.equal(idOnlyRes.statusCode, 400);

      // Create & promote a moderator for role testing
      const modSignup = await makeRequest(restartedServerObj.server, {
        method: 'POST',
        path: '/api/auth/signup'
      }, {
        id: 'strict_mod_01',
        displayName: 'Strict Mod',
        password: 'ModSecurePass2026!',
        confirmPassword: 'ModSecurePass2026!'
      });
      assert.equal(modSignup.statusCode, 201);

      const promoteRes = await makeRequest(restartedServerObj.server, {
        method: 'POST',
        path: '/api/settings/admin/users/strict_mod_01/role',
        headers: { Authorization: `Bearer ${newAdminToken}` }
      }, { role: 'MODERATOR' });
      assert.equal(promoteRes.statusCode, 200);

      const roleCases = [
        { id: 'sg_karthi', pass: 'MyNewPermanentAdminPassword2026!#', expectedRole: 'ADMIN' },
        { id: 'strict_mod_01', pass: 'ModSecurePass2026!', expectedRole: 'MODERATOR' },
        { id: 'persistent_member_01', pass: 'MemberSecurePass2026!', expectedRole: 'MEMBER' }
      ];

      for (const rc of roleCases) {
        // Wrong password rejected
        const badLogin = await makeRequest(restartedServerObj.server, {
          method: 'POST',
          path: '/api/auth/login'
        }, { id: rc.id, password: 'WrongPassword999!' });
        assert.equal(badLogin.statusCode, 401);

        // Correct password succeeds and loads dashboard
        const goodLogin = await makeRequest(restartedServerObj.server, {
          method: 'POST',
          path: '/api/auth/login'
        }, { id: rc.id, password: rc.pass });
        assert.equal(goodLogin.statusCode, 200);
        assert.equal(goodLogin.body.user.role, rc.expectedRole);

        const dash = await makeRequest(restartedServerObj.server, {
          method: 'GET',
          path: '/api/dashboard/summary',
          headers: { Authorization: `Bearer ${goodLogin.body.sessionToken}` }
        });
        assert.equal(dash.statusCode, 200);
        assert.equal(dash.body.role, rc.expectedRole);

        // Logout invalidates session
        const logout = await makeRequest(restartedServerObj.server, {
          method: 'POST',
          path: '/api/auth/logout',
          headers: { Authorization: `Bearer ${goodLogin.body.sessionToken}` }
        });
        assert.equal(logout.statusCode, 200);

        const afterLogoutDash = await makeRequest(restartedServerObj.server, {
          method: 'GET',
          path: '/api/dashboard/summary',
          headers: { Authorization: `Bearer ${goodLogin.body.sessionToken}` }
        });
        assert.equal(afterLogoutDash.statusCode, 401);
      }
    } finally {
      await new Promise(r => restartedServerObj.server.close(r));
    }
  });

  // TEST 7: STRICT ROLE-BASED PASSWORD PERMISSIONS & IN-MEMORY-DB (VERCEL) ADAPTER VERIFICATION
  await t.test('7. Strict role-based password permissions, HTTP 401/403 enforcement, and in-memory-db persistence', async () => {
    const { createInMemoryDatabase } = require('../src/in-memory-db');
    const { initSchema, seedInitialData } = require('../src/db');
    const http = require('node:http');
    const setupAuthRoutes = require('../src/routes/auth');
    const { hashToken } = require('../src/crypto-utils');

    const memStorePath = path.join(__dirname, 'temp_mem_security_store.json');
    if (fs.existsSync(memStorePath)) fs.unlinkSync(memStorePath);

    // Spin up a server backed explicitly by Vercel's in-memory-db adapter (isolated from repo seed file)
    const memDb = createInMemoryDatabase(memStorePath);
    memDb.restoreData({
      users: [], sessions: [], announcements: [], announcement_reads: [],
      conversations: [], messages: [], reward_configs: [], reward_claims: [],
      minecraft_player_stats: [], teams: [], team_members: [], team_invitations: [],
      app_settings: [], groups: [], group_members: [], group_messages: [],
      group_message_reads: [], audit_logs: [], schema_migrations: []
    });
    initSchema(memDb);
    seedInitialData(memDb);

    // Build minimal router using the same auth middleware logic as server.js
    const routes = [];
    const router = {
      get: (p, h) => routes.push({ method: 'GET', path: p, h }),
      post: (p, h) => routes.push({ method: 'POST', path: p, h }),
      put: (p, h) => routes.push({ method: 'PUT', path: p, h })
    };
    setupAuthRoutes(router, memDb);

    const memServer = http.createServer((req, res) => {
      res.json = function(d) { this.setHeader('Content-Type', 'application/json'); this.end(JSON.stringify(d)); };
      res.status = function(c) { this.statusCode = c; return this; };
      const parsed = new URL(req.url, 'http://localhost');
      req.pathname = parsed.pathname;
      req.query = Object.fromEntries(parsed.searchParams.entries());
      req.user = null;
      req.session = null;
      const auth = req.headers['authorization'];
      if (auth && auth.startsWith('Bearer ')) {
        const th = hashToken(auth.slice(7).trim());
        const s = memDb.prepare('SELECT id, user_id, expires_at, is_revoked FROM sessions WHERE token_hash = ? AND is_revoked = 0 AND expires_at > ?').get(th, Date.now());
        if (s) {
          const u = memDb.prepare('SELECT id, display_name, role, must_change_password, status, password_hash, created_at FROM users WHERE id = ?').get(s.user_id);
          if (u && u.status === 'ACTIVE') {
            req.session = s;
            req.user = u;
          }
        }
      }
      let body = '';
      req.on('data', c => { body += c; });
      req.on('end', () => {
        req.body = body ? JSON.parse(body) : {};
        for (const r of routes) {
          if (r.method !== req.method) continue;
          const regex = new RegExp('^' + r.path.replace(/:([a-zA-Z0-9_]+)/g, '([^/]+)') + '$');
          const m = req.pathname.match(regex);
          if (m) {
            req.params = {};
            const names = [...r.path.matchAll(/:([a-zA-Z0-9_]+)/g)].map(x => x[1]);
            names.forEach((n, idx) => { req.params[n] = decodeURIComponent(m[idx + 1]); });
            return r.h(req, res);
          }
        }
        res.status(404).json({ error: 'Not found' });
      });
    });

    await new Promise(r => memServer.listen(0, '127.0.0.1', r));

    try {
      // Login admin (dhanush - non-zero row index in users!)
      const adminLogin = await makeRequest(memServer, { method: 'POST', path: '/api/auth/login' }, {
        id: 'dhanush',
        password: process.env.ADMIN_BOOTSTRAP_PASSWORD || 'OriginalBootstrapPassword2026!'
      });
      assert.equal(adminLogin.statusCode, 200, 'Non-zero row admin login must succeed on in-memory-db');
      const dhanushToken = adminLogin.body.sessionToken;

      // Verify /api/auth/me works with token on in-memory-db (no false 401!)
      const dhanushMe = await makeRequest(memServer, {
        method: 'GET',
        path: '/api/auth/me',
        headers: { Authorization: `Bearer ${dhanushToken}` }
      });
      assert.equal(dhanushMe.statusCode, 200);
      assert.equal(dhanushMe.body.user.id, 'dhanush');

      // Signup two members & one moderator
      const m1 = await makeRequest(memServer, { method: 'POST', path: '/api/auth/signup' }, {
        id: 'mem_alpha', displayName: 'Alpha', password: 'AlphaPass2026!', confirmPassword: 'AlphaPass2026!'
      });
      assert.equal(m1.statusCode, 201);
      let mem1Token = m1.body.sessionToken;

      const m2 = await makeRequest(memServer, { method: 'POST', path: '/api/auth/signup' }, {
        id: 'mem_beta', displayName: 'Beta', password: 'BetaPass2026!', confirmPassword: 'BetaPass2026!'
      });
      assert.equal(m2.statusCode, 201);

      const modSignup = await makeRequest(memServer, { method: 'POST', path: '/api/auth/signup' }, {
        id: 'mod_gamma', displayName: 'Gamma', password: 'GammaPass2026!', confirmPassword: 'GammaPass2026!'
      });
      assert.equal(modSignup.statusCode, 201);
      memDb.prepare("UPDATE users SET role = 'MODERATOR' WHERE id = ?").run('mod_gamma');
      let modToken = modSignup.body.sessionToken;

      // Test 1: Unauthenticated password change -> HTTP 401
      const unauthChg = await makeRequest(memServer, { method: 'POST', path: '/api/auth/change-password' }, {
        userId: 'mem_alpha', currentPassword: 'AlphaPass2026!', newPassword: 'NewAlpha2026!', confirmNewPassword: 'NewAlpha2026!'
      });
      assert.equal(unauthChg.statusCode, 401);

      // Test 2: Member changes another member's password -> Denied (403)
      const memOther = await makeRequest(memServer, {
        method: 'POST', path: '/api/auth/change-password', headers: { Authorization: `Bearer ${mem1Token}` }
      }, {
        userId: 'mem_beta', currentPassword: 'BetaPass2026!', newPassword: 'HackedPass2026!', confirmNewPassword: 'HackedPass2026!'
      });
      assert.equal(memOther.statusCode, 403);

      // Test 3: Member changes admin password -> Denied (403)
      const memAdmin = await makeRequest(memServer, {
        method: 'POST', path: '/api/auth/change-password', headers: { Authorization: `Bearer ${mem1Token}` }
      }, {
        userId: 'dhanush', currentPassword: 'OriginalBootstrapPassword2026!', newPassword: 'HackedAdmin2026!', confirmNewPassword: 'HackedAdmin2026!'
      });
      assert.equal(memAdmin.statusCode, 403);

      // Test 4: Moderator changes admin password -> Denied (403)
      const modAdmin = await makeRequest(memServer, {
        method: 'POST', path: '/api/auth/change-password', headers: { Authorization: `Bearer ${modToken}` }
      }, {
        userId: 'dhanush', currentPassword: 'OriginalBootstrapPassword2026!', newPassword: 'HackedAdmin2026!', confirmNewPassword: 'HackedAdmin2026!'
      });
      assert.equal(modAdmin.statusCode, 403);

      // Test 5: Incorrect current password -> Rejected (401)
      const wrongCur = await makeRequest(memServer, {
        method: 'POST', path: '/api/auth/change-password', headers: { Authorization: `Bearer ${mem1Token}` }
      }, {
        currentPassword: 'WrongPassword!', newPassword: 'NewAlpha2026!', confirmNewPassword: 'NewAlpha2026!'
      });
      assert.equal(wrongCur.statusCode, 401);

      // Test 6: Member changes own password -> Allowed & requires re-login
      const memOwn = await makeRequest(memServer, {
        method: 'POST', path: '/api/auth/change-password', headers: { Authorization: `Bearer ${mem1Token}` }
      }, {
        currentPassword: 'AlphaPass2026!', newPassword: 'NewAlphaPass2026!', confirmNewPassword: 'NewAlphaPass2026!'
      });
      assert.equal(memOwn.statusCode, 200);
      assert.equal(memOwn.body.requiresReLogin, true);

      // Old password rejected, new password succeeds
      const oldPassLogin = await makeRequest(memServer, { method: 'POST', path: '/api/auth/login' }, {
        id: 'mem_alpha', password: 'AlphaPass2026!'
      });
      assert.equal(oldPassLogin.statusCode, 401);

      const newPassLogin = await makeRequest(memServer, { method: 'POST', path: '/api/auth/login' }, {
        id: 'mem_alpha', password: 'NewAlphaPass2026!'
      });
      assert.equal(newPassLogin.statusCode, 200);
      mem1Token = newPassLogin.body.sessionToken;

      // Test 7: Moderator changes own password -> Allowed
      const modOwn = await makeRequest(memServer, {
        method: 'POST', path: '/api/auth/change-password', headers: { Authorization: `Bearer ${modToken}` }
      }, {
        currentPassword: 'GammaPass2026!', newPassword: 'NewGammaPass2026!', confirmNewPassword: 'NewGammaPass2026!'
      });
      assert.equal(modOwn.statusCode, 200);
      const modReLogin = await makeRequest(memServer, { method: 'POST', path: '/api/auth/login' }, {
        id: 'mod_gamma', password: 'NewGammaPass2026!'
      });
      assert.equal(modReLogin.statusCode, 200);

      // Test 8: Admin changes own password -> Allowed
      const admOwn = await makeRequest(memServer, {
        method: 'POST', path: '/api/auth/change-password', headers: { Authorization: `Bearer ${dhanushToken}` }
      }, {
        currentPassword: process.env.ADMIN_BOOTSTRAP_PASSWORD || 'OriginalBootstrapPassword2026!',
        newPassword: 'NewDhanushPass2026!',
        confirmNewPassword: 'NewDhanushPass2026!'
      });
      assert.equal(admOwn.statusCode, 200);
      const admReLogin = await makeRequest(memServer, { method: 'POST', path: '/api/auth/login' }, {
        id: 'dhanush', password: 'NewDhanushPass2026!'
      });
      assert.equal(admReLogin.statusCode, 200);
      const freshAdminToken = admReLogin.body.sessionToken;

      // Test 9: Member & Moderator cannot access admin password-reset endpoint -> Denied (403)
      const memResetAttempt = await makeRequest(memServer, {
        method: 'POST', path: '/api/admin/users/mem_beta/reset-password', headers: { Authorization: `Bearer ${mem1Token}` }
      }, { newPassword: 'ResetPass2026!', confirmNewPassword: 'ResetPass2026!' });
      assert.equal(memResetAttempt.statusCode, 403);

      const modResetAttempt = await makeRequest(memServer, {
        method: 'POST', path: '/api/admin/users/mem_beta/reset-password', headers: { Authorization: `Bearer ${modReLogin.body.sessionToken}` }
      }, { newPassword: 'ResetPass2026!', confirmNewPassword: 'ResetPass2026!' });
      assert.equal(modResetAttempt.statusCode, 403);

      // Test 10: Admin resets another user's password via separate endpoint + audit log created
      const admReset = await makeRequest(memServer, {
        method: 'POST', path: '/api/admin/users/mem_beta/reset-password', headers: { Authorization: `Bearer ${freshAdminToken}` }
      }, { newPassword: 'AdminResetBeta2026!', confirmNewPassword: 'AdminResetBeta2026!' });
      assert.equal(admReset.statusCode, 200);
      assert.equal(memDb.prepare('SELECT COUNT(*) AS cnt FROM audit_logs WHERE target_user_id = ?').get('mem_beta').cnt, 1);

      const betaLogin = await makeRequest(memServer, { method: 'POST', path: '/api/auth/login' }, {
        id: 'mem_beta', password: 'AdminResetBeta2026!'
      });
      assert.equal(betaLogin.statusCode, 200);

      // Test 11: Database update failure produces HTTP 500 and no false success message
      const origPrepare = memDb.prepare.bind(memDb);
      memDb.prepare = (sql) => {
        if (sql.includes('UPDATE users SET password_hash')) {
          return { run: () => ({ changes: 0 }) };
        }
        return origPrepare(sql);
      };
      const failUpdate = await makeRequest(memServer, {
        method: 'POST', path: '/api/auth/change-password', headers: { Authorization: `Bearer ${mem1Token}` }
      }, {
        currentPassword: 'NewAlphaPass2026!', newPassword: 'AnotherPass2026!', confirmNewPassword: 'AnotherPass2026!'
      });
      memDb.prepare = origPrepare;
      assert.equal(failUpdate.statusCode, 500);
      assert.equal(failUpdate.body.success, false);
    } finally {
      await new Promise(r => memServer.close(r));
      if (fs.existsSync(memStorePath)) fs.unlinkSync(memStorePath);
    }
  });
});
