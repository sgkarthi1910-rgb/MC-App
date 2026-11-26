const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { createServer } = require('../src/server');

// Helper for making test HTTP requests against server
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
        try {
          body = data ? JSON.parse(data) : null;
        } catch (e) {
          body = data;
        }
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body
        });
      });
    });

    req.on('error', reject);
    if (payload) {
      req.write(typeof payload === 'string' ? payload : JSON.stringify(payload));
    }
    req.end();
  });
}

test('TGZ Lifesteal Complete Backend Test Suite', async (t) => {
  process.env.ADMIN_BOOTSTRAP_PASSWORD = 'TestBootstrapAdmin2026!';
  process.env.ADMIN_FORCE_PASSWORD_CHANGE = 'true';
  process.env.MINECRAFT_PLUGIN_API_KEY = 'test_plugin_key_12345';

  const { server, db } = createServer(':memory:');
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));

  let adminToken = null;
  let memberToken1 = null;
  let memberToken2 = null;

  t.after(() => {
    server.close();
  });

  // 1. Initial Admin Account Provisioning & Login
  await t.test('1. Initial admin accounts (sg_karthi, dhanush, lmtg, darksight) exist and require password change on bootstrap login', async () => {
    // Verify each configured admin account can log in with bootstrap password
    for (const admId of ['dhanush', 'lmtg', 'darksight']) {
      const aRes = await makeRequest(server, {
        method: 'POST',
        path: '/api/auth/login'
      }, {
        id: admId,
        password: 'TestBootstrapAdmin2026!'
      });
      assert.equal(aRes.statusCode, 200, `Failed login for admin ${admId}`);
      assert.equal(aRes.body.user.id, admId);
      assert.equal(aRes.body.user.role, 'ADMIN');
      assert.equal(aRes.body.user.mustChangePassword, true);
      assert.ok(aRes.body.sessionToken);
    }

    const res = await makeRequest(server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: 'TestBootstrapAdmin2026!'
    });

    assert.equal(res.statusCode, 200);
    assert.equal(res.body.user.id, 'sg_karthi');
    assert.equal(res.body.user.role, 'ADMIN');
    assert.equal(res.body.user.mustChangePassword, true);
    assert.ok(res.body.sessionToken);

    adminToken = res.body.sessionToken;

    // Change password to clear must_change_password
    const changeRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/auth/change-password',
      headers: { Authorization: `Bearer ${adminToken}` }
    }, {
      currentPassword: 'TestBootstrapAdmin2026!',
      newPassword: 'PermanentAdminPassword2026!',
      confirmNewPassword: 'PermanentAdminPassword2026!'
    });

    assert.equal(changeRes.statusCode, 200);
    assert.equal(changeRes.body.success, true);
    assert.equal(changeRes.body.requiresReLogin, true);

    // Old session must be invalidated after password change
    const oldMeRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/auth/me',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(oldMeRes.statusCode, 401);

    // Log in again with the new password
    const reLoginRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: 'PermanentAdminPassword2026!'
    });
    assert.equal(reLoginRes.statusCode, 200);
    adminToken = reLoginRes.body.sessionToken;

    // Verify mustChangePassword is now false
    const meRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/auth/me',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(meRes.statusCode, 200);
    assert.equal(meRes.body.user.mustChangePassword, false);
  });

  // 2. Member Signup & Validation
  await t.test('2. Member signup enforces unique ID, password validation, and prevents admin creation', async () => {
    // Signup user1
    const res1 = await makeRequest(server, {
      method: 'POST',
      path: '/api/auth/signup'
    }, {
      id: 'player_hunter',
      displayName: 'Hunter Champion',
      password: 'PlayerPassword123!',
      confirmPassword: 'PlayerPassword123!'
    });

    assert.equal(res1.statusCode, 201);
    assert.equal(res1.body.user.id, 'player_hunter');
    assert.equal(res1.body.user.role, 'MEMBER');
    assert.ok(res1.body.sessionToken);
    memberToken1 = res1.body.sessionToken;

    // Duplicate ID rejection
    const dupRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/auth/signup'
    }, {
      id: 'player_hunter',
      password: 'AnotherPassword123!',
      confirmPassword: 'AnotherPassword123!'
    });
    assert.equal(dupRes.statusCode, 409);

    // Reserved Admin ID rejection
    for (const reservedAdmin of ['sg_karthi', 'dhanush', 'lmtg', 'darksight']) {
      const adminDupRes = await makeRequest(server, {
        method: 'POST',
        path: '/api/auth/signup'
      }, {
        id: reservedAdmin,
        password: 'Password123!',
        confirmPassword: 'Password123!'
      });
      assert.equal(adminDupRes.statusCode, 409, `Signup should reject reserved admin ID ${reservedAdmin}`);
    }

    // Signup user2
    const res2 = await makeRequest(server, {
      method: 'POST',
      path: '/api/auth/signup'
    }, {
      id: 'player_shield',
      displayName: 'Shield Guardian',
      password: 'ShieldPassword123!',
      confirmPassword: 'ShieldPassword123!'
    });
    assert.equal(res2.statusCode, 201);
    memberToken2 = res2.body.sessionToken;
  });

  // 3. Persistent Sessions & Revocation
  await t.test('3. Persistent login sessions, session listing and revocation', async () => {
    // List sessions for member 1
    const sessRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/auth/sessions',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(sessRes.statusCode, 200);
    assert.equal(sessRes.body.sessions.length, 1);
    assert.equal(sessRes.body.sessions[0].isCurrent, true);

    const sessionId = sessRes.body.sessions[0].id;

    // Admin can list all sessions
    const adminSessRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/auth/sessions?all=true',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(adminSessRes.statusCode, 200);
    assert.ok(adminSessRes.body.sessions.length >= 3);

    // Logout member 2
    const logoutRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/auth/logout',
      headers: { Authorization: `Bearer ${memberToken2}` }
    });
    assert.equal(logoutRes.statusCode, 200);

    // Member 2 token is now invalid
    const meRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/auth/me',
      headers: { Authorization: `Bearer ${memberToken2}` }
    });
    assert.equal(meRes.statusCode, 401);

    // Re-login member 2
    const reLogin = await makeRequest(server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'player_shield',
      password: 'ShieldPassword123!'
    });
    assert.equal(reLogin.statusCode, 200);
    memberToken2 = reLogin.body.sessionToken;
  });

  // 4. Role-based Permissions & Promotion
  await t.test('4. Role-based permissions: Admin promotes Member to Moderator, Member cannot access admin settings', async () => {
    // Member tries to access admin settings -> 403
    const forbiddenRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/settings/admin',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(forbiddenRes.statusCode, 403);

    // Admin promotes player_shield to MODERATOR
    const promoteRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/settings/admin/users/player_shield/role',
      headers: { Authorization: `Bearer ${adminToken}` }
    }, {
      role: 'MODERATOR'
    });
    assert.equal(promoteRes.statusCode, 200);

    // Check player_shield's updated role
    const modMe = await makeRequest(server, {
      method: 'GET',
      path: '/api/auth/me',
      headers: { Authorization: `Bearer ${memberToken2}` }
    });
    assert.equal(modMe.body.user.role, 'MODERATOR');
  });

  // 5. Announcements System
  await t.test('5. Announcements: Staff creates, members view, unread tracking', async () => {
    // Moderator publishes announcement
    const createRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/announcements',
      headers: { Authorization: `Bearer ${memberToken2}` }
    }, {
      title: 'Season 4 Heart Tournament',
      content: 'The heart tournament begins this Friday at 18:00 UTC.',
      category: 'EVENT',
      isPinned: true
    });
    assert.equal(createRes.statusCode, 201);
    const annId = createRes.body.announcement.id;

    // Member lists announcements
    const listRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/announcements',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(listRes.statusCode, 200);
    assert.ok(listRes.body.announcements.length >= 2);

    // Member reads announcement -> marks as read
    const readRes = await makeRequest(server, {
      method: 'GET',
      path: `/api/announcements/${annId}`,
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(readRes.statusCode, 200);

    // Latest announcement
    const latestRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/announcements/latest',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(latestRes.statusCode, 200);
    assert.equal(latestRes.body.announcement.id, annId);
  });

  // 6. Community Messaging
  await t.test('6. Community messaging between members and unread count', async () => {
    // Search users for new conversation
    const searchRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/messages/users/search?q=shield',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(searchRes.statusCode, 200);
    assert.ok(searchRes.body.users.length >= 1);
    assert.equal(searchRes.body.users[0].id, 'player_shield');

    // player_hunter messages player_shield
    const sendRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/messages',
      headers: { Authorization: `Bearer ${memberToken1}` }
    }, {
      recipientId: 'player_shield',
      content: 'Hey, do you want to team up for the dragon fight?'
    });
    assert.equal(sendRes.statusCode, 201);
    const sentMsgId = sendRes.body.message.id;

    // player_shield checks unread count
    const unreadRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/messages/unread-count',
      headers: { Authorization: `Bearer ${memberToken2}` }
    });
    assert.equal(unreadRes.statusCode, 200);
    assert.equal(unreadRes.body.unreadCount, 1);

    // player_shield opens conversation -> messages marked read
    const convRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/messages/conversations/player_hunter',
      headers: { Authorization: `Bearer ${memberToken2}` }
    });
    assert.equal(convRes.statusCode, 200);
    assert.equal(convRes.body.messages.length, 1);
    assert.equal(convRes.body.messages[0].content, 'Hey, do you want to team up for the dragon fight?');

    // Sender deletes their own message
    const delMsgRes = await makeRequest(server, {
      method: 'DELETE',
      path: `/api/messages/${sentMsgId}`,
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(delMsgRes.statusCode, 200);
    assert.equal(delMsgRes.body.success, true);
  });

  // 7. Playtime Rewards (EXACTLY 5 Crates)
  await t.test('7. Playtime rewards: ONLY the 5 crates exist, admin configures playtime, duplicate claim prevented', async () => {
    // Get rewards: exactly the 5 crates
    const rewardsRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/rewards',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(rewardsRes.statusCode, 200);
    assert.equal(rewardsRes.body.rewards.length, 5);
    const crateIds = rewardsRes.body.rewards.map(r => r.id);
    assert.deepEqual(crateIds, ['common_crate', 'monthly_crate', 'special_crate', 'arcane_crate', 'matrix_crate']);

    // Common crate is initially unconfigured
    assert.equal(rewardsRes.body.rewards[0].status, 'PENDING_OFFICIAL_REQUIREMENT');

    // Admin configures required playtime for common_crate to 60 minutes
    const configRes = await makeRequest(server, {
      method: 'PUT',
      path: '/api/rewards/admin/config',
      headers: { Authorization: `Bearer ${adminToken}` }
    }, {
      rewardId: 'common_crate',
      requiredPlaytimeMinutes: 60
    });
    assert.equal(configRes.statusCode, 200);

    // Member attempts claim with 0 playtime -> 403 Insufficient playtime
    const claimFail = await makeRequest(server, {
      method: 'POST',
      path: '/api/rewards/common_crate/claim',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(claimFail.statusCode, 403);

    // Seed authoritative playtime via Minecraft plugin reporting: 120 minutes (7200s)
    await makeRequest(server, {
      method: 'POST',
      path: '/api/minecraft/report-stats',
      headers: { 'X-Plugin-Key': 'test_plugin_key_12345' }
    }, {
      players: [
        {
          uuid: 'uuid-hunter-1234',
          username: 'player_hunter',
          playtime_seconds: 7200,
          kills: 15,
          deaths: 2,
          hearts: 14,
          coins: 250
        }
      ]
    });

    // Now member has sufficient playtime and claims common crate
    const claimSuccess = await makeRequest(server, {
      method: 'POST',
      path: '/api/rewards/common_crate/claim',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(claimSuccess.statusCode, 201);
    assert.equal(claimSuccess.body.success, true);

    // Duplicate claim prevention
    const duplicateClaim = await makeRequest(server, {
      method: 'POST',
      path: '/api/rewards/common_crate/claim',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(duplicateClaim.statusCode, 409);
  });

  // 8. Minecraft Leaderboard & Plugin Telemetry
  await t.test('8. Minecraft leaderboard returns real reported player stats and server status', async () => {
    const lbRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/leaderboard'
    });
    assert.equal(lbRes.statusCode, 200);
    assert.equal(lbRes.body.hasRealData, true);
    assert.equal(lbRes.body.leaderboard[0].username, 'player_hunter');
    assert.equal(lbRes.body.leaderboard[0].kills, 15);
    assert.equal(lbRes.body.leaderboard[0].playtimeSeconds, 7200);
  });

  // 9. Team Management
  await t.test('9. Team creation, invitation by unique ID, acceptance and roster management', async () => {
    // player_hunter creates team 'ApexLifesteal'
    const createTeam = await makeRequest(server, {
      method: 'POST',
      path: '/api/teams',
      headers: { Authorization: `Bearer ${memberToken1}` }
    }, {
      name: 'ApexLifesteal',
      description: 'The premier hunting clan'
    });
    assert.equal(createTeam.statusCode, 201);

    // player_hunter invites player_shield by unique ID
    const inviteRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/teams/invite',
      headers: { Authorization: `Bearer ${memberToken1}` }
    }, {
      inviteeId: 'player_shield'
    });
    assert.equal(inviteRes.statusCode, 201);
    const inviteId = inviteRes.body.inviteId;

    // player_shield checks invitations
    const myInvites = await makeRequest(server, {
      method: 'GET',
      path: '/api/teams/invitations',
      headers: { Authorization: `Bearer ${memberToken2}` }
    });
    assert.equal(myInvites.statusCode, 200);
    assert.equal(myInvites.body.invitations.length, 1);
    assert.equal(myInvites.body.invitations[0].team_name, 'ApexLifesteal');

    // player_shield accepts invitation
    const acceptRes = await makeRequest(server, {
      method: 'POST',
      path: `/api/teams/invitations/${inviteId}/respond`,
      headers: { Authorization: `Bearer ${memberToken2}` }
    }, {
      action: 'ACCEPT'
    });
    assert.equal(acceptRes.statusCode, 200);

    // player_shield checks my-team
    const teamRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/teams/my-team',
      headers: { Authorization: `Bearer ${memberToken2}` }
    });
    assert.equal(teamRes.statusCode, 200);
    assert.equal(teamRes.body.inTeam, true);
    assert.equal(teamRes.body.team.name, 'ApexLifesteal');
    assert.equal(teamRes.body.team.members.length, 2);
    const teamId = teamRes.body.team.id;

    // Browse all teams
    const allTeamsRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/teams'
    });
    assert.equal(allTeamsRes.statusCode, 200);
    assert.ok(allTeamsRes.body.teams.length >= 1);
    assert.equal(allTeamsRes.body.teams[0].name, 'ApexLifesteal');
    assert.equal(allTeamsRes.body.teams[0].memberCount, 2);

    // Get specific team details
    const detailsRes = await makeRequest(server, {
      method: 'GET',
      path: `/api/teams/details/${teamId}`,
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(detailsRes.statusCode, 200);
    assert.equal(detailsRes.body.team.name, 'ApexLifesteal');
    assert.equal(detailsRes.body.team.members.length, 2);
    assert.equal(detailsRes.body.team.isCurrentUserOwner, true);

    // Search members to invite
    const searchMembersRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/teams/search-members?q=karthi',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(searchMembersRes.statusCode, 200);
    assert.ok(searchMembersRes.body.users.length >= 1);
    assert.equal(searchMembersRes.body.users[0].id, 'sg_karthi');

    // Owner edits team details
    const editRes = await makeRequest(server, {
      method: 'PUT',
      path: '/api/teams/my-team',
      headers: { Authorization: `Bearer ${memberToken1}` }
    }, {
      description: 'Updated top tier clan',
      logo: 'crown',
      status: 'RECRUITING'
    });
    assert.equal(editRes.statusCode, 200);
    assert.equal(editRes.body.team.description, 'Updated top tier clan');
    assert.equal(editRes.body.team.logo, 'crown');
    assert.equal(editRes.body.team.status, 'RECRUITING');
  });

  // 10. Role-tailored Dashboard Summaries
  await t.test('10. Role-tailored dashboard summaries for Admin, Moderator, and Member', async () => {
    // Admin summary
    const adminDash = await makeRequest(server, {
      method: 'GET',
      path: '/api/dashboard/summary',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(adminDash.statusCode, 200);
    assert.equal(adminDash.body.role, 'ADMIN');
    assert.ok(adminDash.body.adminStats.totalMembers >= 1);

    // Member summary
    const memberDash = await makeRequest(server, {
      method: 'GET',
      path: '/api/dashboard/summary',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(memberDash.statusCode, 200);
    assert.equal(memberDash.body.role, 'MEMBER');
    assert.equal(memberDash.body.playtime.minutes, 120);
    assert.equal(memberDash.body.rewardProgress.length, 5);
  });

  // 11. Minecraft Java & Bedrock Server Status Separation
  await t.test('11. Minecraft Java (Port 10010) and Bedrock (Port 10011) status separation', async () => {
    // Java endpoint
    const javaRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/minecraft/status/java'
    });
    assert.equal(javaRes.statusCode, 200);
    assert.equal(javaRes.body.address, 'node2.vyroxsmp.online');
    assert.equal(javaRes.body.port, 10010);
    assert.equal(javaRes.body.edition, 'JAVA');
    assert.equal(typeof javaRes.body.online, 'boolean');
    assert.ok(javaRes.body.players);

    // Bedrock endpoint
    const bedrockRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/minecraft/status/bedrock'
    });
    assert.equal(bedrockRes.statusCode, 200);
    assert.equal(bedrockRes.body.address, 'node2.vyroxsmp.online');
    assert.equal(bedrockRes.body.port, 10011);
    assert.equal(bedrockRes.body.edition, 'BEDROCK');
    assert.equal(typeof bedrockRes.body.online, 'boolean');
    assert.ok(bedrockRes.body.players);

    // General status includes both
    const statusRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/minecraft/status'
    });
    assert.equal(statusRes.statusCode, 200);
    assert.ok(statusRes.body.java);
    assert.ok(statusRes.body.bedrock);
    assert.equal(statusRes.body.java.port, 10010);
    assert.equal(statusRes.body.bedrock.port, 10011);
  });

  // 12. Admin Registered Members Management & Details
  await t.test('12. Admin Registered Members list, search, filter, pagination, details, and status control', async () => {
    // 1. Non-admin gets 403 Forbidden
    const forbiddenRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/settings/admin/users',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(forbiddenRes.statusCode, 403);

    // 2. Admin retrieves complete list of registered members
    const allUsersRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/settings/admin/users',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(allUsersRes.statusCode, 200);
    assert.ok(Array.isArray(allUsersRes.body.users));
    assert.ok(allUsersRes.body.users.length >= 5); // 4 initial admins + 2 registered members
    assert.ok(allUsersRes.body.pagination);
    assert.ok(allUsersRes.body.pagination.total >= 5);

    // Verify no password hashes are exposed
    for (const u of allUsersRes.body.users) {
      assert.equal(u.password_hash, undefined);
      assert.equal(u.sessionToken, undefined);
    }

    // 3. Search members by Unique ID
    const searchRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/settings/admin/users?search=player_hunter',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(searchRes.statusCode, 200);
    assert.equal(searchRes.body.users.length, 1);
    assert.equal(searchRes.body.users[0].id, 'player_hunter');

    // 4. Filter by role
    const modFilterRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/settings/admin/users?role=ADMIN',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(modFilterRes.statusCode, 200);
    assert.ok(modFilterRes.body.users.every(u => u.role === 'ADMIN'));

    // 5. Get member detailed profile
    const detailsRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/settings/admin/users/player_hunter/details',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(detailsRes.statusCode, 200);
    assert.equal(detailsRes.body.profile.id, 'player_hunter');
    assert.equal(detailsRes.body.profile.status, 'ACTIVE');
    assert.ok(detailsRes.body.team); // player_hunter is in ApexLifesteal
    assert.equal(detailsRes.body.team.name, 'ApexLifesteal');
    assert.ok(detailsRes.body.rewards);
    assert.ok(detailsRes.body.activity);

    // 6. Suspend member account and verify session revoked
    const suspendRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/settings/admin/users/player_shield/status',
      headers: { Authorization: `Bearer ${adminToken}` }
    }, {
      status: 'SUSPENDED'
    });
    assert.equal(suspendRes.statusCode, 200);
    assert.equal(suspendRes.body.status, 'SUSPENDED');

    // Suspended user token cannot access authenticated endpoints (session revoked -> 401)
    const suspendedReq = await makeRequest(server, {
      method: 'GET',
      path: '/api/auth/me',
      headers: { Authorization: `Bearer ${memberToken2}` }
    });
    assert.equal(suspendedReq.statusCode, 401);

    // 7. Reactivate member account
    const reactivateRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/settings/admin/users/player_shield/status',
      headers: { Authorization: `Bearer ${adminToken}` }
    }, {
      status: 'ACTIVE'
    });
    assert.equal(reactivateRes.statusCode, 200);
    assert.equal(reactivateRes.body.status, 'ACTIVE');

    // Log in again to get fresh token
    const reLogin = await makeRequest(server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'player_shield',
      password: 'ShieldPassword123!'
    });
    assert.equal(reLogin.statusCode, 200);
    memberToken2 = reLogin.body.sessionToken;

    // 8. Promote Member to Moderator and Demote back
    const promoteRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/settings/admin/users/player_shield/role',
      headers: { Authorization: `Bearer ${adminToken}` }
    }, {
      role: 'MODERATOR'
    });
    assert.equal(promoteRes.statusCode, 200);
    assert.equal(promoteRes.body.role, 'MODERATOR');

    const demoteRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/settings/admin/users/player_shield/role',
      headers: { Authorization: `Bearer ${adminToken}` }
    }, {
      role: 'MEMBER'
    });
    assert.equal(demoteRes.statusCode, 200);
    assert.equal(demoteRes.body.role, 'MEMBER');
  });

  // 13. Group Chat System & Direct Message Integration
  await t.test('13. Group Chat creation, member invites, messaging, permissions, and moderation', async () => {
    // 1. Create a new group chat with player_hunter as creator and player_shield invited
    const createGroupRes = await makeRequest(server, {
      method: 'POST',
      path: '/api/groups',
      headers: { Authorization: `Bearer ${memberToken1}` }
    }, {
      name: 'Lifestyle Builders',
      description: 'The master architecture squad',
      avatar: '🏰',
      memberIds: ['player_shield']
    });
    assert.equal(createGroupRes.statusCode, 201);
    assert.equal(createGroupRes.body.group.name, 'Lifestyle Builders');
    assert.equal(createGroupRes.body.group.ownerId, 'player_hunter');
    assert.equal(createGroupRes.body.group.memberCount, 2);

    const groupId = createGroupRes.body.group.id;

    // 2. Both participants see the group in their group list
    const list1 = await makeRequest(server, {
      method: 'GET',
      path: '/api/groups',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(list1.statusCode, 200);
    assert.ok(list1.body.groups.some(g => g.id === groupId));

    const list2 = await makeRequest(server, {
      method: 'GET',
      path: '/api/groups',
      headers: { Authorization: `Bearer ${memberToken2}` }
    });
    assert.equal(list2.statusCode, 200);
    assert.ok(list2.body.groups.some(g => g.id === groupId));

    // 3. Send message into group chat
    const sendMsgRes = await makeRequest(server, {
      method: 'POST',
      path: `/api/groups/${groupId}/messages`,
      headers: { Authorization: `Bearer ${memberToken1}` }
    }, {
      content: 'Welcome to the Lifestyle Builders group!'
    });
    assert.equal(sendMsgRes.statusCode, 201);
    assert.equal(sendMsgRes.body.message.content, 'Welcome to the Lifestyle Builders group!');
    assert.equal(sendMsgRes.body.message.senderId, 'player_hunter');

    // 4. Retrieve messages in group
    const msgsRes = await makeRequest(server, {
      method: 'GET',
      path: `/api/groups/${groupId}/messages`,
      headers: { Authorization: `Bearer ${memberToken2}` }
    });
    assert.equal(msgsRes.statusCode, 200);
    assert.equal(msgsRes.body.messages.length, 1);
    assert.equal(msgsRes.body.messages[0].content, 'Welcome to the Lifestyle Builders group!');

    // 5. Unauthorized non-member cannot post or read messages
    // Register third member
    const signup3 = await makeRequest(server, {
      method: 'POST',
      path: '/api/auth/signup'
    }, {
      id: 'outsider_user',
      displayName: 'Outsider User',
      password: 'StrongPass2026!#',
      confirmPassword: 'StrongPass2026!#'
    });
    assert.equal(signup3.statusCode, 201);
    const outsiderToken = signup3.body.sessionToken;

    const unauthGet = await makeRequest(server, {
      method: 'GET',
      path: `/api/groups/${groupId}`,
      headers: { Authorization: `Bearer ${outsiderToken}` }
    });
    assert.equal(unauthGet.statusCode, 403);

    const unauthPost = await makeRequest(server, {
      method: 'POST',
      path: `/api/groups/${groupId}/messages`,
      headers: { Authorization: `Bearer ${outsiderToken}` }
    }, {
      content: 'I want to infiltrate your group'
    });
    assert.equal(unauthPost.statusCode, 403);

    // 6. Group owner invites the third user
    const addMemberRes = await makeRequest(server, {
      method: 'POST',
      path: `/api/groups/${groupId}/members`,
      headers: { Authorization: `Bearer ${memberToken1}` }
    }, {
      userId: 'outsider_user'
    });
    assert.equal(addMemberRes.statusCode, 200);

    // Now newly invited user can read group
    const authGet = await makeRequest(server, {
      method: 'GET',
      path: `/api/groups/${groupId}`,
      headers: { Authorization: `Bearer ${outsiderToken}` }
    });
    assert.equal(authGet.statusCode, 200);
    assert.equal(authGet.body.members.length, 3);

    // 7. Group owner removes a member
    const removeMemberRes = await makeRequest(server, {
      method: 'DELETE',
      path: `/api/groups/${groupId}/members/outsider_user`,
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(removeMemberRes.statusCode, 200);

    // 8. Group owner transfers ownership
    const transferRes = await makeRequest(server, {
      method: 'POST',
      path: `/api/groups/${groupId}/transfer`,
      headers: { Authorization: `Bearer ${memberToken1}` }
    }, {
      newOwnerId: 'player_shield'
    });
    assert.equal(transferRes.statusCode, 200);
    assert.equal(transferRes.body.group.ownerId, 'player_shield');

    // 9. Admin moderates and deletes group
    const adminDeleteRes = await makeRequest(server, {
      method: 'DELETE',
      path: `/api/groups/${groupId}`,
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    assert.equal(adminDeleteRes.statusCode, 200);

    // Verify group is gone
    const verifyDelete = await makeRequest(server, {
      method: 'GET',
      path: `/api/groups/${groupId}`,
      headers: { Authorization: `Bearer ${memberToken2}` }
    });
    assert.equal(verifyDelete.statusCode, 404);

    // 10. Verify Direct Messages still work normally
    const dmRes = await makeRequest(server, {
      method: 'GET',
      path: '/api/messages/conversations',
      headers: { Authorization: `Bearer ${memberToken1}` }
    });
    assert.equal(dmRes.statusCode, 200);
    assert.ok(Array.isArray(dmRes.body.conversations));
  });

  await t.test('14. Public App Version check (OTA Update) and Admin Version publishing', async () => {
    // 1. Initial public version check
    const initialVersion = await makeRequest(server, {
      method: 'GET',
      path: '/api/app/version'
    });
    assert.equal(initialVersion.statusCode, 200);
    assert.equal(initialVersion.body.versionCode, 5);
    assert.equal(initialVersion.body.versionName, '1.4');
    assert.ok(initialVersion.body.apkUrl);
    assert.equal(initialVersion.body.forceUpdate, false);

    // 2. Admin publishes a new version update
    const updateRes = await makeRequest(server, {
      method: 'PUT',
      path: '/api/settings/admin',
      headers: { Authorization: `Bearer ${adminToken}` }
    }, {
      settings: {
        app_version_code: 6,
        app_version_name: '1.5.0',
        app_apk_url: 'https://github.com/sgkarthi1910-rgb/MC-App/releases/download/v1.5.0/TGZ-Lifestyle-v1.5.apk',
        app_changelog: 'Added in-app auto updates and performance enhancements.',
        app_force_update: 'true',
        app_min_supported_version_code: 5
      }
    });
    assert.equal(updateRes.statusCode, 200);

    // 3. App checks version again (simulating client startup)
    const newVersion = await makeRequest(server, {
      method: 'GET',
      path: '/api/app/version'
    });
    assert.equal(newVersion.statusCode, 200);
    assert.equal(newVersion.body.versionCode, 6);
    assert.equal(newVersion.body.versionName, '1.5.0');
    assert.equal(newVersion.body.apkUrl, 'https://github.com/sgkarthi1910-rgb/MC-App/releases/download/v1.5.0/TGZ-Lifestyle-v1.5.apk');
    assert.equal(newVersion.body.changelog, 'Added in-app auto updates and performance enhancements.');
    assert.equal(newVersion.body.forceUpdate, true);
    assert.equal(newVersion.body.minSupportedVersionCode, 5);
  });

  await t.test('15. Admin default login without forced password change (mustChangePassword defaults to false)', async () => {
    delete process.env.ADMIN_FORCE_PASSWORD_CHANGE;
    const testServer = createServer(':memory:');
    await new Promise(resolve => testServer.server.listen(0, '127.0.0.1', resolve));

    const res = await makeRequest(testServer.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'sg_karthi',
      password: 'TestBootstrapAdmin2026!'
    });

    assert.equal(res.statusCode, 200);
    assert.equal(res.body.user.id, 'sg_karthi');
    assert.equal(res.body.user.role, 'ADMIN');
    assert.equal(res.body.user.mustChangePassword, false);

    testServer.server.close();
  });

  // 16. Unauthenticated password change is rejected with HTTP 401
  await t.test('16. Unauthenticated password change returns HTTP 401 and does not modify password', async () => {
    const testServer = createServer(':memory:');
    await new Promise(resolve => testServer.server.listen(0, '127.0.0.1', resolve));

    // Unauthenticated request without session or cookie must fail with 401
    const changeRes = await makeRequest(testServer.server, {
      method: 'POST',
      path: '/api/auth/change-password'
    }, {
      userId: 'darksight',
      currentPassword: 'TestBootstrapAdmin2026!',
      newPassword: 'BrandNewSecurePass2026!',
      confirmNewPassword: 'BrandNewSecurePass2026!'
    });

    assert.equal(changeRes.statusCode, 401);
    assert.equal(changeRes.body.success, false);

    // Original password must still work
    const loginRes = await makeRequest(testServer.server, {
      method: 'POST',
      path: '/api/auth/login'
    }, {
      id: 'darksight',
      password: 'TestBootstrapAdmin2026!'
    });
    assert.equal(loginRes.statusCode, 200);
    assert.equal(loginRes.body.user.id, 'darksight');

    testServer.server.close();
  });
});
