package com.example.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ObsidianCard
import com.example.ui.components.PulsingStatusDot
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TgzViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: TgzViewModel,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
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
                                    text = "Operator: @${authState.user?.id ?: "sg_karthi"}",
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

            // 2. KEY METRICS GRID
            item {
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

            // 2. STATS
            item {
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
