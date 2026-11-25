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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.DashboardUiState
import com.example.ui.components.LiveStatCard
import com.example.ui.components.LiveTelemetryBar
import com.example.ui.components.ObsidianCard
import com.example.ui.components.SectionHeading
import com.example.ui.components.StatCardSkeleton
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CommunityScreen(
    uiState: DashboardUiState,
    onJoinDiscord: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    title = "TGZ COMMUNITY",
                    subtitle = "Connect with survivors, claim giveaways, form clans, and open store tickets",
                    accentColor = DiscordBlurple
                )
            }

            item {
                LiveTelemetryBar(
                    uiState = uiState,
                    onRefresh = onRefresh
                )
            }

            // DISCORD BRANDING HERO CARD
            item {
                ObsidianCard(
                    borderColor = DiscordBlurple.copy(alpha = 0.55f),
                    accentGlow = DiscordBlurple,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val iconShape = RoundedCornerShape(14.dp)
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(iconShape)
                                .background(DiscordBlurple.copy(alpha = 0.2f))
                                .border(1.5.dp, DiscordBlurple, iconShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!discord?.guildIconUrl.isNullOrBlank()) {
                                SubcomposeAsyncImage(
                                    model = discord.guildIconUrl,
                                    contentDescription = "TGZ Discord Server Icon",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize(),
                                    error = {
                                        Icon(
                                            imageVector = Icons.Default.Forum,
                                            contentDescription = null,
                                            tint = DiscordBlurple,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = "TGZ Discord",
                                    tint = DiscordBlurple,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = discord?.guildName ?: "TGZ LIFESTYLE DISCORD",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = uiState.discordInviteUrl.removePrefix("https://"),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = Color(0xFFB3BAFF)
                            )
                            if (!discord?.guildDescription.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = discord.guildDescription.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onJoinDiscord,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DiscordBlurple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("community_join_discord_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JOIN OFFICIAL TGZ DISCORD",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // LIVE DISCORD STATS CARDS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.isInitialLoading && discord == null) {
                        StatCardSkeleton(modifier = Modifier.weight(1f))
                        StatCardSkeleton(modifier = Modifier.weight(1f))
                    } else {
                        LiveStatCard(
                            title = "TOTAL DISCORD MEMBERS",
                            value = discord?.totalMembers?.toString() ?: "—",
                            caption = discord?.dataSource ?: "Discord Invite API",
                            icon = Icons.Default.Groups,
                            accentColor = DiscordBlurple,
                            modifier = Modifier.weight(1f)
                        )
                        LiveStatCard(
                            title = "DISCORD MEMBERS ONLINE",
                            value = discord?.onlineMembers?.toString() ?: "—",
                            caption = "Currently online",
                            icon = Icons.Default.Forum,
                            accentColor = EmeraldGreen,
                            showLiveDot = (discord?.onlineMembers ?: 0) > 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // COMMUNITY PILLARS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeading(
                        title = "INSIDE THE SERVER",
                        subtitle = "What awaits you inside the TGZ Discord hub",
                        accentColor = EmeraldGreen
                    )
                    CommunityPillarCard(
                        icon = Icons.Default.EmojiEvents,
                        accent = MinecraftGold,
                        title = "Events & Tournaments",
                        description = "Register your squad for scheduled lifestyle events, community builds, and seasonal celebrations."
                    )
                    CommunityPillarCard(
                        icon = Icons.Default.SupportAgent,
                        accent = EmeraldGreen,
                        title = "Store & Support Tickets",
                        description = "Open private tickets with TGZ staff to order Ranks, Crate Keys, or report rule-breakers."
                    )
                    CommunityPillarCard(
                        icon = Icons.Default.Gavel,
                        accent = NetherRed,
                        title = "Fair Play & Server Rules",
                        description = "Zero hacked clients, no duping, and respectful player friendships keep TGZ Lifestyle welcoming for everyone."
                    )
                }
            }

            item {
                Text(
                    text = "Discord member statistics are fetched live from Discord's official API and kept strictly separate from Minecraft SMP online player counts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun CommunityPillarCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    description: String
) {
    ObsidianCard(borderColor = ObsidianBorder) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
