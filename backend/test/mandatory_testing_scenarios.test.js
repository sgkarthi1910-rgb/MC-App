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

test('MANDATORY USER TESTING SCENARIOS (A, B, C, D)', async (t) => {
  const tempStorePath = path.join(__dirname, 'mandatory_test_store.json');
  if (fs.existsSync(tempStorePath)) fs.unlinkSync(tempStorePath);

  process.env.ADMIN_BOOTSTRAP_PASSWORD = 'TgzAdminInitial2026!Secure';
  process.env.DATABASE_PATH = tempStorePath;

  let serverObj = createServer(tempStorePath);
  await new Promise(r => serverObj.server.listen(0, '127.0.0.1', r));

  t.after(() => {
    try { serverObj.server.close(); } catch(e){}
    try { if (fs.existsSync(tempStorePath)) fs.unlinkSync(tempStorePath); } catch(e){}
  });

  let newAdminPassword = 'PermanentAdminPassword9988!@';

  // ==========================================
  // TEST A — Password persistence
  // ==========================================
  await t.test('Test A: Password persistence through logout, login, and full restart', async () => {
    // 1. Log in with current bootstrap password
    const loginBootstrap = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: 'TgzAdminInitial2026!Secure'
    });
    assert.equal(loginBootstrap.statusCode, 200);
    assert.equal(loginBootstrap.body.user.id, 'sg_karthi');
    const initialToken = loginBootstrap.body.sessionToken;

    // 2. Change the admin password
    const changeRes = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/change-password',
      headers: { Authorization: `Bearer ${initialToken}` }
    }, {
      currentPassword: 'TgzAdminInitial2026!Secure',
      newPassword: newAdminPassword,
      confirmNewPassword: newAdminPassword
    });
    assert.equal(changeRes.statusCode, 200);
    assert.equal(changeRes.body.user.mustChangePassword, false);

    // 3. Log out
    const logoutRes = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/logout',
      headers: { Authorization: `Bearer ${initialToken}` }
    });
    assert.equal(logoutRes.statusCode, 200);

    // 4. Log in using the new password
    const loginNew = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: newAdminPassword
    });
    assert.equal(loginNew.statusCode, 200);
    assert.equal(loginNew.body.user.id, 'sg_karthi');
    assert.equal(loginNew.body.user.mustChangePassword, false);

    // 5. Confirm the old bootstrap password is rejected
    const loginOldRejected = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: 'TgzAdminInitial2026!Secure'
    });
    assert.equal(loginOldRejected.statusCode, 401, 'Old bootstrap password must be rejected');

    // 6. Close and reopen the app (simulate complete container reboot from persistent disk)
    await new Promise(r => serverObj.server.close(r));
    serverObj = createServer(tempStorePath);
    await new Promise(r => serverObj.server.listen(0, '127.0.0.1', r));

    // 7. Confirm the new password still works after restart
    const loginAfterRestart = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: newAdminPassword
    });
    assert.equal(loginAfterRestart.statusCode, 200);
    assert.equal(loginAfterRestart.body.user.id, 'sg_karthi');
    assert.equal(loginAfterRestart.body.user.mustChangePassword, false);

    // Confirm old password is still rejected after restart
    const oldAgain = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: 'TgzAdminInitial2026!Secure'
    });
    assert.equal(oldAgain.statusCode, 401);
  });

  // ==========================================
  // TEST B — Session restoration
  // ==========================================
  await t.test('Test B: Session restoration loads full admin profile, role, and dashboard', async () => {
    // 1. Log in as admin
    const loginRes = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: newAdminPassword
    });
    assert.equal(loginRes.statusCode, 200);
    const token = loginRes.body.sessionToken;

    // 2 & 3. Simulate reopening app: restore session via /api/auth/me
    const meRes = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/auth/me',
      headers: { Authorization: `Bearer ${token}` }
    });
    assert.equal(meRes.statusCode, 200);
    assert.ok(meRes.body.user);
    // 4. Confirm user ID & profile
    assert.equal(meRes.body.user.id, 'sg_karthi');
    // 5. Confirm role is ADMIN
    assert.equal(meRes.body.user.role, 'ADMIN');

    // 6 & 7. Load dashboard data for admin
    const dashRes = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/dashboard/summary',
      headers: { Authorization: `Bearer ${token}` }
    });
    assert.equal(dashRes.statusCode, 200);
    assert.ok(dashRes.body.adminStats, 'Admin stats must be present in dashboard summary');
    assert.ok(typeof dashRes.body.adminStats.totalMembers === 'number');
    assert.ok(typeof dashRes.body.adminStats.activeSessions === 'number');

    // 8. Load diagnostic data
    const diagRes = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/admin/diagnostics',
      headers: { Authorization: `Bearer ${token}` }
    });
    assert.equal(diagRes.statusCode, 200);
    assert.equal(diagRes.body.currentAccountId, 'sg_karthi');
    assert.equal(diagRes.body.currentAccountRole, 'ADMIN');
    assert.equal(diagRes.body.passwordStatus, 'SAVED_PERMANENT');
    assert.equal(diagRes.body.persistentStorageStatus, 'CONNECTED');
  });

  // ==========================================
  // TEST C — Data persistence
  // ==========================================
  await t.test('Test C: Data persistence across refreshes and restarts', async () => {
    // 1. Log in as admin
    const loginRes = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: newAdminPassword
    });
    const token = loginRes.body.sessionToken;

    // Create a new member, an announcement, and a team
    await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/signup'
    }, {
      id: 'scenario_c_member',
      displayName: 'Scenario C Member',
      password: 'MemberPassword123!',
      confirmPassword: 'MemberPassword123!'
    });

    await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/announcements',
      headers: { Authorization: `Bearer ${token}` }
    }, {
      title: 'Persistent Announcement C',
      content: 'Must persist across restarts',
      category: 'GENERAL'
    });

    await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/teams',
      headers: { Authorization: `Bearer ${token}` }
    }, {
      name: 'Scenario C Clan',
      description: 'Clan for testing persistence'
    });

    // 2. Check registered members before restart
    const membersBefore = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/settings/admin/users',
      headers: { Authorization: `Bearer ${token}` }
    });
    assert.equal(membersBefore.statusCode, 200);
    assert.ok(membersBefore.body.users.some(u => u.id === 'scenario_c_member'));

    // 3 & 4. Restart server
    await new Promise(r => serverObj.server.close(r));
    serverObj = createServer(tempStorePath);
    await new Promise(r => serverObj.server.listen(0, '127.0.0.1', r));

    // 5. Confirm the same information is still available after restart
    const membersAfter = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/settings/admin/users',
      headers: { Authorization: `Bearer ${token}` }
    });
    assert.equal(membersAfter.statusCode, 200);
    assert.ok(membersAfter.body.users.some(u => u.id === 'scenario_c_member'), 'Member must still exist after restart');

    const annAfter = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/announcements'
    });
    assert.equal(annAfter.statusCode, 200);
    assert.ok(annAfter.body.announcements.some(a => a.title === 'Persistent Announcement C'), 'Announcement must still exist after restart');

    const teamsAfter = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/teams'
    });
    assert.equal(teamsAfter.statusCode, 200);
    assert.ok(teamsAfter.body.teams.some(t => t.name === 'Scenario C Clan'), 'Team must still exist after restart');
  });

  // ==========================================
  // TEST D — Role verification
  // ==========================================
  await t.test('Test D: Role separation, member cannot access admin endpoints', async () => {
    // 1. Log in as member
    const memberLogin = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'scenario_c_member',
      password: 'MemberPassword123!'
    });
    assert.equal(memberLogin.statusCode, 200);
    assert.equal(memberLogin.body.user.role, 'MEMBER');
    const memberToken = memberLogin.body.sessionToken;

    // 2. Member dashboard summary check (should have memberStats, NOT adminStats)
    const memberDash = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/dashboard/summary',
      headers: { Authorization: `Bearer ${memberToken}` }
    });
    assert.equal(memberDash.statusCode, 200);
    assert.equal(memberDash.body.role, 'MEMBER');
    assert.equal(memberDash.body.adminStats, undefined, 'Member must not receive adminStats');

    // 3. Confirm member CANNOT access admin-only screens / endpoints
    const memberAdminUsers = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/settings/admin/users',
      headers: { Authorization: `Bearer ${memberToken}` }
    });
    assert.equal(memberAdminUsers.statusCode, 403, 'Member must be forbidden from accessing admin users');

    const memberAdminDiag = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/admin/diagnostics',
      headers: { Authorization: `Bearer ${memberToken}` }
    });
    assert.equal(memberAdminDiag.statusCode, 403, 'Member must be forbidden from admin diagnostics');

    const memberExportDb = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/admin/database/export',
      headers: { Authorization: `Bearer ${memberToken}` }
    });
    assert.equal(memberExportDb.statusCode, 403, 'Member must be forbidden from exporting DB');

    // 4. Log in as admin
    const adminLogin = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: newAdminPassword
    });
    assert.equal(adminLogin.statusCode, 200);
    assert.equal(adminLogin.body.user.role, 'ADMIN');
    const adminToken = adminLogin.body.sessionToken;

    // 5. Admin CAN access admin endpoints
    const adminDiag = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/admin/diagnostics',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(adminDiag.statusCode, 200);
    assert.equal(adminDiag.body.currentAccountRole, 'ADMIN');
  });
});
