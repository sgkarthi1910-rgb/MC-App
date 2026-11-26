package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val user: StoredUser? = null,
    val role: String = "MEMBER",
    val mustChangePassword: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class TgzViewModel(
    private val repository: ServerRepository,
    private val backendRepository: TgzBackendRepository? = null,
    private val sessionManager: SessionManager? = null
) : ViewModel() {

    companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 120_000L
        private const val AUTO_REFRESH_SECONDS = 120
    }

    private val refreshMutex = Mutex()
    private var autoRefreshJob: Job? = null
    private var countdownJob: Job? = null

    // 1. BASELINE UI STATE (Server telemetry & Discord status)
    private val _uiState = MutableStateFlow(
        DashboardUiState(
            isInitialLoading = true,
            isRefreshing = false,
            nextRefreshSeconds = AUTO_REFRESH_SECONDS,
            serverAddress = repository.config.fullServerAddress,
            discordInviteUrl = repository.config.discordInviteUrl
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _playerSearchQuery = MutableStateFlow("")
    val playerSearchQuery: StateFlow<String> = _playerSearchQuery.asStateFlow()

    private val _selectedStoreCategory = MutableStateFlow(StoreCategory.ALL)
    val selectedStoreCategory: StateFlow<StoreCategory> = _selectedStoreCategory.asStateFlow()

    private val _expandedFaqIds = MutableStateFlow(setOf("faq_join"))
    val expandedFaqIds: StateFlow<Set<String>> = _expandedFaqIds.asStateFlow()

    // 2. AUTHENTICATION STATE
    private val _authState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = sessionManager?.hasActiveSession() ?: false,
            user = sessionManager?.getUser(),
            role = sessionManager?.getUser()?.role ?: "MEMBER",
            mustChangePassword = sessionManager?.getUser()?.mustChangePassword ?: false
        )
    )
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _activeSessions = MutableStateFlow<List<SessionItemDto>>(emptyList())
    val activeSessions: StateFlow<List<SessionItemDto>> = _activeSessions.asStateFlow()

    // 3. ANNOUNCEMENTS STATE
    private val _announcements = MutableStateFlow<List<AnnouncementDto>>(emptyList())
    val announcements: StateFlow<List<AnnouncementDto>> = _announcements.asStateFlow()

    private val _latestAnnouncement = MutableStateFlow<AnnouncementDto?>(null)
    val latestAnnouncement: StateFlow<AnnouncementDto?> = _latestAnnouncement.asStateFlow()

    private val _isAnnouncementsLoading = MutableStateFlow(false)
    val isAnnouncementsLoading: StateFlow<Boolean> = _isAnnouncementsLoading.asStateFlow()

    // 4. MESSAGING STATE
    private val _conversations = MutableStateFlow<List<ConversationDto>>(emptyList())
    val conversations: StateFlow<List<ConversationDto>> = _conversations.asStateFlow()

    private val _currentConversation = MutableStateFlow<ConversationDetailResponse?>(null)
    val currentConversation: StateFlow<ConversationDetailResponse?> = _currentConversation.asStateFlow()

    private val _unreadMessagesCount = MutableStateFlow(0)
    val unreadMessagesCount: StateFlow<Int> = _unreadMessagesCount.asStateFlow()

    private val _userSearchResults = MutableStateFlow<List<UserSearchDto>>(emptyList())
    val userSearchResults: StateFlow<List<UserSearchDto>> = _userSearchResults.asStateFlow()

    private val _isSearchingUsers = MutableStateFlow(false)
    val isSearchingUsers: StateFlow<Boolean> = _isSearchingUsers.asStateFlow()

    private val _isMessagingLoading = MutableStateFlow(false)
    val isMessagingLoading: StateFlow<Boolean> = _isMessagingLoading.asStateFlow()

    // 5. PLAYTIME REWARDS (5 CRATES ONLY)
    private val _rewards = MutableStateFlow<List<PlaytimeRewardDto>>(emptyList())
    val rewards: StateFlow<List<PlaytimeRewardDto>> = _rewards.asStateFlow()

    private val _userPlaytimeMinutes = MutableStateFlow(0)
    val userPlaytimeMinutes: StateFlow<Int> = _userPlaytimeMinutes.asStateFlow()

    private val _claimHistory = MutableStateFlow<List<ClaimHistoryItemDto>>(emptyList())
    val claimHistory: StateFlow<List<ClaimHistoryItemDto>> = _claimHistory.asStateFlow()

    private val _adminClaims = MutableStateFlow<List<AdminClaimItemDto>>(emptyList())
    val adminClaims: StateFlow<List<AdminClaimItemDto>> = _adminClaims.asStateFlow()

    private val _adminRewardConfigs = MutableStateFlow<List<AdminRewardConfigDto>>(emptyList())
    val adminRewardConfigs: StateFlow<List<AdminRewardConfigDto>> = _adminRewardConfigs.asStateFlow()

    private val _rewardsActionMessage = MutableStateFlow<String?>(null)
    val rewardsActionMessage: StateFlow<String?> = _rewardsActionMessage.asStateFlow()

    private val _isRewardsLoading = MutableStateFlow(false)
    val isRewardsLoading: StateFlow<Boolean> = _isRewardsLoading.asStateFlow()

    // 6. LEADERBOARD STATE
    private val _leaderboardPlayers = MutableStateFlow<List<LeaderboardPlayerDto>>(emptyList())
    val leaderboardPlayers: StateFlow<List<LeaderboardPlayerDto>> = _leaderboardPlayers.asStateFlow()

    private val _leaderboardCategory = MutableStateFlow("playtime")
    val leaderboardCategory: StateFlow<String> = _leaderboardCategory.asStateFlow()

    private val _leaderboardSyncNote = MutableStateFlow<String?>(null)
    val leaderboardSyncNote: StateFlow<String?> = _leaderboardSyncNote.asStateFlow()

    private val _isLeaderboardLoading = MutableStateFlow(false)
    val isLeaderboardLoading: StateFlow<Boolean> = _isLeaderboardLoading.asStateFlow()

    // 7. TEAMS STATE
    private val _myTeam = MutableStateFlow<TeamDetailsDto?>(null)
    val myTeam: StateFlow<TeamDetailsDto?> = _myTeam.asStateFlow()

    private val _inTeam = MutableStateFlow(false)
    val inTeam: StateFlow<Boolean> = _inTeam.asStateFlow()

    private val _publicTeams = MutableStateFlow<List<TeamSummaryDto>>(emptyList())
    val publicTeams: StateFlow<List<TeamSummaryDto>> = _publicTeams.asStateFlow()

    private val _selectedTeamDetails = MutableStateFlow<TeamDetailsDto?>(null)
    val selectedTeamDetails: StateFlow<TeamDetailsDto?> = _selectedTeamDetails.asStateFlow()

    private val _teamMemberSearchResults = MutableStateFlow<List<TeamSearchUserDto>>(emptyList())
    val teamMemberSearchResults: StateFlow<List<TeamSearchUserDto>> = _teamMemberSearchResults.asStateFlow()

    private val _isSearchingTeamMembers = MutableStateFlow(false)
    val isSearchingTeamMembers: StateFlow<Boolean> = _isSearchingTeamMembers.asStateFlow()

    private val _teamInvitations = MutableStateFlow<List<TeamInvitationReceivedDto>>(emptyList())
    val teamInvitations: StateFlow<List<TeamInvitationReceivedDto>> = _teamInvitations.asStateFlow()

    private val _allTeams = MutableStateFlow<List<AdminTeamItemDto>>(emptyList())
    val allTeams: StateFlow<List<AdminTeamItemDto>> = _allTeams.asStateFlow()

    private val _teamActionMessage = MutableStateFlow<String?>(null)
    val teamActionMessage: StateFlow<String?> = _teamActionMessage.asStateFlow()

    private val _isTeamLoading = MutableStateFlow(false)
    val isTeamLoading: StateFlow<Boolean> = _isTeamLoading.asStateFlow()

    // 8. SETTINGS STATE
    private val _memberSettings = MutableStateFlow<MemberSettingsDto?>(null)
    val memberSettings: StateFlow<MemberSettingsDto?> = _memberSettings.asStateFlow()

    private val _adminSettings = MutableStateFlow<Map<String, String>>(emptyMap())
    val adminSettings: StateFlow<Map<String, String>> = _adminSettings.asStateFlow()

    private val _userManagementList = MutableStateFlow<List<UserManagementItemDto>>(emptyList())
    val userManagementList: StateFlow<List<UserManagementItemDto>> = _userManagementList.asStateFlow()

    private val _settingsActionMessage = MutableStateFlow<String?>(null)
    val settingsActionMessage: StateFlow<String?> = _settingsActionMessage.asStateFlow()

    private val _isSettingsLoading = MutableStateFlow(false)
    val isSettingsLoading: StateFlow<Boolean> = _isSettingsLoading.asStateFlow()

    // 9. DASHBOARD STATE
    private val _dashboardSummary = MutableStateFlow<DashboardSummaryResponse?>(null)
    val dashboardSummary: StateFlow<DashboardSummaryResponse?> = _dashboardSummary.asStateFlow()

    private val _isDashboardLoading = MutableStateFlow(false)
    val isDashboardLoading: StateFlow<Boolean> = _isDashboardLoading.asStateFlow()

    // 10. MINECRAFT SERVER STATUS (JAVA & BEDROCK)
    private val _javaServerStatus = MutableStateFlow<MinecraftServerStatusDto?>(null)
    val javaServerStatus: StateFlow<MinecraftServerStatusDto?> = _javaServerStatus.asStateFlow()

    private val _bedrockServerStatus = MutableStateFlow<MinecraftServerStatusDto?>(null)
    val bedrockServerStatus: StateFlow<MinecraftServerStatusDto?> = _bedrockServerStatus.asStateFlow()

    private val _isServerStatusLoading = MutableStateFlow(false)
    val isServerStatusLoading: StateFlow<Boolean> = _isServerStatusLoading.asStateFlow()

    // 11. ADMIN REGISTERED MEMBERS STATE
    private val _adminMembers = MutableStateFlow<List<AdminMemberItemDto>>(emptyList())
    val adminMembers: StateFlow<List<AdminMemberItemDto>> = _adminMembers.asStateFlow()

    private val _adminMembersPagination = MutableStateFlow<AdminMembersPaginationDto?>(null)
    val adminMembersPagination: StateFlow<AdminMembersPaginationDto?> = _adminMembersPagination.asStateFlow()

    private val _adminMemberDetails = MutableStateFlow<AdminMemberDetailsResponse?>(null)
    val adminMemberDetails: StateFlow<AdminMemberDetailsResponse?> = _adminMemberDetails.asStateFlow()

    private val _isAdminMembersLoading = MutableStateFlow(false)
    val isAdminMembersLoading: StateFlow<Boolean> = _isAdminMembersLoading.asStateFlow()

    // 12. GROUP CHATS STATE
    private val _groups = MutableStateFlow<List<GroupDto>>(emptyList())
    val groups: StateFlow<List<GroupDto>> = _groups.asStateFlow()

    private val _currentGroupDetails = MutableStateFlow<GroupDetailResponse?>(null)
    val currentGroupDetails: StateFlow<GroupDetailResponse?> = _currentGroupDetails.asStateFlow()

    private val _currentGroupMessages = MutableStateFlow<List<GroupMessageDto>>(emptyList())
    val currentGroupMessages: StateFlow<List<GroupMessageDto>> = _currentGroupMessages.asStateFlow()

    private val _activeGroupId = MutableStateFlow<String?>(null)
    val activeGroupId: StateFlow<String?> = _activeGroupId.asStateFlow()

    private val _isGroupsLoading = MutableStateFlow(false)
    val isGroupsLoading: StateFlow<Boolean> = _isGroupsLoading.asStateFlow()

    private val _groupActionMessage = MutableStateFlow<String?>(null)
    val groupActionMessage: StateFlow<String?> = _groupActionMessage.asStateFlow()

    private val _appUpdateInfo = MutableStateFlow<AppVersionResponse?>(null)
    val appUpdateInfo: StateFlow<AppVersionResponse?> = _appUpdateInfo.asStateFlow()

    init {
        startAutoRefreshCycle()
        fetchJavaServerStatus()
        fetchBedrockServerStatus()
        checkForAppUpdate()
        if (sessionManager?.hasActiveSession() == true) {
            refreshSessionUser()
            loadDashboardSummary()
            loadUnreadMessagesCount()
            loadGroups()
        }
    }

    fun checkForAppUpdate() {
        viewModelScope.launch {
            val repo = backendRepository ?: return@launch
            repo.getAppVersion().onSuccess { version ->
                if (version.versionCode > BuildConfig.VERSION_CODE) {
                    _appUpdateInfo.value = version
                }
            }.onFailure {
                // Silently ignore network failures on version polling
            }
        }
    }

    fun dismissAppUpdate() {
        val info = _appUpdateInfo.value
        if (info != null && !info.forceUpdate && BuildConfig.VERSION_CODE >= info.minSupportedVersionCode) {
            _appUpdateInfo.value = null
        }
    }

    private val _backendUrl = MutableStateFlow(sessionManager?.getBackendUrl() ?: "https://mc-app-ten.vercel.app/")
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    fun getBackendUrl(): String = _backendUrl.value

    fun setBackendUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        sessionManager?.setBackendUrl(normalized)
        _backendUrl.value = normalized
    }

    // ----------------- AUTHENTICATION ACTIONS -----------------

    fun login(id: String, pass: String, onSuccess: () -> Unit = {}) {
        val repo = backendRepository ?: run {
            _authState.update { it.copy(errorMessage = "Backend repository not initialized") }
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repo.login(id, pass)
            result.onSuccess { resp ->
                if (resp.success && resp.user != null) {
                    val user = StoredUser(
                        id = resp.user.id,
                        displayName = resp.user.displayName,
                        role = resp.user.role,
                        mustChangePassword = resp.user.mustChangePassword,
                        status = resp.user.status,
                        createdAt = resp.user.createdAt
                    )
                    _authState.update {
                        it.copy(
                            isLoggedIn = true,
                            user = user,
                            role = user.role,
                            mustChangePassword = user.mustChangePassword,
                            isLoading = false,
                            errorMessage = null,
                            successMessage = "Welcome, ${user.displayName}!"
                        )
                    }
                    loadDashboardSummary()
                    loadUnreadMessagesCount()
                    onSuccess()
                } else {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = resp.error ?: resp.message ?: "Login failed"
                        )
                    }
                }
            }.onFailure { err ->
                _authState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Network connection error"
                    )
                }
            }
        }
    }

    fun signup(
        id: String,
        displayName: String,
        pass: String,
        confirmPass: String,
        onSuccess: () -> Unit = {}
    ) {
        val repo = backendRepository ?: run {
            _authState.update { it.copy(errorMessage = "Backend repository not initialized") }
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repo.signup(id, displayName, pass, confirmPass)
            result.onSuccess { resp ->
                if (resp.success && resp.user != null) {
                    val user = StoredUser(
                        id = resp.user.id,
                        displayName = resp.user.displayName,
                        role = resp.user.role,
                        mustChangePassword = resp.user.mustChangePassword,
                        status = resp.user.status,
                        createdAt = resp.user.createdAt
                    )
                    _authState.update {
                        it.copy(
                            isLoggedIn = true,
                            user = user,
                            role = user.role,
                            mustChangePassword = false,
                            isLoading = false,
                            errorMessage = null,
                            successMessage = "Account created successfully!"
                        )
                    }
                    loadDashboardSummary()
                    onSuccess()
                } else {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = resp.error ?: resp.message ?: "Registration failed"
                        )
                    }
                }
            }.onFailure { err ->
                _authState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Network error during signup"
                    )
                }
            }
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            backendRepository?.logout()
            _authState.update {
                AuthUiState(isLoggedIn = false, user = null, role = "MEMBER", mustChangePassword = false)
            }
            _dashboardSummary.value = null
            _myTeam.value = null
            _inTeam.value = false
            _conversations.value = emptyList()
            _currentConversation.value = null
            _activeSessions.value = emptyList()
            onComplete()
        }
    }

    fun changePassword(current: String?, newPass: String, confirmPass: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repo.changePassword(current, newPass, confirmPass)
            result.onSuccess { resp ->
                if (resp.success) {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            mustChangePassword = false,
                            user = it.user?.copy(mustChangePassword = false),
                            successMessage = "Password updated successfully!"
                        )
                    }
                } else {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = resp.error ?: "Failed to update password"
                        )
                    }
                }
            }.onFailure { err ->
                _authState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Network error"
                    )
                }
            }
        }
    }

    fun clearAuthMessage() {
        _authState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun refreshSessionUser() {
        viewModelScope.launch {
            backendRepository?.refreshCurrentUser()?.onSuccess { resp ->
                if (resp.user != null) {
                    val user = StoredUser(
                        id = resp.user.id,
                        displayName = resp.user.displayName,
                        role = resp.user.role,
                        mustChangePassword = resp.user.mustChangePassword,
                        status = resp.user.status,
                        createdAt = resp.user.createdAt
                    )
                    _authState.update {
                        it.copy(
                            isLoggedIn = true,
                            user = user,
                            role = user.role,
                            mustChangePassword = user.mustChangePassword
                        )
                    }
                }
            }
        }
    }

    fun loadSessions(all: Boolean = false) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.getSessions(all).onSuccess { list ->
                _activeSessions.value = list
            }
        }
    }

    fun revokeSession(sessionId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.revokeSession(sessionId).onSuccess {
                loadSessions(authState.value.role == "ADMIN")
            }
        }
    }

    // ----------------- ANNOUNCEMENTS ACTIONS -----------------

    fun loadAnnouncements() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isAnnouncementsLoading.value = true
            repo.getAnnouncements().onSuccess { list ->
                _announcements.value = list
                _latestAnnouncement.value = list.firstOrNull()
            }
            _isAnnouncementsLoading.value = false
        }
    }

    fun createAnnouncement(title: String, content: String, category: String, isPinned: Boolean, onSuccess: () -> Unit = {}) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isAnnouncementsLoading.value = true
            repo.createAnnouncement(title, content, category, isPinned).onSuccess {
                loadAnnouncements()
                onSuccess()
            }
            _isAnnouncementsLoading.value = false
        }
    }

    fun deleteAnnouncement(id: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.deleteAnnouncement(id).onSuccess {
                loadAnnouncements()
            }
        }
    }

    // ----------------- MESSAGING ACTIONS -----------------

    fun loadConversations() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isMessagingLoading.value = true
            repo.getConversations().onSuccess { list ->
                _conversations.value = list
            }
            _isMessagingLoading.value = false
            loadUnreadMessagesCount()
        }
    }

    fun openConversation(withUserId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isMessagingLoading.value = true
            repo.getConversationMessages(withUserId).onSuccess { detail ->
                _currentConversation.value = detail
                loadUnreadMessagesCount()
            }
            _isMessagingLoading.value = false
        }
    }

    fun sendMessage(recipientId: String, content: String, onSent: () -> Unit = {}) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.sendMessage(recipientId, content).onSuccess {
                openConversation(recipientId)
                loadConversations()
                onSent()
            }
        }
    }

    fun deleteMessage(id: String, currentPartnerId: String? = null) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.deleteMessage(id).onSuccess {
                if (currentPartnerId != null) {
                    openConversation(currentPartnerId)
                }
                loadConversations()
            }
        }
    }

    fun searchUsers(query: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isSearchingUsers.value = true
            repo.searchUsers(query).onSuccess { users ->
                _userSearchResults.value = users
            }
            _isSearchingUsers.value = false
        }
    }

    fun clearUserSearchResults() {
        _userSearchResults.value = emptyList()
    }

    fun closeConversation() {
        _currentConversation.value = null
    }

    fun deleteConversation(withUserId: String, onDeleted: () -> Unit = {}) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.deleteConversation(withUserId).onSuccess {
                _currentConversation.value = null
                loadConversations()
                onDeleted()
            }
        }
    }

    fun loadUnreadMessagesCount() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.getUnreadMessagesCount().onSuccess { count ->
                _unreadMessagesCount.value = count
            }
        }
    }

    // ----------------- PLAYTIME REWARDS ACTIONS (5 CRATES ONLY) -----------------

    fun loadRewards() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isRewardsLoading.value = true
            repo.getRewards().onSuccess { resp ->
                _rewards.value = resp.rewards
                _userPlaytimeMinutes.value = resp.userPlaytimeMinutes
            }
            _isRewardsLoading.value = false
        }
    }

    fun claimReward(rewardId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isRewardsLoading.value = true
            _rewardsActionMessage.value = null
            repo.claimReward(rewardId).onSuccess { resp ->
                if (resp.success) {
                    _rewardsActionMessage.value = resp.message ?: "Crate key voucher claimed!"
                    loadRewards()
                    loadRewardHistory()
                } else {
                    _rewardsActionMessage.value = resp.error ?: "Unable to claim reward"
                }
            }.onFailure { err ->
                _rewardsActionMessage.value = err.localizedMessage ?: "Failed to claim reward"
            }
            _isRewardsLoading.value = false
        }
    }

    fun loadRewardHistory() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.getRewardHistory().onSuccess { list ->
                _claimHistory.value = list
            }
        }
    }

    fun loadAdminClaims() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.getAdminClaims().onSuccess { list ->
                _adminClaims.value = list
            }
        }
    }

    fun updateClaimStatus(claimId: String, status: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.updateClaimStatus(claimId, status).onSuccess {
                loadAdminClaims()
            }
        }
    }

    fun loadAdminRewardConfigs() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.getAdminRewardConfigs().onSuccess { list ->
                _adminRewardConfigs.value = list
            }
        }
    }

    fun updateRewardConfig(rewardId: String, minutes: Int?) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.updateRewardConfig(rewardId, minutes).onSuccess {
                loadAdminRewardConfigs()
                loadRewards()
            }
        }
    }

    fun clearRewardsMessage() {
        _rewardsActionMessage.value = null
    }

    // ----------------- LEADERBOARD ACTIONS -----------------

    fun loadLeaderboard(category: String = "playtime") {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isLeaderboardLoading.value = true
            _leaderboardCategory.value = category
            repo.getLeaderboard(category).onSuccess { resp ->
                _leaderboardPlayers.value = resp.leaderboard
                _leaderboardSyncNote.value = resp.syncNote
            }
            _isLeaderboardLoading.value = false
        }
    }

    // ----------------- TEAMS ACTIONS -----------------

    fun loadPublicTeams() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isTeamLoading.value = true
            repo.getPublicTeams().onSuccess { teams ->
                _publicTeams.value = teams
            }
            _isTeamLoading.value = false
        }
    }

    fun loadTeamDetails(teamId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isTeamLoading.value = true
            repo.getTeamDetails(teamId).onSuccess { details ->
                _selectedTeamDetails.value = details
            }
            _isTeamLoading.value = false
        }
    }

    fun clearSelectedTeamDetails() {
        _selectedTeamDetails.value = null
    }

    fun searchTeamMembers(query: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isSearchingTeamMembers.value = true
            repo.searchTeamMembers(query).onSuccess { users ->
                _teamMemberSearchResults.value = users
            }
            _isSearchingTeamMembers.value = false
        }
    }

    fun clearTeamMemberSearchResults() {
        _teamMemberSearchResults.value = emptyList()
    }

    fun loadMyTeam() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isTeamLoading.value = true
            repo.getMyTeam().onSuccess { resp ->
                _inTeam.value = resp.inTeam
                _myTeam.value = resp.team
            }
            _isTeamLoading.value = false
        }
    }

    fun createTeam(
        name: String,
        desc: String?,
        logo: String = "shield",
        status: String = "ACTIVE",
        onSuccess: () -> Unit = {}
    ) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isTeamLoading.value = true
            _teamActionMessage.value = null
            repo.createTeam(name, desc, logo, status).onSuccess { resp ->
                if (resp.success) {
                    _teamActionMessage.value = resp.message
                    loadMyTeam()
                    loadPublicTeams()
                    onSuccess()
                } else {
                    _teamActionMessage.value = resp.error ?: "Failed to create team"
                }
            }.onFailure { err ->
                _teamActionMessage.value = err.localizedMessage ?: "Network error creating team"
            }
            _isTeamLoading.value = false
        }
    }

    fun editMyTeam(
        desc: String?,
        logo: String?,
        status: String?,
        onSuccess: () -> Unit = {}
    ) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isTeamLoading.value = true
            _teamActionMessage.value = null
            repo.editMyTeam(desc, logo, status).onSuccess { resp ->
                if (resp.success) {
                    _teamActionMessage.value = resp.message ?: "Clan updated successfully"
                    loadMyTeam()
                    loadPublicTeams()
                    onSuccess()
                } else {
                    _teamActionMessage.value = resp.error ?: "Failed to update team"
                }
            }.onFailure { err ->
                _teamActionMessage.value = err.localizedMessage ?: "Network error updating team"
            }
            _isTeamLoading.value = false
        }
    }

    fun disbandMyTeam(onSuccess: () -> Unit = {}) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isTeamLoading.value = true
            repo.disbandMyTeam().onSuccess { resp ->
                _teamActionMessage.value = resp.message ?: "Team disbanded"
                loadMyTeam()
                loadPublicTeams()
                onSuccess()
            }
            _isTeamLoading.value = false
        }
    }

    fun inviteTeamMember(userId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _teamActionMessage.value = null
            repo.inviteTeamMember(userId).onSuccess { resp ->
                _teamActionMessage.value = if (resp.success) resp.message else (resp.error ?: "Invite failed")
                loadMyTeam()
            }.onFailure { err ->
                _teamActionMessage.value = err.localizedMessage ?: "Failed to invite member"
            }
        }
    }

    fun loadTeamInvitations() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.getTeamInvitations().onSuccess { list ->
                _teamInvitations.value = list
            }
        }
    }

    fun respondInvitation(inviteId: String, accept: Boolean) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.respondInvitation(inviteId, accept).onSuccess {
                loadTeamInvitations()
                loadMyTeam()
            }
        }
    }

    fun leaveTeam() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.leaveTeam().onSuccess {
                loadMyTeam()
            }
        }
    }

    fun removeTeamMember(memberId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.removeTeamMember(memberId).onSuccess {
                loadMyTeam()
            }
        }
    }

    fun transferTeamOwnership(newOwnerId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.transferTeamOwnership(newOwnerId).onSuccess {
                loadMyTeam()
            }
        }
    }

    fun loadAllTeams() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.getAllTeams().onSuccess { list ->
                _allTeams.value = list
            }
        }
    }

    fun deleteTeam(teamId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.deleteTeam(teamId).onSuccess {
                loadAllTeams()
            }
        }
    }

    fun clearTeamMessage() {
        _teamActionMessage.value = null
    }

    // ----------------- SETTINGS ACTIONS -----------------

    fun loadMemberSettings() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isSettingsLoading.value = true
            repo.getMemberSettings().onSuccess { settings ->
                _memberSettings.value = settings
            }
            _isSettingsLoading.value = false
        }
    }

    fun loadAdminSettings() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isSettingsLoading.value = true
            repo.getAdminSettings().onSuccess { map ->
                _adminSettings.value = map
            }
            _isSettingsLoading.value = false
        }
    }

    fun updateAdminSettings(settings: Map<String, String>) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isSettingsLoading.value = true
            repo.updateAdminSettings(settings).onSuccess { resp ->
                _settingsActionMessage.value = if (resp.success) "✓ Update published to backend successfully!" else (resp.error ?: "Failed to save settings")
                loadAdminSettings()
                checkForAppUpdate()
            }.onFailure { err ->
                _settingsActionMessage.value = "Failed to publish: ${err.localizedMessage ?: "Network error"}"
            }
            _isSettingsLoading.value = false
        }
    }

    fun loadUserManagementList() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.getUserManagementList().onSuccess { list ->
                _userManagementList.value = list
            }
        }
    }

    fun updateUserRole(userId: String, role: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.updateUserRole(userId, role).onSuccess { resp ->
                _settingsActionMessage.value = if (resp.success) resp.message else resp.error
                loadUserManagementList()
            }
        }
    }

    fun updateUserStatus(userId: String, status: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.updateUserStatus(userId, status).onSuccess { resp ->
                _settingsActionMessage.value = if (resp.success) resp.message else resp.error
                loadUserManagementList()
            }
        }
    }

    fun clearSettingsMessage() {
        _settingsActionMessage.value = null
    }

    // ----------------- DASHBOARD ACTIONS -----------------

    fun loadDashboardSummary() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isDashboardLoading.value = true
            repo.getDashboardSummary().onSuccess { summary ->
                _dashboardSummary.value = summary
            }
            _isDashboardLoading.value = false
        }
    }

    // ----------------- MINECRAFT STATUS ACTIONS -----------------

    fun fetchJavaServerStatus() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isServerStatusLoading.value = true
            repo.getJavaServerStatus().onSuccess { status ->
                _javaServerStatus.value = status
            }
            _isServerStatusLoading.value = false
        }
    }

    fun fetchBedrockServerStatus() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isServerStatusLoading.value = true
            repo.getBedrockServerStatus().onSuccess { status ->
                _bedrockServerStatus.value = status
            }
            _isServerStatusLoading.value = false
        }
    }

    // ----------------- ADMIN REGISTERED MEMBERS ACTIONS -----------------

    fun loadAdminRegisteredMembers(
        page: Int = 1,
        search: String? = null,
        role: String? = null,
        status: String? = null,
        sort: String = "created_at",
        order: String = "desc"
    ) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isAdminMembersLoading.value = true
            repo.getAdminRegisteredMembers(
                search = search?.takeIf { it.isNotBlank() },
                role = role?.takeIf { it.isNotBlank() },
                status = status?.takeIf { it.isNotBlank() },
                sort = sort,
                order = order,
                page = page,
                limit = 20
            ).onSuccess { resp ->
                _adminMembers.value = resp.users
                _adminMembersPagination.value = resp.pagination
            }.onFailure {
                _adminMembers.value = emptyList()
            }
            _isAdminMembersLoading.value = false
        }
    }

    fun loadAdminMemberDetails(userId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isAdminMembersLoading.value = true
            repo.getAdminMemberDetails(userId).onSuccess { details ->
                _adminMemberDetails.value = details
            }.onFailure {
                _adminMemberDetails.value = null
            }
            _isAdminMembersLoading.value = false
        }
    }

    fun clearAdminMemberDetails() {
        _adminMemberDetails.value = null
    }

    fun updateAdminMemberRole(userId: String, newRole: String, onDone: () -> Unit = {}) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.updateUserRole(userId, newRole).onSuccess {
                loadAdminRegisteredMembers()
                onDone()
            }
        }
    }

    fun updateAdminMemberStatus(userId: String, newStatus: String, onDone: () -> Unit = {}) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.updateUserStatus(userId, newStatus).onSuccess {
                loadAdminRegisteredMembers()
                onDone()
            }
        }
    }

    // ----------------- GROUP CHAT ACTIONS -----------------

    fun loadGroups() {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isGroupsLoading.value = true
            repo.getGroups().onSuccess { list ->
                _groups.value = list
            }
            _isGroupsLoading.value = false
        }
    }

    fun selectGroup(groupId: String) {
        _activeGroupId.value = groupId
        loadGroupDetails(groupId)
        loadGroupMessages(groupId)
    }

    fun loadGroupDetails(groupId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.getGroupDetails(groupId).onSuccess { details ->
                _currentGroupDetails.value = details
            }
        }
    }

    fun loadGroupMessages(groupId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.getGroupMessages(groupId).onSuccess { msgs ->
                _currentGroupMessages.value = msgs
            }
        }
    }

    fun createGroup(
        name: String,
        description: String? = null,
        avatar: String? = "👥",
        memberIds: List<String> = emptyList(),
        onSuccess: (GroupDto) -> Unit = {}
    ) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            _isGroupsLoading.value = true
            repo.createGroup(name, description, avatar, memberIds).onSuccess { grp ->
                loadGroups()
                selectGroup(grp.id)
                _groupActionMessage.value = "Group created successfully!"
                onSuccess(grp)
            }.onFailure { err ->
                _groupActionMessage.value = err.localizedMessage ?: "Failed to create group"
            }
            _isGroupsLoading.value = false
        }
    }

    fun sendGroupMessage(groupId: String, content: String) {
        val repo = backendRepository ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            repo.sendGroupMessage(groupId, content.trim()).onSuccess { msg ->
                _currentGroupMessages.update { it + msg }
                loadGroups() // Update preview and ordering
            }
        }
    }

    fun updateGroupInfo(groupId: String, name: String?, description: String?, avatar: String?) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.updateGroup(groupId, name, description, avatar).onSuccess {
                loadGroupDetails(groupId)
                loadGroups()
            }
        }
    }

    fun addGroupMember(groupId: String, userId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.addGroupMember(groupId, userId).onSuccess {
                loadGroupDetails(groupId)
            }
        }
    }

    fun removeGroupMember(groupId: String, userId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.removeGroupMember(groupId, userId).onSuccess {
                loadGroupDetails(groupId)
            }
        }
    }

    fun transferGroupOwnership(groupId: String, newOwnerId: String) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.transferGroupOwnership(groupId, newOwnerId).onSuccess {
                loadGroupDetails(groupId)
                loadGroups()
            }
        }
    }

    fun deleteGroup(groupId: String, onDone: () -> Unit = {}) {
        val repo = backendRepository ?: return
        viewModelScope.launch {
            repo.deleteGroup(groupId).onSuccess {
                _activeGroupId.value = null
                _currentGroupDetails.value = null
                _currentGroupMessages.value = emptyList()
                loadGroups()
                onDone()
            }
        }
    }

    fun clearGroupActionMessage() {
        _groupActionMessage.value = null
    }

    // ----------------- EXISTING BASELINE CONTROLS -----------------

    fun refreshNow() {
        viewModelScope.launch {
            performRefresh(resetSchedule = true)
            fetchJavaServerStatus()
            fetchBedrockServerStatus()
            checkForAppUpdate()
            if (authState.value.isLoggedIn) {
                loadDashboardSummary()
                loadUnreadMessagesCount()
                loadGroups()
            }
        }
    }

    fun updatePlayerSearch(query: String) {
        _playerSearchQuery.value = query
    }

    fun selectStoreCategory(category: StoreCategory) {
        _selectedStoreCategory.value = category
    }

    fun toggleFaqExpanded(faqId: String) {
        _expandedFaqIds.update { current ->
            if (faqId in current) current - faqId else current + faqId
        }
    }

    private fun startAutoRefreshCycle() {
        autoRefreshJob?.cancel()
        countdownJob?.cancel()

        autoRefreshJob = viewModelScope.launch {
            performRefresh(resetSchedule = false)
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                performRefresh(resetSchedule = false)
            }
        }

        startCountdownTicker()
    }

    private fun startCountdownTicker() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                _uiState.update { state ->
                    val next = if (state.isRefreshing) {
                        AUTO_REFRESH_SECONDS
                    } else {
                        (state.nextRefreshSeconds - 1).coerceAtLeast(0)
                    }
                    state.copy(nextRefreshSeconds = next)
                }
            }
        }
    }

    private suspend fun performRefresh(resetSchedule: Boolean) {
        if (!refreshMutex.tryLock()) return
        try {
            _uiState.update { it.copy(isRefreshing = true) }
            val snapshot = repository.fetchLiveSnapshot()
            val nowFormatted = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

            _uiState.update { current ->
                current.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    lastUpdatedFormatted = nowFormatted,
                    nextRefreshSeconds = if (resetSchedule) AUTO_REFRESH_SECONDS else current.nextRefreshSeconds,
                    discordStats = snapshot.discordStats ?: current.discordStats,
                    minecraftStatus = snapshot.minecraftStatus ?: current.minecraftStatus,
                    discordError = snapshot.discordError,
                    minecraftError = snapshot.minecraftError
                )
            }
        } finally {
            refreshMutex.unlock()
        }
    }
}
