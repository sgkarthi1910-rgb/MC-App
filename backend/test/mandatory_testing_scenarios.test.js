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

test('CRITICAL PRODUCTION MANDATORY TESTING SCENARIOS (A, B, C, D, E)', async (t) => {
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

  const permanentAdminPassword = 'PermanentAdminPassword9988!@';

  // ==========================================
  // SETUP: Provision & Authenticate Admin
  // ==========================================
  const adminInitialLogin = await makeRequest(serverObj.server, {
    method: 'POST',
    path: '/api/auth/login'
  }, {
    id: 'sg_karthi',
    password: 'TgzAdminInitial2026!Secure'
  });
  assert.equal(adminInitialLogin.statusCode, 200);
  let adminToken = adminInitialLogin.body.sessionToken;

  // Change admin password to permanent
  const changeRes = await makeRequest(serverObj.server, {
    method: 'POST',
    path: '/api/auth/change-password',
    headers: { Authorization: `Bearer ${adminToken}` }
  }, {
    currentPassword: 'TgzAdminInitial2026!Secure',
    newPassword: permanentAdminPassword,
    confirmNewPassword: permanentAdminPassword
  });
  assert.equal(changeRes.statusCode, 200);

  // Relogin admin with permanent password
  const adminRelogin = await makeRequest(serverObj.server, {
    method: 'POST',
    path: '/api/auth/login'
  }, {
    id: 'sg_karthi',
    password: permanentAdminPassword
  });
  assert.equal(adminRelogin.statusCode, 200);
  adminToken = adminRelogin.body.sessionToken;

  // ==========================================
  // TEST A — Persistent account
  // 1. Register a new member.
  // 2. Verify the account exists in the production database.
  // 3. Log in.
  // 4. Close the browser completely.
  // 5. Reopen the app.
  // 6. Confirm the user is still logged in.
  // 7. Confirm the correct dashboard and profile load.
  // ==========================================
  await t.test('Test A — Persistent account', async () => {
    // 1. Register new member
    const signupRes = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/signup'
    }, {
      id: 'persist_member_a',
      displayName: 'Persistent Member Alpha',
      password: 'SecurePassword123!',
      confirmPassword: 'SecurePassword123!'
    });
    assert.equal(signupRes.statusCode, 201);
    assert.equal(signupRes.body.user.id, 'persist_member_a');

    // Verify Set-Cookie header contains Max-Age, Expires, HttpOnly, and SameSite
    const setCookie = signupRes.headers['set-cookie']?.[0] || '';
    assert.ok(setCookie.includes('tgz_session='));
    assert.ok(setCookie.includes('Max-Age='));
    assert.ok(setCookie.includes('Expires='));
    assert.ok(setCookie.includes('HttpOnly'));
    assert.ok(setCookie.includes('SameSite=Lax'));

    // 2. Verify account exists in permanent database
    const dbUser = serverObj.db.prepare('SELECT id, display_name, role, status FROM users WHERE id = ?').get('persist_member_a');
    assert.ok(dbUser, 'User must exist in permanent database');
    assert.equal(dbUser.id, 'persist_member_a');
    assert.equal(dbUser.role, 'MEMBER');

    // 3. Log in
    const loginRes = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'persist_member_a',
      password: 'SecurePassword123!'
    });
    assert.equal(loginRes.statusCode, 200);
    const memberToken = loginRes.body.sessionToken;
    const loginCookie = loginRes.headers['set-cookie']?.[0] || '';
    assert.ok(loginCookie.includes('Expires='));

    // 4. Close the browser/application completely (Simulate serverless cold boot / app process kill)
    await new Promise(r => serverObj.server.close(r));
    serverObj = createServer(tempStorePath);
    await new Promise(r => serverObj.server.listen(0, '127.0.0.1', r));

    // 5. Reopen the application and restore session via token/cookie
    const reopenRes = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/auth/me',
      headers: {
        Authorization: `Bearer ${memberToken}`,
        Cookie: `tgz_session=${memberToken}`
      }
    });

    // 6. Confirm the user is still logged in
    assert.equal(reopenRes.statusCode, 200, 'Session must remain valid across app restarts');
    assert.equal(reopenRes.body.user.id, 'persist_member_a');
    assert.equal(reopenRes.body.user.role, 'MEMBER');

    // 7. Confirm the correct dashboard and profile load
    const dashRes = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/dashboard/summary',
      headers: { Authorization: `Bearer ${memberToken}` }
    });
    assert.equal(dashRes.statusCode, 200);
    assert.equal(dashRes.body.role, 'MEMBER');
    assert.ok(dashRes.body.memberStats, 'Member stats must load properly');
    assert.equal(dashRes.body.adminStats, undefined, 'Member must not see admin stats');
  });

  // ==========================================
  // TEST B — Admin member visibility
  // 1. Register multiple test members.
  // 2. Log in as admin.
  // 3. Open the admin member-management page.
  // 4. Confirm all registered accounts appear.
  // 5. Refresh the page.
  // 6. Confirm the same members remain visible.
  // 7. Verify that search and filtering work.
  // ==========================================
  await t.test('Test B — Admin member visibility', async () => {
    // 1. Register multiple test members
    const testMembers = ['member_b_1', 'member_b_2', 'member_b_3'];
    for (const mId of testMembers) {
      const reg = await makeRequest(serverObj.server, {
        method: 'POST',
        path: '/api/auth/signup'
      }, {
        id: mId,
        displayName: `Test Display ${mId}`,
        password: 'Password123!',
        confirmPassword: 'Password123!'
      });
      assert.equal(reg.statusCode, 201);
    }

    // 2. Log in as admin
    const adminLogin = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: permanentAdminPassword
    });
    assert.equal(adminLogin.statusCode, 200);
    adminToken = adminLogin.body.sessionToken;

    // 3. Open the admin member-management page
    const listRes1 = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/settings/admin/users',
      headers: { Authorization: `Bearer ${adminToken}` }
    });

    // 4. Confirm all registered accounts appear
    assert.equal(listRes1.statusCode, 200);
    assert.ok(Array.isArray(listRes1.body.users));
    for (const mId of testMembers) {
      const found = listRes1.body.users.find(u => u.id === mId);
      assert.ok(found, `Member ${mId} must appear in admin member list`);
      assert.equal(found.role, 'MEMBER');
      assert.equal(found.status, 'ACTIVE');
      assert.ok(found.created_at > 0);
      // Ensure NO password hash or token is exposed in admin member list!
      assert.equal(found.password_hash, undefined);
      assert.equal(found.token, undefined);
    }

    // 5. Refresh the page (fetch again)
    const listRes2 = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/settings/admin/users',
      headers: { Authorization: `Bearer ${adminToken}` }
    });

    // 6. Confirm the same members remain visible
    assert.equal(listRes2.statusCode, 200);
    assert.equal(listRes2.body.users.length, listRes1.body.users.length);

    // 7. Verify search and filtering
    const searchRes = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/settings/admin/users?search=member_b_1',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(searchRes.statusCode, 200);
    assert.equal(searchRes.body.users.length, 1);
    assert.equal(searchRes.body.users[0].id, 'member_b_1');

    const filterRoleRes = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/settings/admin/users?role=MEMBER',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(filterRoleRes.statusCode, 200);
    assert.ok(filterRoleRes.body.users.every(u => u.role === 'MEMBER'));
  });

  // ==========================================
  // TEST C — Production persistence
  // 1. Add a test announcement.
  // 2. Add a test team.
  // 3. Register a test account.
  // 4. Deploy a new application version (simulate redeploy/cold boot).
  // 5. Confirm all records remain available after deployment.
  // ==========================================
  await t.test('Test C — Production persistence', async () => {
    // 1. Add test announcement
    const annRes = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/announcements',
      headers: { Authorization: `Bearer ${adminToken}` }
    }, {
      title: 'Persistent Server Update C',
      content: 'Production database persistence verified across redeployments.',
      category: 'UPDATE',
      isPinned: true
    });
    assert.equal(annRes.statusCode, 201);

    // 2. Add test team
    const teamRes = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/teams',
      headers: { Authorization: `Bearer ${adminToken}` }
    }, {
      name: 'Vanguard Legends C',
      description: 'Persistent Clan C'
    });
    assert.equal(teamRes.statusCode, 201);

    // 3. Register test account
    const accRes = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/signup'
    }, {
      id: 'deploy_survivor_c',
      displayName: 'Deploy Survivor C',
      password: 'DeployPassword123!',
      confirmPassword: 'DeployPassword123!'
    });
    assert.equal(accRes.statusCode, 201);

    // 4. Simulate application redeployment / serverless container recycling
    await new Promise(r => serverObj.server.close(r));
    serverObj = createServer(tempStorePath);
    await new Promise(r => serverObj.server.listen(0, '127.0.0.1', r));

    // 5. Confirm all records remain available after deployment
    const annCheck = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/announcements'
    });
    assert.equal(annCheck.statusCode, 200);
    assert.ok(annCheck.body.announcements.some(a => a.title === 'Persistent Server Update C'));

    const teamCheck = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/teams'
    });
    assert.equal(teamCheck.statusCode, 200);
    assert.ok(teamCheck.body.teams.some(t => t.name === 'Vanguard Legends C'));

    const userCheck = serverObj.db.prepare('SELECT id, display_name FROM users WHERE id = ?').get('deploy_survivor_c');
    assert.ok(userCheck, 'User record must persist across redeployments');
    assert.equal(userCheck.id, 'deploy_survivor_c');
  });

  // ==========================================
  // TEST D — Logout
  // 1. Click Logout.
  // 2. Reopen the app.
  // 3. Confirm the login screen appears (401 on protected me API).
  // 4. Confirm protected dashboard APIs reject unauthenticated requests.
  // 5. Log in again and verify the correct dashboard loads.
  // ==========================================
  await t.test('Test D — Logout', async () => {
    // Member logs in
    const memberLogin = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'deploy_survivor_c',
      password: 'DeployPassword123!'
    });
    assert.equal(memberLogin.statusCode, 200);
    const activeMemberToken = memberLogin.body.sessionToken;

    // 1. Click Logout
    const logoutRes = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/logout',
      headers: { Authorization: `Bearer ${activeMemberToken}` }
    });
    assert.equal(logoutRes.statusCode, 200);
    const logoutCookie = logoutRes.headers['set-cookie']?.[0] || '';
    assert.ok(logoutCookie.includes('Max-Age=0'));
    assert.ok(logoutCookie.includes('Expires=Thu, 01 Jan 1970'));

    // 2 & 3. Reopen app: attempt accessing protected session
    const meAfterLogout = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/auth/me',
      headers: { Authorization: `Bearer ${activeMemberToken}` }
    });
    assert.equal(meAfterLogout.statusCode, 401, 'Logged out session must be rejected');

    // 4. Confirm protected dashboard APIs reject unauthenticated requests
    const dashUnauth = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/dashboard/summary',
      headers: { Authorization: `Bearer ${activeMemberToken}` }
    });
    assert.equal(dashUnauth.statusCode, 401, 'Dashboard API must reject revoked session');

    // 5. Log in again and verify correct dashboard loads
    const relogin = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'deploy_survivor_c',
      password: 'DeployPassword123!'
    });
    assert.equal(relogin.statusCode, 200);
    const newToken = relogin.body.sessionToken;

    const dashAuth = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/dashboard/summary',
      headers: { Authorization: `Bearer ${newToken}` }
    });
    assert.equal(dashAuth.statusCode, 200);
    assert.equal(dashAuth.body.role, 'MEMBER');
    assert.ok(dashAuth.body.memberStats);
  });

  // ==========================================
  // TEST E — Authorization
  // Verify that Members cannot access the admin member-list API or change another user's password.
  // ==========================================
  await t.test('Test E — Authorization', async () => {
    // 1. Member login
    const memberLogin = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'deploy_survivor_c',
      password: 'DeployPassword123!'
    });
    assert.equal(memberLogin.statusCode, 200);
    const memberToken = memberLogin.body.sessionToken;

    // 2. Member CANNOT access admin member-list API
    const memberListAttempt = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/settings/admin/users',
      headers: { Authorization: `Bearer ${memberToken}` }
    });
    assert.equal(memberListAttempt.statusCode, 403, 'Member must be forbidden from accessing admin member list');

    // 3. Member CANNOT access admin diagnostics
    const diagAttempt = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/admin/diagnostics',
      headers: { Authorization: `Bearer ${memberToken}` }
    });
    assert.equal(diagAttempt.statusCode, 403, 'Member must be forbidden from accessing admin diagnostics');

    // 4. Member CANNOT access database backup export
    const exportAttempt = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/admin/database/export',
      headers: { Authorization: `Bearer ${memberToken}` }
    });
    assert.equal(exportAttempt.statusCode, 403, 'Member must be forbidden from accessing database export');

    // 5. Admin CAN access all admin endpoints
    const adminRelogin = await makeRequest(serverObj.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: permanentAdminPassword
    });
    assert.equal(adminRelogin.statusCode, 200);
    const validAdminToken = adminRelogin.body.sessionToken;

    const adminDiagSuccess = await makeRequest(serverObj.server, {
      method: 'GET',
      path: '/api/admin/diagnostics',
      headers: { Authorization: `Bearer ${validAdminToken}` }
    });
    assert.equal(adminDiagSuccess.statusCode, 200);
    assert.equal(adminDiagSuccess.body.currentAccountRole, 'ADMIN');
    assert.equal(adminDiagSuccess.body.databaseConnectionStatus, 'CONNECTED');
    assert.ok(typeof adminDiagSuccess.body.totalRegisteredAccounts === 'number');
    assert.ok(adminDiagSuccess.body.sessionValidationStatus);
  });
});
