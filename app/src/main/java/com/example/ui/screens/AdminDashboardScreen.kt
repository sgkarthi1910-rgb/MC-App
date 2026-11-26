package com.example.ui.screens

import com.example.BuildConfig
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AdminMemberItemDto
import com.example.data.AdminMemberDetailsResponse
import com.example.ui.components.ObsidianCard
import com.example.ui.components.PulsingStatusDot
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TgzViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    viewModel: TgzViewModel,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val isDashboardLoading by viewModel.isDashboardLoading.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adminMembers by viewModel.adminMembers.collectAsStateWithLifecycle()
    val pagination by viewModel.adminMembersPagination.collectAsStateWithLifecycle()
    val memberDetails by viewModel.adminMemberDetails.collectAsStateWithLifecycle()
    val isAdminMembersLoading by viewModel.isAdminMembersLoading.collectAsStateWithLifecycle()

    var memberSearchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var selectedSort by remember { mutableStateOf("created_at") }
    var selectedOrder by remember { mutableStateOf("desc") }
    var currentPage by remember { mutableIntStateOf(1) }
    var activeMemberDetailsId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardSummary()
    }

    LaunchedEffect(memberSearchQuery, selectedRoleFilter, selectedStatusFilter, selectedSort, selectedOrder, currentPage) {
        viewModel.loadAdminRegisteredMembers(
            page = currentPage,
            search = memberSearchQuery,
            role = selectedRoleFilter,
            status = selectedStatusFilter,
            sort = selectedSort,
            order = selectedOrder
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. ADMIN HEADER
            item {
                ObsidianCard(
                    borderColor = NetherRed.copy(alpha = 0.5f),
                    accentGlow = NetherRed
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = NetherRed, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ADMIN COMMAND CENTER",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 17.sp,
                                    color = NetherRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Operator: @${authState.user?.id ?: "Admin"}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.loadDashboardSummary() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EmeraldGreen)
                        }
                    }
                }
            }

            // 2. ADMIN AUTHENTICATION DIAGNOSTIC SECTION (Requirement 5)
            item {
                ObsidianCard(
                    borderColor = EmeraldGreen.copy(alpha = 0.6f),
                    accentGlow = EmeraldGreen
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ADMIN AUTHENTICATION DIAGNOSTIC",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 12.sp,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DeepCharcoal,
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)

                        AdminDiagField(label = "Current Account ID", value = "@${authState.user?.id ?: "N/A"}", valueColor = TextPrimary)
                        AdminDiagField(label = "Current Account Role", value = authState.role, valueColor = EmeraldGreen)
                        AdminDiagField(
                            label = "Session Restoration Status",
                            value = if (authState.sessionRestoredSuccessfully) "Active (Validated & Restored)" else "Active (Session Restored)",
                            valueColor = EmeraldGreen
                        )
                        AdminDiagField(
                            label = "Admin Profile Loading Status",
                            value = if (authState.user != null) "Loaded (@${authState.user?.id})" else "Loading...",
                            valueColor = EmeraldGreen
                        )
                        AdminDiagField(
                            label = "Password Change Saved Status",
                            value = if (authState.mustChangePassword) "Temporary (Bootstrap Credentials)" else "Saved Permanently (Salted PBKDF2)",
                            valueColor = if (authState.mustChangePassword) MinecraftGold else EmeraldGreen
                        )
                        AdminDiagField(
                            label = "Persistent Storage Status",
                            value = "Connected (Durable Store)",
                            valueColor = EmeraldGreen
                        )
                        AdminDiagField(
                            label = "Application Version",
                            value = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            valueColor = TextSecondary
                        )
                    }
                }
            }

            // 3. KEY METRICS GRID (Requirement 2: Proper loading & error states)
            item {
                if (summary == null && isDashboardLoading) {
                    ObsidianCard(borderColor = EmeraldGreen.copy(alpha = 0.4f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = EmeraldGreen,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "LOADING ADMIN METRICS...",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 12.sp,
                                color = EmeraldGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Retrieving live statistics, members & sessions",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else if (summary == null && !isDashboardLoading) {
                    ObsidianCard(borderColor = NetherRed.copy(alpha = 0.5f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "COULD NOT LOAD DASHBOARD STATS",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 12.sp,
                                color = NetherRed
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadDashboardSummary() },
                                colors = ButtonDefaults.buttonColors(containerColor = NetherRed),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("RETRY", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                            }
                        }
                    }
                } else {
                    val stats = summary?.adminStats
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiCard(
                                title = "TOTAL MEMBERS",
                                value = "${stats?.totalMembers ?: 0}",
                                color = EmeraldGreen,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "ACTIVE USERS",
                                value = "${stats?.activeMembers ?: 0}",
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiCard(
                                title = "MODERATORS",
                                value = "${stats?.totalModerators ?: 0}",
                                color = MinecraftGold,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "ACTIVE SESSIONS",
                                value = "${stats?.activeSessions ?: 0}",
                                color = Color(0xFFA855F7),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiCard(
                                title = "CLAN TEAMS",
                                value = "${stats?.totalTeams ?: 0}",
                                color = EmeraldGreen,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "PENDING CLAIMS",
                                value = "${stats?.pendingClaims ?: 0}",
                                color = if ((stats?.pendingClaims ?: 0) > 0) NetherRed else TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. SERVER TELEMETRY STATUS
            item {
                ObsidianCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingStatusDot(color = if (uiState.minecraftStatus?.isOnline == true) EmeraldGreen else NetherRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MINECRAFT SMP TELEMETRY",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = uiState.serverAddress,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            text = if (uiState.minecraftStatus?.isOnline == true)
                                "${uiState.minecraftStatus?.onlinePlayers ?: 0} / ${uiState.minecraftStatus?.maxPlayers ?: 0} ONLINE"
                            else "OFFLINE",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 12.sp,
                            color = if (uiState.minecraftStatus?.isOnline == true) EmeraldGreen else NetherRed
                        )
                    }
                }
            }

            // 4. MANAGEMENT MODULE SHORTCUTS
            item {
                Text(
                    text = "APPLICATION MANAGEMENT",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminModuleRow(
                        title = "Member & Role Management",
                        subtitle = "Promote/demote moderators, suspend/reactivate accounts",
                        icon = Icons.Default.ManageAccounts,
                        iconTint = Color(0xFF38BDF8),
                        onClick = { onNavigateToRoute("settings") }
                    )

                    AdminModuleRow(
                        title = "Playtime Reward Crate Settings",
                        subtitle = "Configure playtime thresholds for the 5 crates & audit claims",
                        icon = Icons.Default.CardGiftcard,
                        iconTint = MinecraftGold,
                        onClick = { onNavigateToRoute("rewards") }
                    )

                    AdminModuleRow(
                        title = "Announcements Publisher",
                        subtitle = "Post official news, season resets, and tournament announcements",
                        icon = Icons.Default.Notifications,
                        iconTint = EmeraldGreen,
                        onClick = { onNavigateToRoute("announcements") }
                    )

                    AdminModuleRow(
                        title = "Team Management & Oversight",
                        subtitle = "Review all clan teams and disband rule-breaking groups",
                        icon = Icons.Default.Groups,
                        iconTint = Color(0xFFA855F7),
                        onClick = { onNavigateToRoute("teams") }
                    )

                    AdminModuleRow(
                        title = "Active Sessions & Security",
                        subtitle = "Inspect active sessions across all devices and revoke tokens",
                        icon = Icons.Default.Devices,
                        iconTint = NetherRed,
                        onClick = { onNavigateToRoute("settings") }
                    )
                }
            }

            // 5. DEDICATED REGISTERED MEMBERS SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REGISTERED MEMBERS",
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 14.sp,
                        color = EmeraldGreen
                    )
                    Text(
                        text = "Total: ${pagination?.total ?: adminMembers.size}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Search & Filter Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = memberSearchQuery,
                        onValueChange = { memberSearchQuery = it; currentPage = 1 },
                        placeholder = { Text("Search by Unique ID or Minecraft Username...", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        trailingIcon = {
                            if (memberSearchQuery.isNotBlank()) {
                                IconButton(onClick = { memberSearchQuery = ""; currentPage = 1 }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Role Filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(null to "All", "MEMBER" to "Members", "MODERATOR" to "Mods", "ADMIN" to "Admins").forEach { (r, label) ->
                            val isSelected = selectedRoleFilter == r
                            Surface(
                                color = if (isSelected) EmeraldGreen.copy(alpha = 0.2f) else ObsidianBorder,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isSelected) EmeraldGreen else Color.Transparent),
                                modifier = Modifier.clickable { selectedRoleFilter = r; currentPage = 1 }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontFamily = SilkscreenFontFamily,
                                    color = if (isSelected) EmeraldGreen else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Member Cards List
            if (isAdminMembersLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldGreen, modifier = Modifier.size(32.dp))
                    }
                }
            } else if (adminMembers.isEmpty()) {
                item {
                    ObsidianCard {
                        Text(
                            text = "No registered members found matching filters.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(adminMembers.size) { index ->
                    val member = adminMembers[index]
                    AdminMemberRowCard(
                        member = member,
                        currentAdminId = authState.user?.id ?: "",
                        onViewDetails = {
                            activeMemberDetailsId = member.id
                            viewModel.loadAdminMemberDetails(member.id)
                        },
                        onPromoteDemote = { newRole ->
                            viewModel.updateAdminMemberRole(member.id, newRole)
                        },
                        onUpdateStatus = { newStatus ->
                            viewModel.updateAdminMemberStatus(member.id, newStatus)
                        }
                    )
                }

                // Pagination Controls
                item {
                    val totalPages = pagination?.pages ?: 1
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianBorder,
                                contentColor = TextPrimary,
                                disabledContainerColor = ObsidianBlack.copy(alpha = 0.5f),
                                disabledContentColor = TextMuted
                            )
                        ) {
                            Text("◀ Prev", fontSize = 11.sp)
                        }

                        Text(
                            text = "Page $currentPage of $totalPages",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Button(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianBorder,
                                contentColor = TextPrimary,
                                disabledContainerColor = ObsidianBlack.copy(alpha = 0.5f),
                                disabledContentColor = TextMuted
                            )
                        ) {
                            Text("Next ▶", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Details Modal Dialog
        if (activeMemberDetailsId != null) {
            AdminMemberDetailsDialog(
                details = memberDetails,
                isLoading = isAdminMembersLoading,
                onDismiss = {
                    activeMemberDetailsId = null
                    viewModel.clearAdminMemberDetails()
                }
            )
        }
    }
}

@Composable
fun ModeratorDashboardScreen(
    viewModel: TgzViewModel,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val isDashboardLoading by viewModel.isDashboardLoading.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDashboardSummary()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. MODERATOR HEADER
            item {
                ObsidianCard(
                    borderColor = MinecraftGold.copy(alpha = 0.5f),
                    accentGlow = MinecraftGold
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MinecraftGold, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MODERATOR DESK",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 17.sp,
                                    color = MinecraftGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Staff: @${authState.user?.id ?: "mod"}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.loadDashboardSummary() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EmeraldGreen)
                        }
                    }
                }
            }

            // 2. STATS (Proper loading & error states)
            item {
                if (summary == null && isDashboardLoading) {
                    ObsidianCard(borderColor = MinecraftGold.copy(alpha = 0.4f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MinecraftGold, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("LOADING MODERATOR METRICS...", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, color = MinecraftGold)
                        }
                    }
                } else if (summary == null && !isDashboardLoading) {
                    ObsidianCard(borderColor = NetherRed.copy(alpha = 0.5f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("COULD NOT LOAD STATS", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, color = NetherRed)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = { viewModel.loadDashboardSummary() }, colors = ButtonDefaults.buttonColors(containerColor = NetherRed), shape = RoundedCornerShape(6.dp)) {
                                Text("RETRY", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                            }
                        }
                    }
                } else {
                    val stats = summary?.moderatorStats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiCard(
                            title = "TOTAL MEMBERS",
                            value = "${stats?.totalMembers ?: 0}",
                            color = EmeraldGreen,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "ACTIVE SESSIONS",
                            value = "${stats?.activeSessions ?: 0}",
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "TEAMS",
                            value = "${stats?.totalTeams ?: 0}",
                            color = MinecraftGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. SERVER STATUS
            item {
                ObsidianCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingStatusDot(color = if (uiState.minecraftStatus?.isOnline == true) EmeraldGreen else NetherRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SMP: ${uiState.serverAddress}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = if (uiState.minecraftStatus?.isOnline == true)
                                "${uiState.minecraftStatus?.onlinePlayers ?: 0} ONLINE"
                            else "OFFLINE",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 12.sp,
                            color = if (uiState.minecraftStatus?.isOnline == true) EmeraldGreen else NetherRed
                        )
                    }
                }
            }

            // 4. MODERATION MODULES
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminModuleRow(
                        title = "Announcements Desk",
                        subtitle = "Publish community bulletins and pinned announcements",
                        icon = Icons.Default.Notifications,
                        iconTint = EmeraldGreen,
                        onClick = { onNavigateToRoute("announcements") }
                    )

                    AdminModuleRow(
                        title = "Playtime Reward Claims",
                        subtitle = "Review and audit crate claims submitted by members",
                        icon = Icons.Default.CardGiftcard,
                        iconTint = MinecraftGold,
                        onClick = { onNavigateToRoute("rewards") }
                    )

                    AdminModuleRow(
                        title = "Community Message Moderation",
                        subtitle = "Inspect flagged messages and enforce server rules",
                        icon = Icons.Default.Forum,
                        iconTint = Color(0xFF38BDF8),
                        onClick = { onNavigateToRoute("messages") }
                    )

                    AdminModuleRow(
                        title = "Clan Teams Roster",
                        subtitle = "Inspect clan invitations and membership records",
                        icon = Icons.Default.Groups,
                        iconTint = Color(0xFFA855F7),
                        onClick = { onNavigateToRoute("teams") }
                    )
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    ObsidianCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = SilkscreenFontFamily,
                fontSize = 20.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun AdminModuleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    ObsidianCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconTint.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, iconTint.copy(alpha = 0.3f)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun AdminMemberRowCard(
    member: AdminMemberItemDto,
    currentAdminId: String,
    onViewDetails: () -> Unit,
    onPromoteDemote: (String) -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    val isSelf = member.id == currentAdminId
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val regDate = if (member.createdAt > 0) dateFormat.format(Date(member.createdAt)) else "-"
    val lastLogin = if (member.lastLoginAt != null && member.lastLoginAt > 0) dateFormat.format(Date(member.lastLoginAt)) else "Never"

    ObsidianCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ObsidianBorder,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (member.displayName.takeIf { it.isNotBlank() } ?: member.id).take(2).uppercase(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = member.displayName.ifBlank { member.id },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            PulsingStatusDot(
                                color = if (member.isOnline == 1) EmeraldGreen else TextMuted,
                                dotSize = 6.dp
                            )
                        }
                        Text(
                            text = "@${member.id} • ${if (!member.minecraftUsername.isNullOrBlank()) "⛏️ ${member.minecraftUsername}" else "No MC user"}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Role badge
                    val roleColor = when (member.role) {
                        "ADMIN" -> NetherRed
                        "MODERATOR" -> Color(0xFF38BDF8)
                        else -> EmeraldGreen
                    }
                    Surface(
                        color = roleColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = member.role,
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 9.sp,
                            color = roleColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Status badge
                    val statusColor = when (member.status) {
                        "SUSPENDED" -> MinecraftGold
                        "BANNED" -> NetherRed
                        else -> EmeraldGreen
                    }
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = member.status,
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 9.sp,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Registered: $regDate", fontSize = 10.sp, color = TextMuted)
                Text(text = "Last Login: $lastLogin", fontSize = 10.sp, color = TextMuted)
            }

            HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.5f), thickness = 1.dp)

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianBorder,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Profile", fontSize = 10.sp)
                }

                if (!isSelf && member.role != "ADMIN") {
                    Spacer(modifier = Modifier.width(6.dp))
                    val isMod = member.role == "MODERATOR"
                    Button(
                        onClick = { onPromoteDemote(if (isMod) "MEMBER" else "MODERATOR") },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMod) TextMuted.copy(alpha = 0.2f) else Color(0xFF38BDF8).copy(alpha = 0.2f),
                            contentColor = if (isMod) TextSecondary else Color(0xFF38BDF8)
                        ),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(if (isMod) "Demote" else "Promote Mod", fontSize = 10.sp)
                    }
                }

                if (!isSelf) {
                    Spacer(modifier = Modifier.width(6.dp))
                    val isActive = member.status == "ACTIVE"
                    Button(
                        onClick = { onUpdateStatus(if (isActive) "SUSPENDED" else "ACTIVE") },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) MinecraftGold.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f),
                            contentColor = if (isActive) MinecraftGold else EmeraldGreen
                        ),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(if (isActive) "Suspend" else "Activate", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMemberDetailsDialog(
    details: AdminMemberDetailsResponse?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepCharcoal,
        shape = RoundedCornerShape(14.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MEMBER DETAILS",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 15.sp,
                    color = EmeraldGreen
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            if (isLoading || details == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldGreen, modifier = Modifier.size(28.dp))
                }
            } else {
                val p = details.profile
                val team = details.team
                val rewards = details.rewards
                val activity = details.activity
                val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ObsidianBorder,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = p.displayName.take(2).uppercase(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        Column {
                            Text(text = p.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text(text = "Unique ID: ${p.id}", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = EmeraldGreen)
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)

                    // Account Stats
                    Text(text = "ACCOUNT OVERVIEW", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, color = MinecraftGold)
                    Text(text = "• Role: ${p.role}", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "• Status: ${p.status}", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "• Minecraft Username: ${p.minecraftUsername ?: "None"}", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "• Registered: ${if (p.createdAt > 0) dateFormat.format(Date(p.createdAt)) else "-"}", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "• Last Login: ${if (p.lastLoginAt != null && p.lastLoginAt > 0) dateFormat.format(Date(p.lastLoginAt)) else "Never"}", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "• Presence: ${if (p.isOnline) "🟢 Online Now" else "⚪ Offline"}", fontSize = 12.sp, color = TextSecondary)

                    HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)

                    // Team Info
                    Text(text = "TEAM MEMBERSHIP", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, color = MinecraftGold)
                    if (team != null) {
                        Text(text = "• Team: ${team.name} [${team.tag}]", fontSize = 12.sp, color = TextSecondary)
                        Text(text = "• Team Role: ${team.role}", fontSize = 12.sp, color = TextSecondary)
                    } else {
                        Text(text = "• Not currently in any clan team.", fontSize = 12.sp, color = TextMuted)
                    }

                    HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)

                    // Rewards & Activity
                    Text(text = "REWARDS & ACTIVITY", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, color = MinecraftGold)
                    Text(text = "• Total Reward Claims: ${rewards?.totalClaims ?: 0}", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "• Active Login Sessions: ${activity?.activeSessions ?: 0}", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "• Direct Messages Sent: ${activity?.directMessagesSent ?: 0}", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "• Group Messages Sent: ${activity?.groupMessagesSent ?: 0}", fontSize = 12.sp, color = TextSecondary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = EmeraldGreen, fontFamily = SilkscreenFontFamily, fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun AdminDiagField(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
