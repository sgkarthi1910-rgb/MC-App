package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.DashboardUiState
import com.example.data.DiscordActivityMember
import com.example.data.MinecraftPlayer
import com.example.ui.components.LiveTelemetryBar
import com.example.ui.components.MinecraftPlayerAvatar
import com.example.ui.components.ObsidianCard
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.SectionHeading
import com.example.ui.components.StatCardSkeleton
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlayersScreen(
    uiState: DashboardUiState,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCopyIp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mc = uiState.minecraftStatus
    val discordWidgetPlayers = uiState.discordStats?.widgetPlayingMinecraft.orEmpty()

    val filteredPlayers = remember(mc?.players, searchQuery) {
        val list = mc?.players.orEmpty()
        if (searchQuery.isBlank()) list
        else list.filter { it.username.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeading(
                    title = stringResource(R.string.players_section_title),
                    subtitle = "Live Minecraft SMP roster on ${uiState.serverAddress}",
                    accentColor = EmeraldGreen
                )
            }

            item {
                LiveTelemetryBar(
                    uiState = uiState,
                    onRefresh = onRefresh
                )
            }

            // CAPACITY OVERVIEW CARD
            item {
                if (uiState.isInitialLoading && mc == null) {
                    StatCardSkeleton(modifier = Modifier.fillMaxWidth())
                } else {
                    val isOnline = mc?.isOnline == true
                    val onlineCount = if (isOnline) (mc?.onlinePlayers ?: 0) else 0
                    val maxPlayers = mc?.maxPlayers ?: 0
                    val progress = if (maxPlayers > 0 && isOnline) {
                        (onlineCount.toFloat() / maxPlayers.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    ObsidianCard(
                        borderColor = if (isOnline) EmeraldGreen.copy(alpha = 0.4f) else NetherRed.copy(alpha = 0.4f),
                        accentGlow = if (isOnline) EmeraldGreen else NetherRed
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "MINECRAFT SMP PLAYERS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (mc == null) "—" else "$onlineCount",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = if (isOnline) EmeraldGreen else NetherRed
                                    )
                                    if (maxPlayers > 0) {
                                        Text(
                                            text = "/ $maxPlayers capacity",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = TextSecondary,
                                            modifier = Modifier.padding(bottom = 3.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = if (isOnline) EmeraldGreen.copy(alpha = 0.14f) else NetherRed.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(
                                    1.dp,
                                    if (isOnline) EmeraldGreen.copy(alpha = 0.5f) else NetherRed.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PulsingStatusDot(
                                        color = if (isOnline) EmeraldGreen else NetherRed,
                                        dotSize = 7.dp
                                    )
                                    Text(
                                        text = when {
                                            mc == null -> "CHECKING"
                                            isOnline -> "ONLINE"
                                            else -> "OFFLINE"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOnline) EmeraldGreen else NetherRed
                                    )
                                }
                            }
                        }

                        if (maxPlayers > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = EmeraldGreen,
                                trackColor = ObsidianBlack
                            )
                        }
                    }
                }
            }

            // PROTOCOL TRANSPARENCY BANNER (Sample list vs Full list)
            if (mc != null && mc.isOnline && mc.onlinePlayers > 0 && mc.isSampleListOnly) {
                item {
                    Surface(
                        color = ObsidianElevated.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MinecraftGold.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Protocol note",
                                tint = MinecraftGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (mc.players.isEmpty()) {
                                    "The server reports ${mc.onlinePlayers} online player(s), but its Java Server List Ping hides public usernames. Connect an optional SMP plugin endpoint in configuration to expose the complete roster."
                                } else {
                                    "Displaying ${mc.players.size} sample username(s) returned by Java Server List Ping (${mc.onlinePlayers} total online). Configure MINECRAFT_PLUGIN_PLAYERS_URL for the full player list."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // SEARCH FILTER WHEN PLAYERS ARE LISTED
            if (mc != null && mc.isOnline && mc.players.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text(
                                text = "Filter surviving players by username…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search players",
                                tint = EmeraldGreen
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedContainerColor = DeepCharcoal,
                            unfocusedContainerColor = DeepCharcoal,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_search_input")
                    )
                }
            }

            // PLAYER LIST OR EMPTY / OFFLINE STATES
            when {
                mc == null -> {
                    item {
                        ObsidianCard {
                            Text(
                                text = "Querying Minecraft server status for ${uiState.serverAddress}…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                !mc.isOnline -> {
                    item {
                        EmptyPlayersStateCard(
                            icon = Icons.Default.CloudOff,
                            accentColor = NetherRed,
                            title = stringResource(R.string.players_offline_title),
                            subtitle = stringResource(R.string.players_offline_subtitle),
                            actionLabel = "COPY SERVER IP",
                            onAction = onCopyIp
                        )
                    }
                }
                mc.onlinePlayers == 0 -> {
                    item {
                        EmptyPlayersStateCard(
                            icon = Icons.Default.SportsEsports,
                            accentColor = EmeraldGreen,
                            title = stringResource(R.string.players_empty_title),
                            subtitle = stringResource(R.string.players_empty_subtitle),
                            actionLabel = "COPY IP & JOIN FIRST",
                            onAction = onCopyIp
                        )
                    }
                }
                mc.players.isNotEmpty() && filteredPlayers.isEmpty() -> {
                    item {
                        ObsidianCard {
                            Text(
                                text = "No online players match \"$searchQuery\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                filteredPlayers.isNotEmpty() -> {
                    items(filteredPlayers, key = { it.username }) { player ->
                        PlayerRosterCard(player = player)
                    }
                }
            }

            // SEPARATE OPTIONAL DISCORD ACTIVITY SECTION (NEVER MIXED WITH SMP PLAYER COUNT)
            if (discordWidgetPlayers.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SectionHeading(
                        title = "DISCORD ACTIVITY",
                        subtitle = "Discord members showing Minecraft status (distinct from SMP server roster)",
                        accentColor = DiscordBlurple
                    )
                }
                items(discordWidgetPlayers, key = { "discord_${it.id}" }) { member ->
                    DiscordActivityCard(member = member)
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun PlayerRosterCard(player: MinecraftPlayer) {
    ObsidianCard(
        borderColor = EmeraldGreen.copy(alpha = 0.28f),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = Modifier.testTag("player_card_${player.username}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MinecraftPlayerAvatar(
                username = player.username,
                avatarUrl = player.avatarUrl,
                size = 44.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Minecraft Java SMP",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Surface(
                color = EmeraldGreen.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PulsingStatusDot(color = EmeraldGreen, dotSize = 6.dp)
                    Text(
                        text = "ONLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscordActivityCard(member: DiscordActivityMember) {
    ObsidianCard(
        borderColor = DiscordBlurple.copy(alpha = 0.35f),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DiscordBlurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!member.avatarUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = member.avatarUrl,
                        contentDescription = "${member.username} Discord avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = DiscordBlurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = member.gameName?.let { "Discord Status: Playing $it" } ?: "Discord Member",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB3BAFF)
                )
            }
        }
    }
}

@Composable
private fun EmptyPlayersStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    ObsidianCard(
        borderColor = accentColor.copy(alpha = 0.35f),
        accentGlow = accentColor,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreen,
                    contentColor = OnEmeraldDark
                ),
                modifier = Modifier
                    .height(46.dp)
                    .testTag("players_empty_copy_ip_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
