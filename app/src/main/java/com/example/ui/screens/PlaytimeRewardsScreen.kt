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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.data.AdminClaimItemDto
import com.example.data.PlaytimeRewardDto
import com.example.ui.components.ObsidianCard
import com.example.ui.theme.DeepCharcoal
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaytimeRewardsScreen(
    viewModel: TgzViewModel,
    modifier: Modifier = Modifier
) {
    val rewards by viewModel.rewards.collectAsStateWithLifecycle()
    val userPlaytimeMinutes by viewModel.userPlaytimeMinutes.collectAsStateWithLifecycle()
    val claimHistory by viewModel.claimHistory.collectAsStateWithLifecycle()
    val adminClaims by viewModel.adminClaims.collectAsStateWithLifecycle()
    val adminConfigs by viewModel.adminRewardConfigs.collectAsStateWithLifecycle()
    val actionMessage by viewModel.rewardsActionMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isRewardsLoading.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    var editConfigRewardId by remember { mutableStateOf<String?>(null) }
    var editConfigMinutes by remember { mutableStateOf("") }

    val isStaff = authState.role == "ADMIN" || authState.role == "MODERATOR"
    val isAdmin = authState.role == "ADMIN"

    LaunchedEffect(Unit) {
        viewModel.loadRewards()
        viewModel.loadRewardHistory()
        if (isStaff) {
            viewModel.loadAdminClaims()
        }
        if (isAdmin) {
            viewModel.loadAdminRewardConfigs()
        }
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
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MinecraftGold, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PLAYTIME CRATES",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 18.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = { viewModel.loadRewards() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EmeraldGreen)
                    }
                }
            }

            // Playtime Overview Card
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
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MinecraftGold, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "YOUR CURRENT PLAYTIME",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${userPlaytimeMinutes / 60}h ${userPlaytimeMinutes % 60}m ($userPlaytimeMinutes mins)",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 18.sp,
                                    color = MinecraftGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Authoritative Sync",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = EmeraldGreen
                        )
                    }
                }
            }

            // Action Feedback Message
            if (actionMessage != null) {
                item {
                    ObsidianCard(borderColor = EmeraldGreen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = actionMessage!!, color = EmeraldGreen, fontSize = 13.sp)
                            TextButton(onClick = { viewModel.clearRewardsMessage() }) {
                                Text("DISMISS", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Tabs for Members vs Staff
            if (isStaff) {
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DeepCharcoal,
                        contentColor = EmeraldGreen,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = EmeraldGreen
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("5 CRATES", fontFamily = SilkscreenFontFamily, fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("HISTORY", fontFamily = SilkscreenFontFamily, fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("AUDIT CLAIMS", fontFamily = SilkscreenFontFamily, fontSize = 11.sp) }
                        )
                        if (isAdmin) {
                            Tab(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                text = { Text("SETTINGS", fontFamily = SilkscreenFontFamily, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // TAB 0: 5 CRATES ONLY
            if (selectedTab == 0) {
                if (isLoading && rewards.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MinecraftGold)
                        }
                    }
                } else {
                    items(rewards, key = { it.id }) { crate ->
                        CrateRewardCard(
                            crate = crate,
                            onClaim = { viewModel.claimReward(crate.id) }
                        )
                    }
                }
            } else if (selectedTab == 1) {
                // TAB 1: CLAIM HISTORY
                if (claimHistory.isEmpty()) {
                    item {
                        ObsidianCard {
                            Text(
                                text = "No rewards claimed yet. Play on node2.vyroxsmp.online:10010 to unlock crate key vouchers.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(claimHistory, key = { it.id }) { item ->
                        val dateFormatted = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(item.claimedAt))
                        ObsidianCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = item.rewardName, fontFamily = SilkscreenFontFamily, fontSize = 14.sp, color = TextPrimary)
                                    Text(text = "Claimed at $dateFormatted", fontSize = 11.sp, color = TextMuted)
                                }
                                Text(text = item.status, fontFamily = SilkscreenFontFamily, fontSize = 12.sp, color = EmeraldGreen)
                            }
                        }
                    }
                }
            } else if (selectedTab == 2) {
                // TAB 2: AUDIT CLAIMS (STAFF)
                if (adminClaims.isEmpty()) {
                    item {
                        ObsidianCard {
                            Text(text = "No claim submissions for review.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(adminClaims, key = { it.id }) { claim ->
                        ObsidianCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "${claim.displayName} (@${claim.userId})", fontFamily = SilkscreenFontFamily, fontSize = 13.sp, color = TextPrimary)
                                    Text(text = "Crate: ${claim.rewardName} • Playtime: ${claim.playtimeAtClaim}m", fontSize = 11.sp, color = TextSecondary)
                                    Text(text = "Status: ${claim.status}", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = EmeraldGreen)
                                }

                                if (claim.status == "APPROVED") {
                                    TextButton(onClick = { viewModel.updateClaimStatus(claim.id, "REVOKED") }) {
                                        Text("REVOKE", color = NetherRed, fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                                    }
                                } else {
                                    TextButton(onClick = { viewModel.updateClaimStatus(claim.id, "APPROVED") }) {
                                        Text("APPROVE", color = EmeraldGreen, fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 3 && isAdmin) {
                // TAB 3: ADMIN CONFIGURATION
                item {
                    Text(
                        text = "ADMIN: CONFIGURE PLAYTIME REQUIREMENTS",
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 13.sp,
                        color = MinecraftGold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(adminConfigs, key = { it.rewardId }) { config ->
                    ObsidianCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = config.name, fontFamily = SilkscreenFontFamily, fontSize = 14.sp, color = TextPrimary)
                                Text(
                                    text = config.requiredPlaytimeMinutes?.let { "$it minutes (${it / 60} hrs)" } ?: "Unconfigured (Pending official requirement)",
                                    fontSize = 12.sp,
                                    color = if (config.requiredPlaytimeMinutes != null) MinecraftGold else NetherRed
                                )
                            }

                            Button(
                                onClick = {
                                    editConfigRewardId = config.rewardId
                                    editConfigMinutes = config.requiredPlaytimeMinutes?.toString() ?: ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("SET", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (editConfigRewardId != null) {
        AlertDialog(
            onDismissRequest = { editConfigRewardId = null },
            title = { Text("SET PLAYTIME REQUIREMENT", fontFamily = SilkscreenFontFamily, color = EmeraldGreen, fontSize = 15.sp) },
            text = {
                Column {
                    Text(text = "Enter required playtime in minutes for $editConfigRewardId. Leave empty for unconfigured.", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editConfigMinutes,
                        onValueChange = { editConfigMinutes = it },
                        label = { Text("Required Minutes") },
                        placeholder = { Text("e.g. 120") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = ObsidianBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val min = editConfigMinutes.trim().toIntOrNull()
                        viewModel.updateRewardConfig(editConfigRewardId!!, min)
                        editConfigRewardId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark)
                ) {
                    Text("SAVE", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { editConfigRewardId = null }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = DeepCharcoal
        )
    }
}

@Composable
fun CrateRewardCard(
    crate: PlaytimeRewardDto,
    onClaim: () -> Unit
) {
    val tierColor = when (crate.id) {
        "common_crate" -> EmeraldGreen
        "monthly_crate" -> MinecraftGold
        "special_crate" -> Color(0xFF38BDF8)
        "arcane_crate" -> Color(0xFFA855F7)
        "matrix_crate" -> NetherRed
        else -> EmeraldGreen
    }

    ObsidianCard(
        borderColor = tierColor.copy(alpha = 0.5f),
        accentGlow = tierColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = tierColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = crate.name,
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        crate.badge?.let { badge ->
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = tierColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, tierColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = badge,
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 9.sp,
                                    color = tierColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Requirement: ${crate.requiredPlaytimeFormatted}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = tierColor
                    )
                }
            }

            // Claim / Status Button
            when (crate.status) {
                "CLAIMED" -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DeepCharcoal,
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CLAIMED", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, color = EmeraldGreen)
                        }
                    }
                }
                "AVAILABLE" -> {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("CLAIM CRATE", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                "PENDING_OFFICIAL_REQUIREMENT" -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DeepCharcoal,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Text(
                            text = "AWAITING CONFIG",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 10.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
                else -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DeepCharcoal,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LOCKED (${crate.progressPercent}%)", fontFamily = SilkscreenFontFamily, fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = crate.description,
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Progress: ${crate.progressPercent}%",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = TextMuted
            )
            Text(
                text = "${crate.currentPlaytimeMinutes} mins logged",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { (crate.progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (crate.status == "CLAIMED") EmeraldGreen else tierColor,
            trackColor = DeepCharcoal
        )
    }
}
