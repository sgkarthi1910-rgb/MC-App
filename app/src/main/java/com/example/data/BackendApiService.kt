package com.example.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface BackendApiService {

    // ----------------- AUTH -----------------
    @POST("api/auth/signup")
    suspend fun signup(@Body req: SignupRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") auth: String?): GenericSuccessResponse

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") auth: String?): AuthResponse

    @POST("api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") auth: String?,
        @Body req: ChangePasswordRequest
    ): GenericSuccessResponse

    @GET("api/auth/sessions")
    suspend fun getSessions(
        @Header("Authorization") auth: String?,
        @Query("all") all: Boolean? = null
    ): SessionListResponse

    @POST("api/auth/sessions/revoke")
    suspend fun revokeSession(
        @Header("Authorization") auth: String?,
        @Body req: RevokeSessionRequest
    ): GenericSuccessResponse

    // ----------------- ANNOUNCEMENTS -----------------
    @GET("api/announcements")
    suspend fun getAnnouncements(@Header("Authorization") auth: String?): AnnouncementListResponse

    @GET("api/announcements/latest")
    suspend fun getLatestAnnouncement(@Header("Authorization") auth: String?): LatestAnnouncementResponse

    @POST("api/announcements")
    suspend fun createAnnouncement(
        @Header("Authorization") auth: String?,
        @Body req: CreateAnnouncementRequest
    ): GenericSuccessResponse

    @DELETE("api/announcements/{id}")
    suspend fun deleteAnnouncement(
        @Header("Authorization") auth: String?,
        @Path("id") id: String
    ): GenericSuccessResponse

    // ----------------- MESSAGING -----------------
    @GET("api/messages/conversations")
    suspend fun getConversations(@Header("Authorization") auth: String?): ConversationListResponse

    @GET("api/messages/conversations/{withUserId}")
    suspend fun getConversationMessages(
        @Header("Authorization") auth: String?,
        @Path("withUserId") withUserId: String
    ): ConversationDetailResponse

    @POST("api/messages")
    suspend fun sendMessage(
        @Header("Authorization") auth: String?,
        @Body req: SendMessageRequest
    ): GenericSuccessResponse

    @GET("api/messages/unread-count")
    suspend fun getUnreadMessagesCount(@Header("Authorization") auth: String?): UnreadMessagesCountResponse

    @GET("api/messages/users/search")
    suspend fun searchUsers(
        @Header("Authorization") auth: String?,
        @Query("q") query: String
    ): UserSearchResponse

    @DELETE("api/messages/{id}")
    suspend fun deleteMessage(
        @Header("Authorization") auth: String?,
        @Path("id") id: String
    ): GenericSuccessResponse

    @DELETE("api/messages/conversations/{withUserId}")
    suspend fun deleteConversation(
        @Header("Authorization") auth: String?,
        @Path("withUserId") withUserId: String
    ): GenericSuccessResponse

    // ----------------- PLAYTIME REWARDS -----------------
    @GET("api/rewards")
    suspend fun getRewards(@Header("Authorization") auth: String?): RewardsResponse

    @POST("api/rewards/{rewardId}/claim")
    suspend fun claimReward(
        @Header("Authorization") auth: String?,
        @Path("rewardId") rewardId: String
    ): ClaimRewardResponse

    @GET("api/rewards/history")
    suspend fun getRewardHistory(@Header("Authorization") auth: String?): RewardHistoryResponse

    @GET("api/rewards/admin/claims")
    suspend fun getAdminClaims(@Header("Authorization") auth: String?): AdminClaimsResponse

    @PUT("api/rewards/admin/claims/{claimId}")
    suspend fun updateClaimStatus(
        @Header("Authorization") auth: String?,
        @Path("claimId") claimId: String,
        @Body req: UpdateClaimStatusRequest
    ): GenericSuccessResponse

    @GET("api/rewards/admin/config")
    suspend fun getAdminRewardConfigs(@Header("Authorization") auth: String?): AdminRewardConfigsResponse

    @PUT("api/rewards/admin/config")
    suspend fun updateRewardConfig(
        @Header("Authorization") auth: String?,
        @Body req: UpdateRewardConfigRequest
    ): GenericSuccessResponse

    // ----------------- LEADERBOARD -----------------
    @GET("api/leaderboard")
    suspend fun getLeaderboard(@Query("category") category: String? = null): LeaderboardResponse

    // ----------------- TEAMS -----------------
    @GET("api/teams")
    suspend fun getPublicTeams(): TeamListResponse

    @GET("api/teams/details/{teamId}")
    suspend fun getTeamDetails(
        @Header("Authorization") auth: String?,
        @Path("teamId") teamId: String
    ): SingleTeamDetailsResponse

    @GET("api/teams/search-members")
    suspend fun searchTeamMembers(
        @Header("Authorization") auth: String?,
        @Query("q") query: String
    ): TeamSearchUsersResponse

    @GET("api/teams/my-team")
    suspend fun getMyTeam(@Header("Authorization") auth: String?): MyTeamResponse

    @POST("api/teams")
    suspend fun createTeam(
        @Header("Authorization") auth: String?,
        @Body req: CreateTeamRequest
    ): GenericSuccessResponse

    @PUT("api/teams/my-team")
    suspend fun editMyTeam(
        @Header("Authorization") auth: String?,
        @Body req: EditTeamRequest
    ): GenericSuccessResponse

    @DELETE("api/teams/my-team")
    suspend fun disbandMyTeam(
        @Header("Authorization") auth: String?
    ): GenericSuccessResponse

    @POST("api/teams/invite")
    suspend fun inviteTeamMember(
        @Header("Authorization") auth: String?,
        @Body req: InviteMemberRequest
    ): GenericSuccessResponse

    @GET("api/teams/invitations")
    suspend fun getTeamInvitations(@Header("Authorization") auth: String?): InvitationsResponse

    @POST("api/teams/invitations/{inviteId}/respond")
    suspend fun respondInvitation(
        @Header("Authorization") auth: String?,
        @Path("inviteId") inviteId: String,
        @Body req: RespondInvitationRequest
    ): GenericSuccessResponse

    @POST("api/teams/leave")
    suspend fun leaveTeam(@Header("Authorization") auth: String?): GenericSuccessResponse

    @POST("api/teams/remove-member")
    suspend fun removeTeamMember(
        @Header("Authorization") auth: String?,
        @Body req: RemoveMemberRequest
    ): GenericSuccessResponse

    @POST("api/teams/transfer-ownership")
    suspend fun transferTeamOwnership(
        @Header("Authorization") auth: String?,
        @Body req: TransferOwnershipRequest
    ): GenericSuccessResponse

    @GET("api/teams/all")
    suspend fun getAllTeams(@Header("Authorization") auth: String?): AdminTeamsResponse

    @DELETE("api/teams/{id}")
    suspend fun deleteTeam(
        @Header("Authorization") auth: String?,
        @Path("id") id: String
    ): GenericSuccessResponse

    // ----------------- SETTINGS -----------------
    @GET("api/settings/member")
    suspend fun getMemberSettings(@Header("Authorization") auth: String?): MemberSettingsResponse

    @GET("api/settings/admin")
    suspend fun getAdminSettings(@Header("Authorization") auth: String?): AdminSettingsResponse

    @PUT("api/settings/admin")
    suspend fun updateAdminSettings(
        @Header("Authorization") auth: String?,
        @Body req: UpdateAdminSettingsRequest
    ): GenericSuccessResponse

    @GET("api/settings/admin/users")
    suspend fun getUserManagementList(@Header("Authorization") auth: String?): UserManagementListResponse

    @POST("api/settings/admin/users/{userId}/role")
    suspend fun updateUserRole(
        @Header("Authorization") auth: String?,
        @Path("userId") userId: String,
        @Body req: UpdateUserRoleRequest
    ): GenericSuccessResponse

    @POST("api/settings/admin/users/{userId}/status")
    suspend fun updateUserStatus(
        @Header("Authorization") auth: String?,
        @Path("userId") userId: String,
        @Body req: UpdateUserStatusRequest
    ): GenericSuccessResponse

    // ----------------- DASHBOARD -----------------
    @GET("api/dashboard/summary")
    suspend fun getDashboardSummary(@Header("Authorization") auth: String?): DashboardSummaryResponse
}
