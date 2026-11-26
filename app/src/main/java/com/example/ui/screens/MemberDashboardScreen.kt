package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ObsidianCard
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.VoxelHeartIcon
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.EmeraldDim
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TgzViewModel

@Composable
fun MemberDashboardScreen(
    viewModel: TgzViewModel,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val isLoading by viewModel.isDashboardLoading.collectAsStateWithLifecycle()
    val dashboardError by viewModel.dashboardError.collectAsStateWithLifecycle()
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
            // 1. WELCOME & PROFILE HEADER
            item {
                ObsidianCard(
                    borderColor = EmeraldGreen.copy(alpha = 0.5f),
                    accentGlow = EmeraldGreen
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldGreen.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldGreen)
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = authState.user?.displayName ?: "Survivor",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 18.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ID: @${authState.user?.id ?: "unknown"}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = EmeraldGreen
                                )
                            }
                        }

                        // Role Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DeepCharcoal,
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "MEMBER",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 11.sp,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryMiniStat(
                            label = "PLAYTIME",
                            value = summary?.playtime?.formatted ?: if (isLoading) "..." else "0h 0m",
                            color = MinecraftGold
                        )
                        SummaryMiniStat(
                            label = "HEARTS",
                            value = summary?.hearts?.let { "$it ♥" } ?: if (isLoading) "..." else "10 ♥",
                            color = NetherRed
                        )
                        SummaryMiniStat(
                            label = "KILLS",
                            value = summary?.kills?.let { "$it" } ?: if (isLoading) "..." else "0",
                            color = EmeraldGreen
                        )
                        SummaryMiniStat(
                            label = "RANK",
                            value = summary?.leaderboardRank?.let { "#$it" } ?: if (isLoading) "..." else "-",
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }

            if (summary == null && isLoading) {
                item {
                    ObsidianCard(borderColor = EmeraldGreen.copy(alpha = 0.4f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = EmeraldGreen, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("LOADING MEMBER DASHBOARD...", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, color = EmeraldGreen)
                        }
                    }
                }
            } else if (dashboardError != null || (summary == null && !isLoading)) {
                item {
                    ObsidianCard(borderColor = NetherRed.copy(alpha = 0.5f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "COULD NOT LOAD DASHBOARD DATA",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 12.sp,
                                color = NetherRed
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dashboardError ?: "Please check your connection and retry.",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.loadDashboardSummary() },
                                colors = ButtonDefaults.buttonColors(containerColor = NetherRed),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("RETRY", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // 2. MINECRAFT SERVER STATUS CARD
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
                                    text = "MINECRAFT SMP",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 14.sp,
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
                                "${uiState.minecraftStatus?.onlinePlayers ?: 0} ONLINE"
                            else "OFFLINE",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 12.sp,
                            color = if (uiState.minecraftStatus?.isOnline == true) EmeraldGreen else NetherRed
                        )
                    }
                }
            }

            // 3. PLAYTIME REWARDS PROGRESS (5 CRATES ONLY)
            item {
                ObsidianCard(
                    borderColor = MinecraftGold.copy(alpha = 0.3f),
                    accentGlow = MinecraftGold
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MinecraftGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PLAYTIME CRATES",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 14.sp,
                                color = MinecraftGold
                            )
                        }

                        TextButton(onClick = { onNavigateToRoute("rewards") }) {
                            Text(
                                text = "VIEW ALL CRATES →",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 11.sp,
                                color = EmeraldGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val crateProgress = summary?.rewardProgress.orEmpty()
                    if (crateProgress.isEmpty()) {
                        Text(
                            text = "Loading authoritative playtime crate rewards...",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            crateProgress.take(5).forEach { crate ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = crate.name,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )

                                    val statusColor = when (crate.status) {
                                        "CLAIMED" -> EmeraldGreen
                                        "AVAILABLE" -> MinecraftGold
                                        "AWAITING_CONFIG" -> TextMuted
                                        else -> TextSecondary
                                    }

                                    Text(
                                        text = when (crate.status) {
                                            "CLAIMED" -> "CLAIMED ✓"
                                            "AVAILABLE" -> "READY TO CLAIM!"
                                            "AWAITING_CONFIG" -> "PENDING CONFIG"
                                            else -> "${crate.progressPercent}%"
                                        },
                                        fontFamily = SilkscreenFontFamily,
                                        fontSize = 11.sp,
                                        color = statusColor
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (crate.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = if (crate.status == "CLAIMED") EmeraldGreen else MinecraftGold,
                                    trackColor = DeepCharcoal
                                )
                            }
                        }
                    }
                }
            }

            // 4. LATEST ANNOUNCEMENT BANNER
            item {
                val latest = summary?.latestAnnouncement
                if (latest != null) {
                    ObsidianCard(
                        modifier = Modifier.clickable { onNavigateToRoute("announcements") },
                        borderColor = EmeraldGreen.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = EmeraldGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ANNOUNCEMENT",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 13.sp,
                                    color = EmeraldGreen
                                )
                            }

                            Text(
                                text = latest.category,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = latest.title,
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = latest.content,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 5. QUICK NAVIGATION GRID
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Messaging Card
                    ObsidianCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToRoute("messages") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = Color(0xFF38BDF8))
                            if ((summary?.unreadMessagesCount ?: 0) > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = NetherRed
                                ) {
                                    Text(
                                        text = "${summary?.unreadMessagesCount}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "MESSAGES",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "In-app community chat",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    // Teams Card
                    ObsidianCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToRoute("teams") }
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = EmeraldGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CLAN TEAMS",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = summary?.team?.name ?: "Create or join team",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryMiniStat(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = SilkscreenFontFamily,
            fontSize = 14.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = TextMuted
        )
    }
}
