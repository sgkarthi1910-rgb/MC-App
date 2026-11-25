package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DashboardUiState
import com.example.ui.components.HeroLifestealCanvas
import com.example.ui.components.LiveStatsGrid
import com.example.ui.components.LiveTelemetryBar
import com.example.ui.components.MinecraftPlayerAvatar
import com.example.ui.components.ObsidianCard
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.SectionHeading
import com.example.ui.components.VoxelHeartIcon
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.DiscordBlurple
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
import com.example.ui.viewmodel.AuthUiState

@Composable
fun HomeScreen(
    uiState: DashboardUiState,
    authState: AuthUiState? = null,
    ipCopied: Boolean,
    onJoinServer: () -> Unit,
    onJoinDiscord: () -> Unit,
    onCopyIp: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. HERO SECTION
            item {
                HeroBannerCard(
                    uiState = uiState,
                    ipCopied = ipCopied,
                    onJoinServer = onJoinServer,
                    onJoinDiscord = onJoinDiscord,
                    onCopyIp = onCopyIp
                )
            }

            // 2. AUTH & REWARDS PORTAL BANNER
            item {
                if (authState?.isLoggedIn == true) {
                    ObsidianCard(
                        borderColor = when (authState.role) {
                            "ADMIN" -> NetherRed
                            "MODERATOR" -> MinecraftGold
                            else -> EmeraldGreen
                        },
                        accentGlow = EmeraldGreen,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (authState.role) {
                                        "ADMIN" -> NetherRed
                                        "MODERATOR" -> MinecraftGold
                                        else -> EmeraldGreen
                                    }.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = when (authState.role) {
                                                "ADMIN" -> NetherRed
                                                "MODERATOR" -> MinecraftGold
                                                else -> EmeraldGreen
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "LOGGED IN AS @${authState.user?.id?.uppercase() ?: "MEMBER"}",
                                        fontFamily = SilkscreenFontFamily,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Role: ${authState.role} • Status: ${authState.user?.status ?: "ACTIVE"}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = EmeraldGreen
                                    )
                                }
                            }

                            Button(
                                onClick = { onNavigateToRoute("dashboard") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen,
                                    contentColor = OnEmeraldDark
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("MY DASHBOARD", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    ObsidianCard(
                        borderColor = EmeraldGreen.copy(alpha = 0.6f),
                        accentGlow = EmeraldGreen,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TGZ MEMBER & STAFF PORTAL",
                                        fontFamily = SilkscreenFontFamily,
                                        fontSize = 14.sp,
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { onNavigateToRoute("login") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen,
                                        contentColor = OnEmeraldDark
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("SIGN IN", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "Sign in to access your role dashboard, claim 5 official playtime crate rewards, manage clan teams, and join community chat.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToRoute("rewards") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepCharcoal,
                                        contentColor = MinecraftGold
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("5 CRATES", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { onNavigateToRoute("leaderboard") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepCharcoal,
                                        contentColor = EmeraldGreen
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Leaderboard, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("LEADERBOARD", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { onNavigateToRoute("announcements") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepCharcoal,
                                        contentColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("NEWS", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2. LIVE SERVER STATISTICS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeading(
                        title = "LIVE SERVER STATS",
                        subtitle = "Real-time Discord community & Minecraft SMP telemetry",
                        accentColor = EmeraldGreen
                    )
                    LiveTelemetryBar(
                        uiState = uiState,
                        onRefresh = onRefresh
                    )
                    LiveStatsGrid(
                        uiState = uiState,
                        onStatusClick = { onNavigateToRoute("status") },
                        onPlayersClick = { onNavigateToRoute("players") },
                        onCommunityClick = { onNavigateToRoute("community") }
                    )
                }
            }

            // 3. PLAYERS CURRENTLY SURVIVING PREVIEW
            item {
                ActivePlayersPreviewCard(
                    uiState = uiState,
                    onViewAllPlayers = { onNavigateToRoute("players") },
                    onCopyIp = onCopyIp
                )
            }

            // 4. EXPLORE TGZ LIFESTEAL PORTAL
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeading(
                        title = "EXPLORE THE REALM",
                        subtitle = "Dive into Lifesteal mechanics, ranks, store packs, and community",
                        accentColor = MinecraftGold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PortalShortcutCard(
                            title = "FEATURES",
                            subtitle = "Lifesteal, PvP, Claims",
                            icon = Icons.Default.AutoAwesome,
                            accentColor = EmeraldGreen,
                            onClick = { onNavigateToRoute("features") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shortcut_features")
                        )
                        PortalShortcutCard(
                            title = "RANKS",
                            subtitle = "Vanguard to Immortal",
                            icon = Icons.Default.MilitaryTech,
                            accentColor = MinecraftGold,
                            onClick = { onNavigateToRoute("ranks") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shortcut_ranks")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PortalShortcutCard(
                            title = "STORE PACKS",
                            subtitle = "Hearts, Crates & Bundles",
                            icon = Icons.Default.ShoppingBag,
                            accentColor = NetherRed,
                            onClick = { onNavigateToRoute("store") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shortcut_store")
                        )
                        PortalShortcutCard(
                            title = "COMMUNITY & FAQ",
                            subtitle = "Discord Hub & Guides",
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            accentColor = DiscordBlurple,
                            onClick = { onNavigateToRoute("faq") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shortcut_faq")
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun HeroBannerCard(
    uiState: DashboardUiState,
    ipCopied: Boolean,
    onJoinServer: () -> Unit,
    onJoinDiscord: () -> Unit,
    onCopyIp: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val mc = uiState.minecraftStatus
    val statusBadgeColor = when {
        mc == null -> MinecraftGold
        mc.isOnline -> EmeraldGreen
        else -> NetherRed
    }
    val statusBadgeLabel = when {
        mc == null -> "CHECKING SMP…"
        mc.isOnline -> "SMP ONLINE • ${mc.onlinePlayers}/${mc.maxPlayers}"
        else -> "SMP OFFLINE"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, EmeraldGreen.copy(alpha = 0.35f), shape)
    ) {
        // Animated Minecraft Lifesteal Canvas Background
        HeroLifestealCanvas(
            modifier = Modifier
                .matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Live Status Pill
            Surface(
                color = ObsidianBlack.copy(alpha = 0.78f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PulsingStatusDot(color = statusBadgeColor, dotSize = 7.dp)
                    Text(
                        text = statusBadgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusBadgeColor
                    )
                }
            }

            // Title & Tagline
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VoxelHeartIcon(tint = NetherRed, iconSize = 26.dp)
                    Text(
                        text = stringResource(R.string.hero_title),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.hero_tagline),
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldGreen
                )
            }

            // Hero Description
            Text(
                text = stringResource(R.string.hero_description),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            // Copyable IP Strip
            Surface(
                color = ObsidianBlack.copy(alpha = 0.82f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MINECRAFT JAVA SERVER",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = uiState.serverAddress,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = EmeraldGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onCopyIp,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ipCopied) EmeraldDim else EmeraldGreen.copy(alpha = 0.16f),
                            contentColor = EmeraldGreen
                        ),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("hero_copy_ip_button")
                    ) {
                        Icon(
                            imageVector = if (ipCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy server IP",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (ipCopied) "COPIED!" else "COPY IP",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Primary CTAs: JOIN THE SERVER + JOIN DISCORD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onJoinServer,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = OnEmeraldDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hero_join_server_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_join_server),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onJoinDiscord,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, DiscordBlurple),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DiscordBlurple.copy(alpha = 0.18f),
                        contentColor = Color(0xFFDCE0FF)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hero_join_discord_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_join_discord),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePlayersPreviewCard(
    uiState: DashboardUiState,
    onViewAllPlayers: () -> Unit,
    onCopyIp: () -> Unit
) {
    val mc = uiState.minecraftStatus
    ObsidianCard(
        borderColor = EmeraldGreen.copy(alpha = 0.28f),
        accentGlow = EmeraldGreen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.players_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldGreen
                )
                Text(
                    text = when {
                        mc == null -> "Fetching active SMP players…"
                        !mc.isOnline -> "Server currently offline"
                        mc.onlinePlayers == 0 -> "0 / ${mc.maxPlayers} players connected"
                        else -> "${mc.onlinePlayers} / ${mc.maxPlayers} players online now"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            TextButton(
                onClick = onViewAllPlayers,
                modifier = Modifier.testTag("home_view_all_players_button")
            ) {
                Text(
                    text = "VIEW ALL",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldGreen
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Players tab",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            mc == null -> {
                Text(
                    text = "Connecting to ${uiState.serverAddress}…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
            !mc.isOnline -> {
                Text(
                    text = stringResource(R.string.players_offline_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            mc.onlinePlayers == 0 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "No survivors online yet — copy the IP and drop in first!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onCopyIp,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "COPY IP",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen
                        )
                    }
                }
            }
            mc.players.isEmpty() -> {
                Text(
                    text = "${mc.onlinePlayers} player(s) are currently surviving on the SMP. The server's Java ping protocol hides public usernames — open Players for full details.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mc.players.take(4).forEach { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ObsidianBlack.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MinecraftPlayerAvatar(
                                username = player.username,
                                avatarUrl = player.avatarUrl,
                                size = 34.dp
                            )
                            Text(
                                text = player.username,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            PulsingStatusDot(color = EmeraldGreen, dotSize = 6.dp)
                            Text(
                                text = "SURVIVING",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGreen
                            )
                        }
                    }
                    if (mc.isSampleListOnly && mc.onlinePlayers > mc.players.size) {
                        Text(
                            text = "Showing ${mc.players.size} sample name(s) exposed by Java Server List Ping out of ${mc.onlinePlayers} online.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianCard(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.28f),
        accentGlow = accentColor,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
