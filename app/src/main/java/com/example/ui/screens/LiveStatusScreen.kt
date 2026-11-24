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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DashboardUiState
import com.example.ui.components.LiveStatsGrid
import com.example.ui.components.LiveTelemetryBar
import com.example.ui.components.ObsidianCard
import com.example.ui.components.SectionHeading
import com.example.ui.theme.EmeraldDim
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LiveStatusScreen(
    uiState: DashboardUiState,
    ipCopied: Boolean,
    onCopyIp: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToPlayers: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mc = uiState.minecraftStatus
    val discord = uiState.discordStats

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeading(
                    title = "LIVE STATUS",
                    subtitle = "Real-time server telemetry synchronized every 2 minutes",
                    accentColor = EmeraldGreen
                )
            }

            item {
                LiveTelemetryBar(
                    uiState = uiState,
                    onRefresh = onRefresh
                )
            }

            item {
                LiveStatsGrid(
                    uiState = uiState,
                    onPlayersClick = onNavigateToPlayers,
                    onCommunityClick = onNavigateToCommunity
                )
            }

            // SERVER INFORMATION CARD
            item {
                ObsidianCard(
                    borderColor = EmeraldGreen.copy(alpha = 0.35f),
                    accentGlow = EmeraldGreen
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Server Information",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "SERVER INFORMATION",
                            style = MaterialTheme.typography.titleSmall,
                            color = EmeraldGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ServerInfoRow(
                        label = "JAVA SERVER IP",
                        value = uiState.serverAddress,
                        valueColor = EmeraldGreen,
                        isMono = true
                    )
                    ServerInfoDivider()

                    ServerInfoRow(
                        label = "SERVER STATUS",
                        value = when {
                            mc == null -> "Checking…"
                            mc.isOnline -> "ONLINE"
                            else -> "OFFLINE"
                        },
                        valueColor = when {
                            mc == null -> MinecraftGold
                            mc.isOnline -> EmeraldGreen
                            else -> NetherRed
                        },
                        isMono = true
                    )
                    ServerInfoDivider()

                    ServerInfoRow(
                        label = "CURRENT PLAYERS",
                        value = when {
                            mc == null -> "—"
                            mc.isOnline -> "${mc.onlinePlayers}"
                            else -> "0"
                        },
                        isMono = true
                    )
                    ServerInfoDivider()

                    ServerInfoRow(
                        label = "MAXIMUM PLAYERS",
                        value = when {
                            mc == null || mc.maxPlayers <= 0 -> "Unavailable"
                            else -> "${mc.maxPlayers}"
                        },
                        isMono = true
                    )

                    // Only show Server Version if actually available from real server query
                    if (!mc?.version.isNullOrBlank()) {
                        ServerInfoDivider()
                        ServerInfoRow(
                            label = "SERVER VERSION",
                            value = mc?.version.orEmpty(),
                            valueColor = MinecraftGold,
                            isMono = true
                        )
                    }

                    if (mc?.latencyMs != null) {
                        ServerInfoDivider()
                        ServerInfoRow(
                            label = "PING LATENCY",
                            value = "${mc.latencyMs} ms",
                            valueColor = EmeraldGreen,
                            isMono = true
                        )
                    }

                    ServerInfoDivider()
                    ServerInfoRow(
                        label = "SUPPORTED PLATFORM",
                        value = "Minecraft: Java Edition"
                    )

                    if (!mc?.motd.isNullOrBlank()) {
                        ServerInfoDivider()
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "SERVER MOTD",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mc?.motd.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onCopyIp,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ipCopied) EmeraldDim else EmeraldGreen,
                            contentColor = if (ipCopied) EmeraldGreen else OnEmeraldDark
                        ),
                        border = if (ipCopied) BorderStroke(1.dp, EmeraldGreen) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("status_copy_ip_button")
                    ) {
                        Icon(
                            imageVector = if (ipCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (ipCopied) "COPIED ${uiState.serverAddress}" else "COPY JAVA SERVER IP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // HOW TO JOIN GUIDE
            item {
                ObsidianCard(borderColor = ObsidianBorder) {
                    Text(
                        text = "HOW TO JOIN TGZ LIFESTEAL",
                        style = MaterialTheme.typography.titleSmall,
                        color = MinecraftGold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val steps = listOf(
                        "Step 1" to "Open Minecraft: Java Edition on your computer.",
                        "Step 2" to "Select Multiplayer from the main menu and click Add Server.",
                        "Step 3" to "Type TGZ Lifesteal as the Server Name and paste ${uiState.serverAddress} into Server Address.",
                        "Step 4" to "Click Done, double-click TGZ Lifesteal, and start surviving."
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        steps.forEach { (step, desc) ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MinecraftGold.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MinecraftGold
                                    )
                                }
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // LIVE DATA SOURCES TRANSPARENCY
            item {
                ObsidianCard(borderColor = ObsidianBorder) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Data Sources",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "LIVE DATA SOURCES",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    ServerInfoRow(
                        label = "DISCORD SOURCE",
                        value = discord?.dataSource ?: "Discord Live Invite API"
                    )
                    ServerInfoDivider()
                    ServerInfoRow(
                        label = "MINECRAFT SOURCE",
                        value = mc?.querySource ?: "Java TCP SLP + mcsrvstat.us"
                    )
                    ServerInfoDivider()
                    ServerInfoRow(
                        label = "AUTO-REFRESH",
                        value = "Every 120 seconds (exact)",
                        isMono = true
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ServerInfoRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    isMono: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = if (isMono) {
                MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = valueColor
        )
    }
}

@Composable
private fun ServerInfoDivider() {
    HorizontalDivider(color = ObsidianBorderSubtle, thickness = 1.dp)
}
