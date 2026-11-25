package com.example.data

class TgzBackendRepository(
    private val apiService: BackendApiService,
    private val sessionManager: SessionManager
) {

    private fun authHeader(): String? {
        val token = sessionManager.getToken() ?: return null
        return "Bearer $token"
    }

    val currentUser: StoredUser?
        get() = sessionManager.getUser()

    val isLoggedIn: Boolean
        get() = sessionManager.hasActiveSession()

    // ----------------- AUTH -----------------

    suspend fun signup(
        id: String,
        displayName: String,
        pass: String,
        confirmPass: String
    ): Result<AuthResponse> = runCatching {
        val resp = apiService.signup(
            SignupRequest(
                id = id.trim().lowercase(),
                displayName = displayName.trim(),
                password = pass,
                confirmPassword = confirmPass
            )
        )
        if (resp.success && resp.sessionToken != null && resp.user != null) {
            sessionManager.saveSession(
                token = resp.sessionToken,
                user = StoredUser(
                    id = resp.user.id,
                    displayName = resp.user.displayName,
                    role = resp.user.role,
                    mustChangePassword = resp.user.mustChangePassword,
                    status = resp.user.status,
                    createdAt = resp.user.createdAt
                ),
                expiresAt = resp.expiresAt ?: 0L
            )
        }
        resp
    }

    suspend fun login(id: String, pass: String): Result<AuthResponse> = runCatching {
        val resp = apiService.login(
            LoginRequest(
                id = id.trim().lowercase(),
                password = pass
            )
        )
        if (resp.success && resp.sessionToken != null && resp.user != null) {
            sessionManager.saveSession(
                token = resp.sessionToken,
                user = StoredUser(
                    id = resp.user.id,
                    displayName = resp.user.displayName,
                    role = resp.user.role,
                    mustChangePassword = resp.user.mustChangePassword,
                    status = resp.user.status,
                    createdAt = resp.user.createdAt
                ),
                expiresAt = resp.expiresAt ?: 0L
            )
        }
        resp
    }

    suspend fun refreshCurrentUser(): Result<AuthResponse> = runCatching {
        val header = authHeader() ?: throw IllegalStateException("Not authenticated")
        val resp = apiService.getMe(header)
        if (resp.user != null) {
            val token = sessionManager.getToken() ?: ""
            sessionManager.saveSession(
                token = token,
                user = StoredUser(
                    id = resp.user.id,
                    displayName = resp.user.displayName,
                    role = resp.user.role,
                    mustChangePassword = resp.user.mustChangePassword,
                    status = resp.user.status,
                    createdAt = resp.user.createdAt
                )
            )
        }
        resp
    }

    suspend fun logout(): Result<Unit> = runCatching {
        try {
            apiService.logout(authHeader())
        } catch (_: Exception) {
            // Local logout should succeed even if network fails
        }
        sessionManager.clearSession()
    }

    suspend fun changePassword(current: String?, newPass: String, confirmPass: String): Result<GenericSuccessResponse> = runCatching {
        val user = sessionManager.getUser()
        val resp = apiService.changePassword(
            authHeader(),
            ChangePasswordRequest(
                userId = user?.id,
                currentPassword = current,
                newPassword = newPass,
                confirmNewPassword = confirmPass
            )
        )
        if (resp.success) {
            sessionManager.updatePasswordChanged()
        }
        resp
    }

    suspend fun getSessions(all: Boolean = false): Result<List<SessionItemDto>> = runCatching {
        apiService.getSessions(authHeader(), all = if (all) true else null).sessions
    }

    suspend fun revokeSession(sessionId: String): Result<GenericSuccessResponse> = runCatching {
        apiService.revokeSession(authHeader(), RevokeSessionRequest(sessionId))
    }

    // ----------------- ANNOUNCEMENTS -----------------

    suspend fun getAnnouncements(): Result<List<AnnouncementDto>> = runCatching {
        apiService.getAnnouncements(authHeader()).announcements
    }

    suspend fun getLatestAnnouncement(): Result<AnnouncementDto?> = runCatching {
        apiService.getLatestAnnouncement(authHeader()).announcement
    }

    suspend fun createAnnouncement(title: String, content: String, category: String, isPinned: Boolean): Result<GenericSuccessResponse> = runCatching {
        apiService.createAnnouncement(
            authHeader(),
            CreateAnnouncementRequest(title, content, category, isPinned)
        )
    }

    suspend fun deleteAnnouncement(id: String): Result<GenericSuccessResponse> = runCatching {
        apiService.deleteAnnouncement(authHeader(), id)
    }

    // ----------------- MESSAGING -----------------

    suspend fun getConversations(): Result<List<ConversationDto>> = runCatching {
        apiService.getConversations(authHeader()).conversations
    }

    suspend fun getConversationMessages(withUserId: String): Result<ConversationDetailResponse> = runCatching {
        apiService.getConversationMessages(authHeader(), withUserId)
    }

    suspend fun sendMessage(recipientId: String, content: String): Result<GenericSuccessResponse> = runCatching {
        apiService.sendMessage(authHeader(), SendMessageRequest(recipientId, content))
    }

    suspend fun getUnreadMessagesCount(): Result<Int> = runCatching {
        apiService.getUnreadMessagesCount(authHeader()).unreadCount
    }

    suspend fun searchUsers(query: String): Result<List<UserSearchDto>> = runCatching {
        apiService.searchUsers(authHeader(), query).users
    }

    suspend fun deleteMessage(id: String): Result<GenericSuccessResponse> = runCatching {
        apiService.deleteMessage(authHeader(), id)
    }

    suspend fun deleteConversation(withUserId: String): Result<GenericSuccessResponse> = runCatching {
        apiService.deleteConversation(authHeader(), withUserId)
    }

    // ----------------- PLAYTIME REWARDS (5 CRATES ONLY) -----------------

    suspend fun getRewards(): Result<RewardsResponse> = runCatching {
        apiService.getRewards(authHeader())
    }

    suspend fun claimReward(rewardId: String): Result<ClaimRewardResponse> = runCatching {
        apiService.claimReward(authHeader(), rewardId)
    }

    suspend fun getRewardHistory(): Result<List<ClaimHistoryItemDto>> = runCatching {
        apiService.getRewardHistory(authHeader()).history
    }

    suspend fun getAdminClaims(): Result<List<AdminClaimItemDto>> = runCatching {
        apiService.getAdminClaims(authHeader()).claims
    }

    suspend fun updateClaimStatus(claimId: String, status: String): Result<GenericSuccessResponse> = runCatching {
        apiService.updateClaimStatus(authHeader(), claimId, UpdateClaimStatusRequest(status))
    }

    suspend fun getAdminRewardConfigs(): Result<List<AdminRewardConfigDto>> = runCatching {
        apiService.getAdminRewardConfigs(authHeader()).configs
    }

    suspend fun updateRewardConfig(rewardId: String, minutes: Int?): Result<GenericSuccessResponse> = runCatching {
        apiService.updateRewardConfig(authHeader(), UpdateRewardConfigRequest(rewardId, minutes))
    }

    // ----------------- LEADERBOARD -----------------

    suspend fun getLeaderboard(category: String? = null): Result<LeaderboardResponse> = runCatching {
        apiService.getLeaderboard(category)
    }

    // ----------------- TEAMS -----------------

    suspend fun getPublicTeams(): Result<List<TeamSummaryDto>> = runCatching {
        apiService.getPublicTeams().teams
    }

    suspend fun getTeamDetails(teamId: String): Result<TeamDetailsDto> = runCatching {
        apiService.getTeamDetails(authHeader(), teamId).team
    }

    suspend fun searchTeamMembers(query: String): Result<List<TeamSearchUserDto>> = runCatching {
        apiService.searchTeamMembers(authHeader(), query).users
    }

    suspend fun getMyTeam(): Result<MyTeamResponse> = runCatching {
        apiService.getMyTeam(authHeader())
    }

    suspend fun createTeam(
        name: String,
        desc: String?,
        logo: String? = "shield",
        status: String? = "ACTIVE"
    ): Result<GenericSuccessResponse> = runCatching {
        apiService.createTeam(authHeader(), CreateTeamRequest(name, desc, logo, status))
    }

    suspend fun editMyTeam(
        desc: String?,
        logo: String?,
        status: String?
    ): Result<GenericSuccessResponse> = runCatching {
        apiService.editMyTeam(authHeader(), EditTeamRequest(desc, logo, status))
    }

    suspend fun disbandMyTeam(): Result<GenericSuccessResponse> = runCatching {
        apiService.disbandMyTeam(authHeader())
    }

    suspend fun inviteTeamMember(inviteeId: String): Result<GenericSuccessResponse> = runCatching {
        apiService.inviteTeamMember(authHeader(), InviteMemberRequest(inviteeId))
    }

    suspend fun getTeamInvitations(): Result<List<TeamInvitationReceivedDto>> = runCatching {
        apiService.getTeamInvitations(authHeader()).invitations
    }

    suspend fun respondInvitation(inviteId: String, accept: Boolean): Result<GenericSuccessResponse> = runCatching {
        apiService.respondInvitation(
            authHeader(),
            inviteId,
            RespondInvitationRequest(if (accept) "ACCEPT" else "REJECT")
        )
    }

    suspend fun leaveTeam(): Result<GenericSuccessResponse> = runCatching {
        apiService.leaveTeam(authHeader())
    }

    suspend fun removeTeamMember(memberId: String): Result<GenericSuccessResponse> = runCatching {
        apiService.removeTeamMember(authHeader(), RemoveMemberRequest(memberId))
    }

    suspend fun transferTeamOwnership(newOwnerId: String): Result<GenericSuccessResponse> = runCatching {
        apiService.transferTeamOwnership(authHeader(), TransferOwnershipRequest(newOwnerId))
    }

    suspend fun getAllTeams(): Result<List<AdminTeamItemDto>> = runCatching {
        apiService.getAllTeams(authHeader()).teams
    }

    suspend fun deleteTeam(teamId: String): Result<GenericSuccessResponse> = runCatching {
        apiService.deleteTeam(authHeader(), teamId)
    }

    // ----------------- SETTINGS -----------------

    suspend fun getMemberSettings(): Result<MemberSettingsDto> = runCatching {
        apiService.getMemberSettings(authHeader()).settings
    }

    suspend fun getAdminSettings(): Result<Map<String, String>> = runCatching {
        apiService.getAdminSettings(authHeader()).settings
    }

    suspend fun updateAdminSettings(settings: Map<String, String>): Result<GenericSuccessResponse> = runCatching {
        apiService.updateAdminSettings(authHeader(), UpdateAdminSettingsRequest(settings))
    }

    suspend fun getUserManagementList(): Result<List<UserManagementItemDto>> = runCatching {
        apiService.getUserManagementList(authHeader()).users
    }

    suspend fun updateUserRole(userId: String, role: String): Result<GenericSuccessResponse> = runCatching {
        apiService.updateUserRole(authHeader(), userId, UpdateUserRoleRequest(role))
    }

    suspend fun updateUserStatus(userId: String, status: String): Result<GenericSuccessResponse> = runCatching {
        apiService.updateUserStatus(authHeader(), userId, UpdateUserStatusRequest(status))
    }

    // ----------------- DASHBOARD -----------------

    suspend fun getDashboardSummary(): Result<DashboardSummaryResponse> = runCatching {
        apiService.getDashboardSummary(authHeader())
    }

    // ----------------- MINECRAFT SERVER STATUS -----------------

    suspend fun getJavaServerStatus(): Result<MinecraftServerStatusDto> = runCatching {
        apiService.getJavaServerStatus()
    }

    suspend fun getBedrockServerStatus(): Result<MinecraftServerStatusDto> = runCatching {
        apiService.getBedrockServerStatus()
    }

    // ----------------- ADMIN REGISTERED MEMBERS -----------------

    suspend fun getAdminRegisteredMembers(
        search: String? = null,
        role: String? = null,
        status: String? = null,
        sort: String? = null,
        order: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<AdminMembersResponse> = runCatching {
        apiService.getAdminRegisteredMembers(
            auth = authHeader(),
            search = search,
            role = role,
            status = status,
            sort = sort,
            order = order,
            page = page,
            limit = limit
        )
    }

    suspend fun getAdminMemberDetails(userId: String): Result<AdminMemberDetailsResponse> = runCatching {
        apiService.getAdminMemberDetails(authHeader(), userId)
    }

    // ----------------- GROUP CHATS -----------------

    suspend fun getGroups(): Result<List<GroupDto>> = runCatching {
        apiService.getGroups(authHeader()).groups
    }

    suspend fun createGroup(
        name: String,
        description: String? = null,
        avatar: String? = "👥",
        memberIds: List<String> = emptyList()
    ): Result<GroupDto> = runCatching {
        apiService.createGroup(
            auth = authHeader(),
            req = CreateGroupRequest(name, description, avatar, memberIds)
        ).group
    }

    suspend fun getGroupDetails(groupId: String): Result<GroupDetailResponse> = runCatching {
        apiService.getGroupDetails(authHeader(), groupId)
    }

    suspend fun getGroupMessages(groupId: String): Result<List<GroupMessageDto>> = runCatching {
        apiService.getGroupMessages(authHeader(), groupId).messages
    }

    suspend fun sendGroupMessage(groupId: String, content: String): Result<GroupMessageDto> = runCatching {
        apiService.sendGroupMessage(
            auth = authHeader(),
            groupId = groupId,
            req = SendGroupMessageRequest(content)
        ).message
    }

    suspend fun updateGroup(
        groupId: String,
        name: String?,
        description: String?,
        avatar: String?
    ): Result<GenericSuccessResponse> = runCatching {
        apiService.updateGroup(authHeader(), groupId, UpdateGroupRequest(name, description, avatar))
    }

    suspend fun addGroupMember(groupId: String, userId: String): Result<GenericSuccessResponse> = runCatching {
        apiService.addGroupMember(authHeader(), groupId, AddGroupMemberRequest(userId))
    }

    suspend fun removeGroupMember(groupId: String, userId: String): Result<GenericSuccessResponse> = runCatching {
        apiService.removeGroupMember(authHeader(), groupId, userId)
    }

    suspend fun transferGroupOwnership(groupId: String, newOwnerId: String): Result<GenericSuccessResponse> = runCatching {
        apiService.transferGroupOwnership(authHeader(), groupId, TransferGroupOwnershipRequest(newOwnerId))
    }

    suspend fun deleteGroup(groupId: String): Result<GenericSuccessResponse> = runCatching {
        apiService.deleteGroup(authHeader(), groupId)
    }

    // ----------------- APP OTA UPDATES -----------------
    suspend fun getAppVersion(): Result<AppVersionResponse> = runCatching {
        apiService.getAppVersion()
    }
}
