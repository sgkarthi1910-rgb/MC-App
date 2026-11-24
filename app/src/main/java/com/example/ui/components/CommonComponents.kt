package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.DashboardUiState
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.EmeraldDim
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.NetherRedContainer
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ObsidianCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ObsidianBorder,
    accentGlow: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val bgBrush = if (accentGlow != null) {
        Brush.verticalGradient(
            colors = listOf(
                accentGlow.copy(alpha = 0.08f),
                DeepCharcoal
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(ObsidianElevated.copy(alpha = 0.72f), DeepCharcoal)
        )
    }

    val clickableModifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        Modifier.clip(shape)
    }

    Box(
        modifier = modifier
            .then(clickableModifier)
            .background(bgBrush, shape)
            .border(BorderStroke(1.dp, borderColor), shape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun SectionHeading(
    title: String,
    subtitle: String? = null,
    accentColor: Color = EmeraldGreen,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 18.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun LiveTelemetryBar(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mins = uiState.nextRefreshSeconds / 60
    val secs = uiState.nextRefreshSeconds % 60
    val countdownText = String.format("%d:%02d", mins, secs)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ObsidianCard(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            borderColor = ObsidianBorder
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PulsingStatusDot(
                            color = if (uiState.isRefreshing) MinecraftGold else EmeraldGreen
                        )
                        Text(
                            text = if (uiState.lastUpdatedFormatted != null) {
                                "LAST UPDATED: ${uiState.lastUpdatedFormatted}"
                            } else {
                                "SYNCING LIVE TELEMETRY…"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Auto-refresh every 2m • Next sync in $countdownText",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !uiState.isRefreshing,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.55f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("manual_refresh_button")
                ) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = EmeraldGreen
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh live statistics",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_refresh),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldGreen
                    )
                }
            }
        }

        AnimatedVisibility(visible = uiState.hasAnyConnectionWarning) {
            Surface(
                color = NetherRedContainer.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NetherRed.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_warning_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Connection warning",
                        tint = NetherRed,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "LIVE CONNECTION WARNING",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        val msg = listOfNotNull(uiState.discordError, uiState.minecraftError)
                            .joinToString(" ")
                        Text(
                            text = "$msg Previous valid statistics remain displayed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStatsGrid(
    uiState: DashboardUiState,
    onStatusClick: (() -> Unit)? = null,
    onPlayersClick: (() -> Unit)? = null,
    onCommunityClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val discord = uiState.discordStats
    val mc = uiState.minecraftStatus

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isInitialLoading && discord == null) {
                StatCardSkeleton(modifier = Modifier.weight(1f))
                StatCardSkeleton(modifier = Modifier.weight(1f))
            } else {
                LiveStatCard(
                    title = stringResource(R.string.stat_discord_members),
                    value = discord?.totalMembers?.toString() ?: "—",
                    caption = discord?.guildName ?: "Discord Invite API",
                    icon = Icons.Default.Groups,
                    accentColor = DiscordBlurple,
                    onClick = onCommunityClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_card_discord_total")
                )
                LiveStatCard(
                    title = stringResource(R.string.stat_discord_online),
                    value = discord?.onlineMembers?.toString() ?: "—",
                    caption = "Active on Discord",
                    icon = Icons.Default.Forum,
                    accentColor = EmeraldGreen,
                    showLiveDot = (discord?.onlineMembers ?: 0) > 0,
                    onClick = onCommunityClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_card_discord_online")
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isInitialLoading && mc == null) {
                StatCardSkeleton(modifier = Modifier.weight(1f))
                StatCardSkeleton(modifier = Modifier.weight(1f))
            } else {
                val playerValue = when {
                    mc == null -> "—"
                    !mc.isOnline -> "0"
                    else -> "${mc.onlinePlayers}"
                }
                val capacityCaption = when {
                    mc == null -> "Querying SMP…"
                    mc.maxPlayers > 0 -> "Capacity: ${mc.onlinePlayers} / ${mc.maxPlayers}"
                    mc.isOnline -> "Java SMP Live"
                    else -> "Server Offline"
                }
                LiveStatCard(
                    title = stringResource(R.string.stat_mc_players),
                    value = playerValue,
                    caption = capacityCaption,
                    icon = Icons.Default.SportsEsports,
                    accentColor = MinecraftGold,
                    showLiveDot = mc?.isOnline == true && mc.onlinePlayers > 0,
                    onClick = onPlayersClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_card_mc_players")
                )

                val statusText = when {
                    mc == null -> stringResource(R.string.status_checking)
                    mc.isOnline -> stringResource(R.string.status_online)
                    else -> stringResource(R.string.status_offline)
                }
                val statusColor = when {
                    mc == null -> MinecraftGold
                    mc.isOnline -> EmeraldGreen
                    else -> NetherRed
                }
                val statusCaption = when {
                    mc == null -> "Checking host…"
                    mc.isOnline && mc.latencyMs != null -> "Ping: ${mc.latencyMs} ms"
                    mc.isOnline && !mc.version.isNullOrBlank() -> mc.version
                    mc.isOnline -> "Reachable"
                    else -> "Host unreachable"
                }
                LiveStatCard(
                    title = stringResource(R.string.stat_server_status),
                    value = statusText,
                    caption = statusCaption,
                    icon = Icons.Default.Dns,
                    accentColor = statusColor,
                    showLiveDot = mc?.isOnline == true,
                    onClick = onStatusClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_card_server_status")
                )
            }
        }
    }
}

@Composable
fun LiveStatCard(
    title: String,
    value: String,
    caption: String,
    icon: ImageVector,
    accentColor: Color,
    showLiveDot: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ObsidianCard(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.32f),
        accentGlow = accentColor,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (showLiveDot) {
                PulsingStatusDot(color = accentColor)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = if (value == "ONLINE") EmeraldGreen else if (value == "OFFLINE") NetherRed else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatCardSkeleton(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    ObsidianCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(ObsidianBorder.copy(alpha = alpha), RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(24.dp)
                .background(ObsidianBorder.copy(alpha = alpha), RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(12.dp)
                .background(ObsidianBorder.copy(alpha = alpha * 0.8f), RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(10.dp)
                .background(ObsidianBorder.copy(alpha = alpha * 0.6f), RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun PulsingStatusDot(
    color: Color,
    dotSize: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    Box(
        modifier = Modifier
            .size(dotSize + 4.dp)
            .background(color.copy(alpha = alpha * 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .background(color, CircleShape)
        )
    }
}

@Composable
fun MinecraftPlayerAvatar(
    username: String,
    avatarUrl: String,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(ObsidianElevated)
            .border(1.dp, EmeraldGreen.copy(alpha = 0.4f), shape),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = avatarUrl,
            contentDescription = "$username Minecraft avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
            loading = {
                Text(
                    text = username.take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldGreen
                )
            },
            error = {
                Text(
                    text = username.take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldGreen
                )
            }
        )
    }
}

@Composable
fun DiscordCheckoutBanner(
    onOpenDiscord: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianCard(
        modifier = modifier,
        borderColor = DiscordBlurple.copy(alpha = 0.5f),
        accentGlow = DiscordBlurple,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DiscordBlurple.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "Discord checkout info",
                    tint = DiscordBlurple,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DIRECT DISCORD FULFILLMENT",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.discord_checkout_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onOpenDiscord,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DiscordBlurple.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_banner_open_discord")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = Color(0xFFB3BAFF),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "OPEN TGZ DISCORD SERVER",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFDCE0FF)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinServerModalSheet(
    serverAddress: String,
    isOnline: Boolean?,
    copied: Boolean,
    onCopyIp: () -> Unit,
    onOpenDiscord: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepCharcoal,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VoxelHeartIcon(tint = NetherRed, iconSize = 24.dp)
                Column {
                    Text(
                        text = "JOIN TGZ LIFESTEAL",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = when (isOnline) {
                            true -> "Java SMP is ONLINE and accepting players"
                            false -> "Server currently appears OFFLINE — check Discord for updates"
                            null -> "Checking live server availability…"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (isOnline) {
                            true -> EmeraldGreen
                            false -> NetherRed
                            null -> MinecraftGold
                        }
                    )
                }
            }

            // Copyable Server IP Box
            Surface(
                color = ObsidianBlack,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "JAVA SERVER ADDRESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = serverAddress,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = EmeraldGreen
                        )
                    }
                    Button(
                        onClick = onCopyIp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copied) EmeraldDim else EmeraldGreen,
                            contentColor = if (copied) EmeraldGreen else OnEmeraldDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("join_sheet_copy_ip_button")
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy server IP",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (copied) "COPIED" else "COPY",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Step-by-step instructions
            val steps = listOf(
                "1. Launch Minecraft: Java Edition on PC or compatible launcher.",
                "2. Open Multiplayer -> click 'Add Server'.",
                "3. Enter 'TGZ Lifesteal' as the Server Name and paste $serverAddress as the Server Address.",
                "4. Click Done, select TGZ Lifesteal, and enter the arena."
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEach { step ->
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            OutlinedButton(
                onClick = onOpenDiscord,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DiscordBlurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("join_sheet_discord_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = null,
                    tint = Color(0xFFB3BAFF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NEED HELP? JOIN TGZ DISCORD",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
        }
    }
}
