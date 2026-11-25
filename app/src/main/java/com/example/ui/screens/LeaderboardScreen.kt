package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.data.LeaderboardPlayerDto
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
fun LeaderboardScreen(
    viewModel: TgzViewModel,
    modifier: Modifier = Modifier
) {
    val players by viewModel.leaderboardPlayers.collectAsStateWithLifecycle()
    val category by viewModel.leaderboardCategory.collectAsStateWithLifecycle()
    val syncNote by viewModel.leaderboardSyncNote.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLeaderboardLoading.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val categories = listOf("playtime", "kills", "hearts", "coins")
    var selectedIndex by remember { mutableStateOf(0) }

    LaunchedEffect(category) {
        val idx = categories.indexOf(category)
        if (idx >= 0) selectedIndex = idx
    }

    LaunchedEffect(Unit) {
        viewModel.loadLeaderboard(category)
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Leaderboard, contentDescription = null, tint = MinecraftGold, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SMP LEADERBOARD",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 18.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = { viewModel.loadLeaderboard(category) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EmeraldGreen)
                    }
                }
            }

            // Real Server Status Card (node2.vyroxsmp.online:10010)
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
                                    text = "SERVER: ${uiState.serverAddress}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (uiState.minecraftStatus?.isOnline == true)
                                        "${uiState.minecraftStatus?.onlinePlayers ?: 0} Players Surviving Live"
                                    else "Server Unreachable",
                                    fontSize = 11.sp,
                                    color = if (uiState.minecraftStatus?.isOnline == true) EmeraldGreen else NetherRed
                                )
                            }
                        }

                        Text(
                            text = uiState.lastUpdatedFormatted ?: "Live",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Category Selector Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = DeepCharcoal,
                    contentColor = EmeraldGreen,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                            color = EmeraldGreen
                        )
                    }
                ) {
                    categories.forEachIndexed { index, cat ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = {
                                selectedIndex = index
                                viewModel.loadLeaderboard(cat)
                            },
                            text = {
                                Text(
                                    text = cat.uppercase(),
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }
            }

            // Sync Note / Empty State
            if (syncNote != null && players.isEmpty()) {
                item {
                    ObsidianCard(borderColor = MinecraftGold.copy(alpha = 0.4f)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MinecraftGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "LIVE PLUGIN INTEGRATION READY",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 13.sp,
                                    color = MinecraftGold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = syncNote!!,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading && players.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldGreen)
                    }
                }
            } else if (players.isEmpty()) {
                item {
                    ObsidianCard {
                        Text(
                            text = "No players recorded on the leaderboard yet. Connect the TGZ Minecraft Server Plugin to stream live kills, deaths, and playtime.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(players, key = { it.uuid ?: it.username }) { player ->
                    LeaderboardPlayerCard(player = player, category = category)
                }
            }
        }
    }
}

@Composable
fun LeaderboardPlayerCard(
    player: LeaderboardPlayerDto,
    category: String
) {
    val rankColor = when (player.rank) {
        1 -> MinecraftGold
        2 -> Color(0xFFE2E8F0)
        3 -> Color(0xFFCD7F32)
        else -> TextSecondary
    }

    ObsidianCard(
        borderColor = if (player.rank <= 3) rankColor.copy(alpha = 0.5f) else ObsidianBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank number
                Text(
                    text = "#${player.rank}",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 16.sp,
                    color = rankColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp)
                )

                // Head Avatar
                SubcomposeAsyncImage(
                    model = player.avatarUrl,
                    contentDescription = player.username,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(modifier = Modifier.background(DeepCharcoal))
                    },
                    error = {
                        Box(modifier = Modifier.background(DeepCharcoal))
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = player.username,
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Playtime: ${player.playtimeFormatted} • K/D: ${player.kdr}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Stat Value based on category
            Column(horizontalAlignment = Alignment.End) {
                val statDisplay = when (category) {
                    "kills" -> "${player.kills} KILLS"
                    "hearts" -> "${player.hearts} ♥"
                    "coins" -> "${player.coins} COINS"
                    else -> player.playtimeFormatted
                }

                Text(
                    text = statDisplay,
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 14.sp,
                    color = when (category) {
                        "kills" -> NetherRed
                        "hearts" -> NetherRed
                        "coins" -> MinecraftGold
                        else -> EmeraldGreen
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
