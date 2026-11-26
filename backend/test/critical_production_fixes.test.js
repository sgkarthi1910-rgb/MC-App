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
      assert.equal(verRes.body.versionCode, 4);
      assert.equal(verRes.body.versionName, '1.3');
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
});
