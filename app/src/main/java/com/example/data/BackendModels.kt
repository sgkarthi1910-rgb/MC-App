package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ----------------- AUTH DTOs -----------------

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "role") val role: String,
    @Json(name = "mustChangePassword") val mustChangePassword: Boolean = false,
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "createdAt") val createdAt: Long = 0L,
    @Json(name = "minecraftUuid") val minecraftUuid: String? = null,
    @Json(name = "minecraftUsername") val minecraftUsername: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "id") val id: String,
    @Json(name = "password") val password: String,
    @Json(name = "deviceInfo") val deviceInfo: String? = "TGZ Android App"
)

@JsonClass(generateAdapter = true)
data class SignupRequest(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "password") val password: String,
    @Json(name = "confirmPassword") val confirmPassword: String,
    @Json(name = "deviceInfo") val deviceInfo: String? = "TGZ Android App"
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "sessionToken") val sessionToken: String? = null,
    @Json(name = "sessionId") val sessionId: String? = null,
    @Json(name = "expiresAt") val expiresAt: Long? = null,
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    @Json(name = "currentPassword") val currentPassword: String?,
    @Json(name = "newPassword") val newPassword: String,
    @Json(name = "confirmNewPassword") val confirmNewPassword: String
)

@JsonClass(generateAdapter = true)
data class SessionItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "device_info") val deviceInfo: String? = null,
    @Json(name = "ip_address") val ipAddress: String? = null,
    @Json(name = "created_at") val createdAt: Long = 0L,
    @Json(name = "last_active_at") val lastActiveAt: Long = 0L,
    @Json(name = "expires_at") val expiresAt: Long = 0L,
    @Json(name = "is_revoked") val isRevoked: Int = 0,
    @Json(name = "isCurrent") val isCurrent: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SessionListResponse(
    @Json(name = "sessions") val sessions: List<SessionItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RevokeSessionRequest(
    @Json(name = "sessionId") val sessionId: String
)

@JsonClass(generateAdapter = true)
data class GenericSuccessResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)

// ----------------- ANNOUNCEMENT DTOs -----------------

@JsonClass(generateAdapter = true)
data class AnnouncementDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String,
    @Json(name = "author_id") val authorId: String? = null,
    @Json(name = "author_name") val authorName: String = "Staff",
    @Json(name = "category") val category: String = "GENERAL",
    @Json(name = "is_pinned") val isPinned: Int = 0,
    @Json(name = "created_at") val createdAt: Long = 0L,
    @Json(name = "updated_at") val updatedAt: Long = 0L,
    @Json(name = "is_read") val isRead: Int = 0
)

@JsonClass(generateAdapter = true)
data class AnnouncementListResponse(
    @Json(name = "announcements") val announcements: List<AnnouncementDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LatestAnnouncementResponse(
    @Json(name = "announcement") val announcement: AnnouncementDto? = null
)

@JsonClass(generateAdapter = true)
data class CreateAnnouncementRequest(
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String,
    @Json(name = "category") val category: String = "GENERAL",
    @Json(name = "isPinned") val isPinned: Boolean = false
)

// ----------------- MESSAGING DTOs -----------------

@JsonClass(generateAdapter = true)
data class ConversationDto(
    @Json(name = "id") val id: String,
    @Json(name = "updated_at") val updatedAt: Long = 0L,
    @Json(name = "partner_id") val partnerId: String,
    @Json(name = "partner_name") val partnerName: String,
    @Json(name = "partner_role") val partnerRole: String = "MEMBER",
    @Json(name = "partner_status") val partnerStatus: String = "ACTIVE",
    @Json(name = "partner_team") val partnerTeam: String? = null,
    @Json(name = "last_message") val lastMessage: String? = null,
    @Json(name = "last_message_time") val lastMessageTime: Long? = null,
    @Json(name = "last_sender_id") val lastSenderId: String? = null,
    @Json(name = "unread_count") val unreadCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class ConversationListResponse(
    @Json(name = "conversations") val conversations: List<ConversationDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MessageDto(
    @Json(name = "id") val id: String,
    @Json(name = "conversation_id") val conversationId: String? = null,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "recipient_id") val recipientId: String,
    @Json(name = "content") val content: String,
    @Json(name = "is_read") val isRead: Int = 0,
    @Json(name = "created_at") val createdAt: Long = 0L,
    @Json(name = "sender_name") val senderName: String? = null,
    @Json(name = "sender_role") val senderRole: String? = null
)

@JsonClass(generateAdapter = true)
data class ConversationPartnerDto(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "role") val role: String,
    @Json(name = "status") val status: String,
    @Json(name = "teamName") val teamName: String? = null,
    @Json(name = "isSharedTeam") val isSharedTeam: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ConversationDetailResponse(
    @Json(name = "conversationId") val conversationId: String,
    @Json(name = "partner") val partner: ConversationPartnerDto,
    @Json(name = "messages") val messages: List<MessageDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UserSearchDto(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "role") val role: String = "MEMBER",
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "teamName") val teamName: String? = null,
    @Json(name = "isSharedTeam") val isSharedTeam: Boolean = false
)

@JsonClass(generateAdapter = true)
data class UserSearchResponse(
    @Json(name = "users") val users: List<UserSearchDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "recipientId") val recipientId: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class UnreadMessagesCountResponse(
    @Json(name = "unreadCount") val unreadCount: Int = 0
)

// ----------------- PLAYTIME REWARDS DTOs -----------------

@JsonClass(generateAdapter = true)
data class PlaytimeRewardDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "badge") val badge: String? = null,
    @Json(name = "requiredPlaytimeMinutes") val requiredPlaytimeMinutes: Int? = null,
    @Json(name = "requiredPlaytimeFormatted") val requiredPlaytimeFormatted: String = "",
    @Json(name = "currentPlaytimeMinutes") val currentPlaytimeMinutes: Int = 0,
    @Json(name = "currentPlaytimeFormatted") val currentPlaytimeFormatted: String = "",
    @Json(name = "progressPercent") val progressPercent: Int = 0,
    @Json(name = "status") val status: String = "LOCKED",
    @Json(name = "isClaimed") val isClaimed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RewardsResponse(
    @Json(name = "userPlaytimeMinutes") val userPlaytimeMinutes: Int = 0,
    @Json(name = "rewards") val rewards: List<PlaytimeRewardDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ClaimRewardResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ClaimHistoryItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "reward_id") val rewardId: String,
    @Json(name = "reward_name") val rewardName: String,
    @Json(name = "badge") val badge: String? = null,
    @Json(name = "playtime_at_claim") val playtimeAtClaim: Int = 0,
    @Json(name = "claimed_at") val claimedAt: Long = 0L,
    @Json(name = "status") val status: String = "APPROVED"
)

@JsonClass(generateAdapter = true)
data class RewardHistoryResponse(
    @Json(name = "history") val history: List<ClaimHistoryItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AdminClaimItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "user_role") val userRole: String,
    @Json(name = "reward_id") val rewardId: String,
    @Json(name = "reward_name") val rewardName: String,
    @Json(name = "playtime_at_claim") val playtimeAtClaim: Int = 0,
    @Json(name = "claimed_at") val claimedAt: Long = 0L,
    @Json(name = "status") val status: String = "APPROVED"
)

@JsonClass(generateAdapter = true)
data class AdminClaimsResponse(
    @Json(name = "claims") val claims: List<AdminClaimItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UpdateClaimStatusRequest(
    @Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class AdminRewardConfigDto(
    @Json(name = "reward_id") val rewardId: String,
    @Json(name = "name") val name: String,
    @Json(name = "required_playtime_minutes") val requiredPlaytimeMinutes: Int? = null,
    @Json(name = "description") val description: String,
    @Json(name = "badge") val badge: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminRewardConfigsResponse(
    @Json(name = "configs") val configs: List<AdminRewardConfigDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UpdateRewardConfigRequest(
    @Json(name = "rewardId") val rewardId: String,
    @Json(name = "requiredPlaytimeMinutes") val requiredPlaytimeMinutes: Int?
)

// ----------------- LEADERBOARD DTOs -----------------

@JsonClass(generateAdapter = true)
data class LeaderboardPlayerDto(
    @Json(name = "rank") val rank: Int,
    @Json(name = "uuid") val uuid: String?,
    @Json(name = "username") val username: String,
    @Json(name = "playtimeSeconds") val playtimeSeconds: Int = 0,
    @Json(name = "playtimeFormatted") val playtimeFormatted: String = "",
    @Json(name = "kills") val kills: Int = 0,
    @Json(name = "deaths") val deaths: Int = 0,
    @Json(name = "kdr") val kdr: Double = 0.0,
    @Json(name = "hearts") val hearts: Int = 10,
    @Json(name = "coins") val coins: Int = 0,
    @Json(name = "avatarUrl") val avatarUrl: String = "",
    @Json(name = "lastSeen") val lastSeen: Long = 0L
)

@JsonClass(generateAdapter = true)
data class LeaderboardResponse(
    @Json(name = "category") val category: String = "playtime",
    @Json(name = "totalPlayersTracked") val totalPlayersTracked: Int = 0,
    @Json(name = "lastUpdated") val lastUpdated: Long = 0L,
    @Json(name = "leaderboard") val leaderboard: List<LeaderboardPlayerDto> = emptyList(),
    @Json(name = "hasRealData") val hasRealData: Boolean = false,
    @Json(name = "syncNote") val syncNote: String? = null
)

// ----------------- TEAMS DTOs -----------------

@JsonClass(generateAdapter = true)
data class TeamMemberDto(
    @Json(name = "user_id") val userId: String,
    @Json(name = "role") val role: String = "MEMBER",
    @Json(name = "joined_at") val joinedAt: Long = 0L,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "app_role") val appRole: String = "MEMBER",
    @Json(name = "status") val status: String = "ACTIVE"
)

@JsonClass(generateAdapter = true)
data class TeamInvitationSentDto(
    @Json(name = "id") val id: String,
    @Json(name = "invitee_id") val inviteeId: String,
    @Json(name = "created_at") val createdAt: Long = 0L,
    @Json(name = "invitee_name") val inviteeName: String
)

@JsonClass(generateAdapter = true)
data class TeamDetailsDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "logo") val logo: String = "shield",
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "ownerId") val ownerId: String,
    @Json(name = "ownerName") val ownerName: String,
    @Json(name = "createdAt") val createdAt: Long = 0L,
    @Json(name = "isCurrentUserOwner") val isCurrentUserOwner: Boolean = false,
    @Json(name = "currentUserRole") val currentUserRole: String = "MEMBER",
    @Json(name = "members") val members: List<TeamMemberDto> = emptyList(),
    @Json(name = "pendingInvitations") val pendingInvitations: List<TeamInvitationSentDto> = emptyList(),
    @Json(name = "totalKills") val totalKills: Int = 0,
    @Json(name = "totalPlaytimeMinutes") val totalPlaytimeMinutes: Int = 0
)

@JsonClass(generateAdapter = true)
data class TeamSummaryDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "logo") val logo: String = "shield",
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "ownerId") val ownerId: String,
    @Json(name = "ownerName") val ownerName: String,
    @Json(name = "createdAt") val createdAt: Long = 0L,
    @Json(name = "memberCount") val memberCount: Int = 1,
    @Json(name = "membersPreview") val membersPreview: List<String> = emptyList(),
    @Json(name = "pendingInviteCount") val pendingInviteCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class TeamListResponse(
    @Json(name = "teams") val teams: List<TeamSummaryDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SingleTeamDetailsResponse(
    @Json(name = "team") val team: TeamDetailsDto
)

@JsonClass(generateAdapter = true)
data class TeamSearchUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "role") val role: String,
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "alreadyInTeam") val alreadyInTeam: Boolean = false,
    @Json(name = "teamName") val teamName: String? = null
)

@JsonClass(generateAdapter = true)
data class TeamSearchUsersResponse(
    @Json(name = "users") val users: List<TeamSearchUserDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MyTeamResponse(
    @Json(name = "inTeam") val inTeam: Boolean = false,
    @Json(name = "team") val team: TeamDetailsDto? = null
)

@JsonClass(generateAdapter = true)
data class TeamInvitationReceivedDto(
    @Json(name = "id") val id: String,
    @Json(name = "team_id") val teamId: String,
    @Json(name = "inviter_id") val inviterId: String,
    @Json(name = "created_at") val createdAt: Long = 0L,
    @Json(name = "status") val status: String = "PENDING",
    @Json(name = "team_name") val teamName: String,
    @Json(name = "team_description") val teamDescription: String? = null,
    @Json(name = "inviter_name") val inviterName: String
)

@JsonClass(generateAdapter = true)
data class InvitationsResponse(
    @Json(name = "invitations") val invitations: List<TeamInvitationReceivedDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CreateTeamRequest(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "logo") val logo: String? = "shield",
    @Json(name = "status") val status: String? = "ACTIVE"
)

@JsonClass(generateAdapter = true)
data class EditTeamRequest(
    @Json(name = "description") val description: String? = null,
    @Json(name = "logo") val logo: String? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class InviteMemberRequest(
    @Json(name = "inviteeId") val inviteeId: String
)

@JsonClass(generateAdapter = true)
data class RespondInvitationRequest(
    @Json(name = "action") val action: String // 'ACCEPT' or 'REJECT'
)

@JsonClass(generateAdapter = true)
data class RemoveMemberRequest(
    @Json(name = "memberId") val memberId: String
)

@JsonClass(generateAdapter = true)
data class TransferOwnershipRequest(
    @Json(name = "newOwnerId") val newOwnerId: String
)

@JsonClass(generateAdapter = true)
data class AdminTeamItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "owner_name") val ownerName: String,
    @Json(name = "created_at") val createdAt: Long = 0L,
    @Json(name = "member_count") val memberCount: Int = 1
)

@JsonClass(generateAdapter = true)
data class AdminTeamsResponse(
    @Json(name = "teams") val teams: List<AdminTeamItemDto> = emptyList()
)

// ----------------- SETTINGS DTOs -----------------

@JsonClass(generateAdapter = true)
data class MemberSettingsDto(
    @Json(name = "userId") val userId: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "role") val role: String,
    @Json(name = "status") val status: String,
    @Json(name = "createdAt") val createdAt: Long = 0L,
    @Json(name = "lastLoginAt") val lastLoginAt: Long? = null,
    @Json(name = "minecraftUuid") val minecraftUuid: String? = null,
    @Json(name = "minecraftUsername") val minecraftUsername: String? = null,
    @Json(name = "activeSessionsCount") val activeSessionsCount: Int = 1
)

@JsonClass(generateAdapter = true)
data class MemberSettingsResponse(
    @Json(name = "settings") val settings: MemberSettingsDto
)

@JsonClass(generateAdapter = true)
data class AdminSettingsResponse(
    @Json(name = "settings") val settings: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class UpdateAdminSettingsRequest(
    @Json(name = "settings") val settings: Map<String, String>
)

@JsonClass(generateAdapter = true)
data class UserManagementItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "role") val role: String,
    @Json(name = "status") val status: String,
    @Json(name = "created_at") val createdAt: Long = 0L,
    @Json(name = "last_login_at") val lastLoginAt: Long? = null,
    @Json(name = "active_sessions") val activeSessions: Int = 0
)

@JsonClass(generateAdapter = true)
data class UserManagementListResponse(
    @Json(name = "users") val users: List<UserManagementItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UpdateUserRoleRequest(
    @Json(name = "role") val role: String
)

@JsonClass(generateAdapter = true)
data class UpdateUserStatusRequest(
    @Json(name = "status") val status: String
)

// ----------------- DASHBOARD DTOs -----------------

@JsonClass(generateAdapter = true)
data class AdminStatsDto(
    @Json(name = "totalMembers") val totalMembers: Int = 0,
    @Json(name = "activeMembers") val activeMembers: Int = 0,
    @Json(name = "totalModerators") val totalModerators: Int = 0,
    @Json(name = "activeSessions") val activeSessions: Int = 0,
    @Json(name = "totalTeams") val totalTeams: Int = 0,
    @Json(name = "pendingClaims") val pendingClaims: Int = 0
)

@JsonClass(generateAdapter = true)
data class ModeratorStatsDto(
    @Json(name = "totalMembers") val totalMembers: Int = 0,
    @Json(name = "activeSessions") val activeSessions: Int = 0,
    @Json(name = "totalTeams") val totalTeams: Int = 0,
    @Json(name = "pendingClaims") val pendingClaims: Int = 0
)

@JsonClass(generateAdapter = true)
data class MemberPlaytimeDto(
    @Json(name = "seconds") val seconds: Int = 0,
    @Json(name = "minutes") val minutes: Int = 0,
    @Json(name = "formatted") val formatted: String = "0h 0m"
)

@JsonClass(generateAdapter = true)
data class MemberRewardProgressDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "badge") val badge: String? = null,
    @Json(name = "status") val status: String = "LOCKED",
    @Json(name = "progressPercent") val progressPercent: Int = 0
)

@JsonClass(generateAdapter = true)
data class MemberTeamSummaryDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String
)

@JsonClass(generateAdapter = true)
data class DashboardSummaryResponse(
    @Json(name = "role") val role: String,
    @Json(name = "adminStats") val adminStats: AdminStatsDto? = null,
    @Json(name = "moderatorStats") val moderatorStats: ModeratorStatsDto? = null,
    @Json(name = "profile") val profile: UserDto? = null,
    @Json(name = "playtime") val playtime: MemberPlaytimeDto? = null,
    @Json(name = "kills") val kills: Int = 0,
    @Json(name = "deaths") val deaths: Int = 0,
    @Json(name = "hearts") val hearts: Int = 10,
    @Json(name = "coins") val coins: Int = 0,
    @Json(name = "leaderboardRank") val leaderboardRank: Int? = null,
    @Json(name = "rewardProgress") val rewardProgress: List<MemberRewardProgressDto> = emptyList(),
    @Json(name = "latestAnnouncement") val latestAnnouncement: AnnouncementDto? = null,
    @Json(name = "unreadMessagesCount") val unreadMessagesCount: Int = 0,
    @Json(name = "team") val team: MemberTeamSummaryDto? = null
)

// ----------------- MINECRAFT SERVER STATUS DTOs -----------------

@JsonClass(generateAdapter = true)
data class MinecraftPlayersDto(
    @Json(name = "online") val online: Int = 0,
    @Json(name = "max") val max: Int = 200,
    @Json(name = "list") val list: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class MinecraftServerStatusDto(
    @Json(name = "online") val online: Boolean = false,
    @Json(name = "address") val address: String = "node2.vyroxsmp.online",
    @Json(name = "port") val port: Int = 10010,
    @Json(name = "edition") val edition: String = "JAVA",
    @Json(name = "players") val players: MinecraftPlayersDto? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "motd") val motd: String? = null,
    @Json(name = "lastUpdated") val lastUpdated: Long = 0L
)

// ----------------- ADMIN REGISTERED MEMBERS DTOs -----------------

@JsonClass(generateAdapter = true)
data class AdminMemberItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "role") val role: String,
    @Json(name = "status") val status: String,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "minecraft_username") val minecraftUsername: String? = null,
    @Json(name = "created_at") val createdAt: Long = 0L,
    @Json(name = "last_login_at") val lastLoginAt: Long? = null,
    @Json(name = "active_sessions") val activeSessions: Int = 0,
    @Json(name = "is_online") val isOnline: Int = 0,
    @Json(name = "team_name") val teamName: String? = null,
    @Json(name = "claimed_rewards_count") val claimedRewardsCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class AdminMembersPaginationDto(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "page") val page: Int = 1,
    @Json(name = "limit") val limit: Int = 20,
    @Json(name = "pages") val pages: Int = 1
)

@JsonClass(generateAdapter = true)
data class AdminMembersResponse(
    @Json(name = "users") val users: List<AdminMemberItemDto> = emptyList(),
    @Json(name = "pagination") val pagination: AdminMembersPaginationDto? = null
)

@JsonClass(generateAdapter = true)
data class AdminMemberProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "role") val role: String,
    @Json(name = "status") val status: String,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "minecraftUsername") val minecraftUsername: String? = null,
    @Json(name = "createdAt") val createdAt: Long = 0L,
    @Json(name = "lastLoginAt") val lastLoginAt: Long? = null,
    @Json(name = "isOnline") val isOnline: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AdminMemberTeamInfoDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "tag") val tag: String,
    @Json(name = "role") val role: String,
    @Json(name = "joinedAt") val joinedAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class AdminMemberRewardsDto(
    @Json(name = "totalClaims") val totalClaims: Int = 0,
    @Json(name = "lastClaimed") val lastClaimed: Long? = null,
    @Json(name = "history") val history: List<ClaimHistoryItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AdminMemberActivityDto(
    @Json(name = "activeSessions") val activeSessions: Int = 0,
    @Json(name = "directMessagesSent") val directMessagesSent: Int = 0,
    @Json(name = "groupMessagesSent") val groupMessagesSent: Int = 0
)

@JsonClass(generateAdapter = true)
data class AdminMemberDetailsResponse(
    @Json(name = "profile") val profile: AdminMemberProfileDto,
    @Json(name = "team") val team: AdminMemberTeamInfoDto? = null,
    @Json(name = "rewards") val rewards: AdminMemberRewardsDto? = null,
    @Json(name = "activity") val activity: AdminMemberActivityDto? = null
)

// ----------------- GROUP CHAT DTOs -----------------

@JsonClass(generateAdapter = true)
data class GroupLatestMessageDto(
    @Json(name = "content") val content: String,
    @Json(name = "senderId") val senderId: String,
    @Json(name = "senderName") val senderName: String? = null,
    @Json(name = "createdAt") val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class GroupDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "avatar") val avatar: String = "👥",
    @Json(name = "ownerId") val ownerId: String,
    @Json(name = "ownerName") val ownerName: String? = null,
    @Json(name = "createdAt") val createdAt: Long = 0L,
    @Json(name = "updatedAt") val updatedAt: Long = 0L,
    @Json(name = "memberCount") val memberCount: Int = 1,
    @Json(name = "unreadCount") val unreadCount: Int = 0,
    @Json(name = "latestMessage") val latestMessage: GroupLatestMessageDto? = null,
    @Json(name = "latestMessageTime") val latestMessageTime: Long? = null
)

@JsonClass(generateAdapter = true)
data class GroupListResponse(
    @Json(name = "groups") val groups: List<GroupDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GroupMemberDto(
    @Json(name = "userId") val userId: String,
    @Json(name = "role") val role: String = "MEMBER",
    @Json(name = "joinedAt") val joinedAt: Long = 0L,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "appRole") val appRole: String = "MEMBER",
    @Json(name = "avatar") val avatar: String? = null
)

@JsonClass(generateAdapter = true)
data class GroupDetailResponse(
    @Json(name = "group") val group: GroupDto,
    @Json(name = "members") val members: List<GroupMemberDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GroupMessageDto(
    @Json(name = "id") val id: String,
    @Json(name = "groupId") val groupId: String,
    @Json(name = "senderId") val senderId: String,
    @Json(name = "senderName") val senderName: String = "",
    @Json(name = "senderAvatar") val senderAvatar: String? = null,
    @Json(name = "content") val content: String,
    @Json(name = "createdAt") val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class GroupMessagesResponse(
    @Json(name = "messages") val messages: List<GroupMessageDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CreateGroupRequest(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "avatar") val avatar: String? = "👥",
    @Json(name = "memberIds") val memberIds: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CreateGroupResponse(
    @Json(name = "group") val group: GroupDto
)

@JsonClass(generateAdapter = true)
data class UpdateGroupRequest(
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "avatar") val avatar: String? = null
)

@JsonClass(generateAdapter = true)
data class SendGroupMessageRequest(
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class SendGroupMessageResponse(
    @Json(name = "message") val message: GroupMessageDto
)

@JsonClass(generateAdapter = true)
data class AddGroupMemberRequest(
    @Json(name = "userId") val userId: String
)

@JsonClass(generateAdapter = true)
data class TransferGroupOwnershipRequest(
    @Json(name = "newOwnerId") val newOwnerId: String
)

// ----------------- APP OTA UPDATES -----------------

@JsonClass(generateAdapter = true)
data class AppVersionResponse(
    @Json(name = "versionCode") val versionCode: Int = 1,
    @Json(name = "versionName") val versionName: String = "1.0",
    @Json(name = "apkUrl") val apkUrl: String = "",
    @Json(name = "changelog") val changelog: String = "",
    @Json(name = "forceUpdate") val forceUpdate: Boolean = false,
    @Json(name = "minSupportedVersionCode") val minSupportedVersionCode: Int = 1
)


