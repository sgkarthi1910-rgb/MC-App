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
import androidx.compose.ui.unit.sp
import com.example.data.DashboardUiState
import com.example.data.MinecraftServerStatusDto
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
    javaStatus: MinecraftServerStatusDto? = null,
    bedrockStatus: MinecraftServerStatusDto? = null,
    onCopyBedrockIp: () -> Unit = {},
    onRefreshJava: () -> Unit = {},
    onRefreshBedrock: () -> Unit = {},
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
                    title = "LIVE SERVER STATUS",
                    subtitle = "Real-time Java & Bedrock telemetry synchronized every 2 minutes",
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

            // 1. JAVA EDITION SERVER CARD
            item {
                val isJavaOnline = javaStatus?.online ?: (mc?.isOnline ?: false)
                val javaOnlinePlayers = javaStatus?.players?.online ?: (mc?.onlinePlayers ?: 0)
                val javaMaxPlayers = javaStatus?.players?.max ?: (mc?.maxPlayers ?: 200)

                ObsidianCard(
                    borderColor = EmeraldGreen.copy(alpha = 0.4f),
                    accentGlow = EmeraldGreen
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "☕", fontSize = 18.sp)
                            Text(
                                text = "JAVA EDITION (PC / MAC / LINUX)",
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onRefreshJava,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen.copy(alpha = 0.15f),
                                contentColor = EmeraldGreen
                            )
                        ) {
                            Text(text = "REFRESH", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ServerInfoRow(
                        label = "JAVA SERVER ADDRESS",
                        value = "node2.vyroxsmp.online:10010",
                        valueColor = EmeraldGreen,
                        isMono = true
                    )
                    ServerInfoDivider()

                    ServerInfoRow(
                        label = "ONLINE STATUS",
                        value = if (isJavaOnline) "ONLINE" else "OFFLINE",
                        valueColor = if (isJavaOnline) EmeraldGreen else NetherRed,
                        isMono = true
                    )
                    ServerInfoDivider()

                    ServerInfoRow(
                        label = "ONLINE PLAYERS",
                        value = "$javaOnlinePlayers / $javaMaxPlayers",
                        isMono = true
                    )
                    ServerInfoDivider()

                    ServerInfoRow(
                        label = "VERSION COMPATIBILITY",
                        value = javaStatus?.version ?: (mc?.version ?: "1.20.4+"),
                        valueColor = MinecraftGold,
                        isMono = true
                    )

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
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = if (ipCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (ipCopied) "COPIED JAVA ADDRESS" else "COPY JAVA IP (10010)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. BEDROCK EDITION SERVER CARD
            item {
                val isBedrockOnline = bedrockStatus?.online ?: false
                val bedrockOnlinePlayers = bedrockStatus?.players?.online ?: 0
                val bedrockMaxPlayers = bedrockStatus?.players?.max ?: 200

                ObsidianCard(
                    borderColor = Color(0xFF38BDF8).copy(alpha = 0.4f),
                    accentGlow = Color(0xFF38BDF8)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "📱", fontSize = 18.sp)
                            Text(
                                text = "BEDROCK EDITION (PE / CONSOLE / WIN 10)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onRefreshBedrock,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                contentColor = Color(0xFF38BDF8)
                            )
                        ) {
                            Text(text = "REFRESH", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ServerInfoRow(
                        label = "BEDROCK SERVER ADDRESS",
                        value = "node2.vyroxsmp.online:10011",
                        valueColor = Color(0xFF38BDF8),
                        isMono = true
                    )
                    ServerInfoDivider()

                    ServerInfoRow(
                        label = "BEDROCK PORT",
                        value = "10011",
                        valueColor = MinecraftGold,
                        isMono = true
                    )
                    ServerInfoDivider()

                    ServerInfoRow(
                        label = "ONLINE STATUS",
                        value = if (isBedrockOnline) "ONLINE" else "STANDBY / OFFLINE",
                        valueColor = if (isBedrockOnline) EmeraldGreen else NetherRed,
                        isMono = true
                    )
                    ServerInfoDivider()

                    ServerInfoRow(
                        label = "ONLINE PLAYERS",
                        value = "$bedrockOnlinePlayers / $bedrockMaxPlayers",
                        isMono = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onCopyBedrockIp,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8).copy(alpha = 0.2f),
                            contentColor = Color(0xFF38BDF8)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COPY BEDROCK ADDRESS (10011)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. HOW TO JOIN GUIDE (JAVA & BEDROCK)
            item {
                ObsidianCard(borderColor = ObsidianBorder) {
                    Text(
                        text = "HOW TO CONNECT TO TGZ LIFESTYLE",
                        style = MaterialTheme.typography.titleSmall,
                        color = MinecraftGold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val steps = listOf(
                        "Java 1.20+" to "Multiplayer -> Add Server -> Enter 'node2.vyroxsmp.online:10010' -> Join.",
                        "Bedrock / PE" to "Servers -> Add Server -> Address: 'node2.vyroxsmp.online', Port: '10011' -> Save & Play.",
                        "Consoles" to "Use BedrockConnect or custom DNS with Bedrock Port 10011 to join from Xbox / PlayStation / Switch."
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

            // 4. LIVE DATA SOURCES TRANSPARENCY
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
                        label = "JAVA SOURCE",
                        value = "Java TCP SLP (Port 10010)"
                    )
                    ServerInfoDivider()
                    ServerInfoRow(
                        label = "BEDROCK SOURCE",
                        value = "RakNet UDP Ping (Port 10011)"
                    )
                    ServerInfoDivider()
                    ServerInfoRow(
                        label = "AUTO-REFRESH",
                        value = "Every 120 seconds",
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
