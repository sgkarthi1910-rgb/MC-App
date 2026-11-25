package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TeamDetailsDto
import com.example.data.TeamMemberDto
import com.example.data.TeamSummaryDto
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamsScreen(
    viewModel: TgzViewModel,
    modifier: Modifier = Modifier
) {
    val myTeam by viewModel.myTeam.collectAsStateWithLifecycle()
    val inTeam by viewModel.inTeam.collectAsStateWithLifecycle()
    val invitations by viewModel.teamInvitations.collectAsStateWithLifecycle()
    val publicTeams by viewModel.publicTeams.collectAsStateWithLifecycle()
    val allTeams by viewModel.allTeams.collectAsStateWithLifecycle()
    val selectedTeamDetails by viewModel.selectedTeamDetails.collectAsStateWithLifecycle()
    val actionMessage by viewModel.teamActionMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isTeamLoading.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var showEditTeamDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var confirmDisbandDialog by remember { mutableStateOf(false) }
    var confirmLeaveDialog by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<TeamMemberDto?>(null) }
    var memberToTransfer by remember { mutableStateOf<TeamMemberDto?>(null) }

    val isStaff = authState.role == "ADMIN" || authState.role == "MODERATOR"
    val isAdmin = authState.role == "ADMIN"
    val currentUserId = authState.user?.id ?: ""

    val filteredClans = remember(publicTeams, searchQuery) {
        if (searchQuery.isBlank()) publicTeams
        else publicTeams.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.ownerName.contains(searchQuery, ignoreCase = true) ||
                    (it.description ?: "").contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMyTeam()
        viewModel.loadTeamInvitations()
        viewModel.loadPublicTeams()
        if (isStaff) {
            viewModel.loadAllTeams()
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
                .widthIn(max = 800.dp),
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
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CLAN MANAGEMENT",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 18.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Form alliances, dominate the realm and claim team glory",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = {
                        viewModel.loadMyTeam()
                        viewModel.loadTeamInvitations()
                        viewModel.loadPublicTeams()
                        if (isStaff) viewModel.loadAllTeams()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EmeraldGreen)
                    }
                }
            }

            // Message feedback banner
            if (actionMessage != null) {
                item {
                    ObsidianCard(borderColor = EmeraldGreen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = actionMessage!!, color = EmeraldGreen, fontSize = 13.sp)
                            }
                            TextButton(onClick = { viewModel.clearTeamMessage() }) {
                                Text("DISMISS", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Tabs Row
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
                        text = {
                            Text(
                                text = if (inTeam) "MY CLAN" else "CLAN HQ",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 11.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "BROWSE (${publicTeams.size})",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 11.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("INVITES", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                                if (invitations.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = MinecraftGold,
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${invitations.size}",
                                                color = ObsidianBlack,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                    if (isStaff) {
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = {
                                Text(
                                    text = "STAFF (${allTeams.size})",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 11.sp,
                                    color = NetherRed
                                )
                            }
                        )
                    }
                }
            }

            // ----------------- TAB 0: MY CLAN -----------------
            if (selectedTab == 0) {
                if (inTeam && myTeam != null) {
                    val team = myTeam!!
                    // 1. Clan Overview Hero Card
                    item {
                        ObsidianCard(
                            borderColor = EmeraldGreen.copy(alpha = 0.6f),
                            accentGlow = EmeraldGreen
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = EmeraldGreen.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                TeamLogoIcon(logo = team.logo, modifier = Modifier.size(28.dp), tint = EmeraldGreen)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = team.name,
                                                    fontFamily = SilkscreenFontFamily,
                                                    fontSize = 18.sp,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                StatusBadge(status = team.status)
                                            }
                                            Text(
                                                text = "Leader: ${team.ownerName} (@${team.ownerId})",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    if (team.isCurrentUserOwner) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MinecraftGold.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MinecraftGold)
                                        ) {
                                            Text(
                                                text = "YOU ARE LEADER",
                                                fontFamily = SilkscreenFontFamily,
                                                fontSize = 9.sp,
                                                color = MinecraftGold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                if (!team.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = team.description,
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Activity Stats summary
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp),
                                        color = DeepCharcoal,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = NetherRed, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("CLAN KILLS", fontFamily = SilkscreenFontFamily, fontSize = 9.sp, color = TextMuted)
                                                Text("${team.totalKills}", fontFamily = SilkscreenFontFamily, fontSize = 14.sp, color = TextPrimary)
                                            }
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp),
                                        color = DeepCharcoal,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("PLAYTIME", fontFamily = SilkscreenFontFamily, fontSize = 9.sp, color = TextMuted)
                                                Text("${team.totalPlaytimeMinutes / 60}h ${team.totalPlaytimeMinutes % 60}m", fontFamily = SilkscreenFontFamily, fontSize = 14.sp, color = TextPrimary)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Controls Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (team.isCurrentUserOwner) {
                                        Button(
                                            onClick = { showInviteDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("INVITE", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { showEditTeamDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal, contentColor = TextPrimary),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("EDIT", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { confirmDisbandDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = NetherRed.copy(alpha = 0.2f), contentColor = NetherRed),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("DISBAND", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = { confirmLeaveDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = NetherRed.copy(alpha = 0.2f), contentColor = NetherRed),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("LEAVE CLAN", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Members Roster Heading
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CLAN ROSTER (${team.members.size})",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "Created: ${formatDate(team.createdAt)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // 3. Member Rows
                    items(team.members, key = { it.userId }) { member ->
                        TeamMemberRow(
                            member = member,
                            isCurrentUserOwner = team.isCurrentUserOwner,
                            currentUserId = currentUserId,
                            onRemove = { memberToRemove = member },
                            onTransfer = { memberToTransfer = member }
                        )
                    }

                    // 4. Pending Outgoing Invitations
                    if (team.isCurrentUserOwner && team.pendingInvitations.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "PENDING OUTGOING INVITATIONS (${team.pendingInvitations.size})",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 12.sp,
                                color = MinecraftGold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        items(team.pendingInvitations, key = { it.id }) { sentInv ->
                            ObsidianCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MinecraftGold, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${sentInv.inviteeName} (@${sentInv.inviteeId})",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 12.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Sent ${formatDate(sentInv.createdAt)}",
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MinecraftGold.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MinecraftGold)
                                    ) {
                                        Text(
                                            text = "PENDING",
                                            fontFamily = SilkscreenFontFamily,
                                            fontSize = 9.sp,
                                            color = MinecraftGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // NOT IN A TEAM: SHOW CALL TO ACTION & INVITATIONS
                    item {
                        ObsidianCard(
                            borderColor = EmeraldGreen.copy(alpha = 0.5f),
                            accentGlow = EmeraldGreen
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldGreen.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Groups, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "YOU ARE NOT IN A CLAN",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Create a clan with custom emblems, recruit fighters, track collective playtime and battle records, or accept an invite to join an existing team.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.widthIn(max = 480.dp),
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { showCreateTeamDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("CREATE NEW CLAN", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { selectedTab = 1 },
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal, contentColor = TextPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("EXPLORE CLANS", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Also display pending invitations preview
                    if (invitations.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "PENDING INVITATIONS (${invitations.size})",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 13.sp,
                                color = MinecraftGold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        items(invitations, key = { it.id }) { inv ->
                            ReceivedInviteCard(
                                invite = inv,
                                onAccept = { viewModel.respondInvitation(inv.id, true) },
                                onReject = { viewModel.respondInvitation(inv.id, false) }
                            )
                        }
                    }
                }
            }

            // ----------------- TAB 1: BROWSE ALL CLANS -----------------
            if (selectedTab == 1) {
                // Search bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search clans by name or leader...", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldGreen) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DeepCharcoal,
                            unfocusedContainerColor = DeepCharcoal
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                if (filteredClans.isEmpty()) {
                    item {
                        ObsidianCard {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No clans match '$searchQuery'" else "No clans formed yet on the server.",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredClans, key = { it.id }) { team ->
                        PublicTeamCard(
                            team = team,
                            onViewDetails = { viewModel.loadTeamDetails(team.id) }
                        )
                    }
                }
            }

            // ----------------- TAB 2: INVITATIONS -----------------
            if (selectedTab == 2) {
                if (invitations.isEmpty()) {
                    item {
                        ObsidianCard {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No pending clan invitations.",
                                    fontFamily = SilkscreenFontFamily,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "When clan leaders invite you, invitations will appear here.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    items(invitations, key = { it.id }) { inv ->
                        ReceivedInviteCard(
                            invite = inv,
                            onAccept = { viewModel.respondInvitation(inv.id, true) },
                            onReject = { viewModel.respondInvitation(inv.id, false) }
                        )
                    }
                }
            }

            // ----------------- TAB 3: STAFF OVERVIEW -----------------
            if (selectedTab == 3 && isStaff) {
                item {
                    Text(
                        text = "SERVER ALL CLANS (ADMINISTRATIVE)",
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 12.sp,
                        color = NetherRed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(allTeams, key = { it.id }) { team ->
                    ObsidianCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = team.name, fontFamily = SilkscreenFontFamily, fontSize = 14.sp, color = TextPrimary)
                                Text(text = "Leader: ${team.ownerName} (@${team.ownerId}) • ${team.memberCount} members", fontSize = 11.sp, color = TextSecondary)
                                if (!team.description.isNullOrBlank()) {
                                    Text(text = team.description, fontSize = 11.sp, color = TextMuted, maxLines = 1)
                                }
                            }

                            Row {
                                TextButton(onClick = { viewModel.loadTeamDetails(team.id) }) {
                                    Text("DETAILS", color = EmeraldGreen, fontSize = 11.sp)
                                }
                                if (isAdmin) {
                                    IconButton(onClick = { viewModel.deleteTeam(team.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Disband", tint = NetherRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ----------------- MODALS & DIALOGS -----------------

    // 1. Team Details Modal
    if (selectedTeamDetails != null) {
        TeamDetailsDialog(
            team = selectedTeamDetails!!,
            currentUserId = currentUserId,
            isAdmin = isAdmin,
            onDismiss = { viewModel.clearSelectedTeamDetails() },
            onEdit = {
                showEditTeamDialog = true
            },
            onInvite = {
                showInviteDialog = true
            },
            onDisband = {
                confirmDisbandDialog = true
            },
            onLeave = {
                confirmLeaveDialog = true
            },
            onRemoveMember = { m ->
                memberToRemove = m
            },
            onTransferOwnership = { m ->
                memberToTransfer = m
            }
        )
    }

    // 2. Create Team Dialog
    if (showCreateTeamDialog) {
        CreateTeamDialog(
            onDismiss = { showCreateTeamDialog = false },
            onCreate = { name, desc, logo, status ->
                viewModel.createTeam(name, desc, logo, status) {
                    showCreateTeamDialog = false
                }
            }
        )
    }

    // 3. Edit Team Dialog
    if (showEditTeamDialog && myTeam != null) {
        EditTeamDialog(
            currentTeam = myTeam!!,
            onDismiss = { showEditTeamDialog = false },
            onSave = { desc, logo, status ->
                viewModel.editMyTeam(desc, logo, status) {
                    showEditTeamDialog = false
                    viewModel.clearSelectedTeamDetails()
                }
            }
        )
    }

    // 4. Invite Member Dialog
    if (showInviteDialog) {
        InviteMemberDialog(
            viewModel = viewModel,
            onDismiss = { showInviteDialog = false },
            onSendInvite = { inviteeId ->
                viewModel.inviteTeamMember(inviteeId)
            }
        )
    }

    // 5. Confirm Disband Dialog
    if (confirmDisbandDialog) {
        AlertDialog(
            onDismissRequest = { confirmDisbandDialog = false },
            title = { Text("DISBAND CLAN?", fontFamily = SilkscreenFontFamily, color = NetherRed, fontSize = 16.sp) },
            text = {
                Text(
                    text = "Are you sure you want to permanently disband this clan? All members will be removed and this action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disbandMyTeam {
                            confirmDisbandDialog = false
                            viewModel.clearSelectedTeamDetails()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NetherRed, contentColor = Color.White)
                ) {
                    Text("DISBAND", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisbandDialog = false }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = DeepCharcoal
        )
    }

    // 6. Confirm Leave Dialog
    if (confirmLeaveDialog) {
        AlertDialog(
            onDismissRequest = { confirmLeaveDialog = false },
            title = { Text("LEAVE CLAN?", fontFamily = SilkscreenFontFamily, color = NetherRed, fontSize = 16.sp) },
            text = {
                Text("Are you sure you want to leave your clan?", color = TextSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.leaveTeam()
                        confirmLeaveDialog = false
                        viewModel.clearSelectedTeamDetails()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NetherRed, contentColor = Color.White)
                ) {
                    Text("LEAVE", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeaveDialog = false }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = DeepCharcoal
        )
    }

    // 7. Confirm Kick Member Dialog
    if (memberToRemove != null) {
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("REMOVE MEMBER?", fontFamily = SilkscreenFontFamily, color = NetherRed, fontSize = 16.sp) },
            text = {
                Text("Remove ${memberToRemove!!.displayName} (@${memberToRemove!!.userId}) from the clan?", color = TextSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeTeamMember(memberToRemove!!.userId)
                        memberToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NetherRed, contentColor = Color.White)
                ) {
                    Text("REMOVE", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = DeepCharcoal
        )
    }

    // 8. Confirm Transfer Ownership Dialog
    if (memberToTransfer != null) {
        AlertDialog(
            onDismissRequest = { memberToTransfer = null },
            title = { Text("TRANSFER OWNERSHIP?", fontFamily = SilkscreenFontFamily, color = MinecraftGold, fontSize = 16.sp) },
            text = {
                Text("Transfer clan leadership to ${memberToTransfer!!.displayName} (@${memberToTransfer!!.userId})? You will become a standard clan member.", color = TextSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.transferTeamOwnership(memberToTransfer!!.userId)
                        memberToTransfer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MinecraftGold, contentColor = ObsidianBlack)
                ) {
                    Text("TRANSFER", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToTransfer = null }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = DeepCharcoal
        )
    }
}

// ----------------- SUB-COMPONENTS -----------------

@Composable
fun PublicTeamCard(
    team: TeamSummaryDto,
    onViewDetails: () -> Unit
) {
    ObsidianCard(
        borderColor = ObsidianBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TeamLogoIcon(logo = team.logo, modifier = Modifier.size(24.dp), tint = EmeraldGreen)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = team.name,
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(status = team.status)
                        }
                        Text(
                            text = "Leader: ${team.ownerName} (@${team.ownerId})",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DeepCharcoal,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${team.memberCount} Members",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            if (!team.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = team.description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Member chips preview
            if (team.membersPreview.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Roster:", fontSize = 10.sp, color = TextMuted)
                    team.membersPreview.take(3).forEach { memberName ->
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = DeepCharcoal,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, ObsidianBorder)
                        ) {
                            Text(
                                text = memberName,
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (team.membersPreview.size > 3) {
                        Text("+${team.memberCount - 3} more", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Created ${formatDate(team.createdAt)}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = TextMuted
                )

                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("VIEW DETAILS", fontFamily = SilkscreenFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReceivedInviteCard(
    invite: com.example.data.TeamInvitationReceivedDto,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    ObsidianCard(borderColor = MinecraftGold.copy(alpha = 0.4f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invite.teamName,
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Invited by ${invite.inviterName} (@${invite.inviterId})",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                if (!invite.teamDescription.isNullOrBlank()) {
                    Text(
                        text = invite.teamDescription,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ACCEPT", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                }

                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal, contentColor = TextMuted),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("REJECT", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun TeamMemberRow(
    member: TeamMemberDto,
    isCurrentUserOwner: Boolean,
    currentUserId: String,
    onRemove: () -> Unit,
    onTransfer: () -> Unit
) {
    val isOwner = member.role == "OWNER"
    val isSelf = member.userId == currentUserId

    ObsidianCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isOwner) MinecraftGold.copy(alpha = 0.2f) else DeepCharcoal,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isOwner) MinecraftGold else ObsidianBorder),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = member.displayName.take(1).uppercase(),
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 13.sp,
                            color = if (isOwner) MinecraftGold else TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.displayName,
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "@${member.userId}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        if (isSelf) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("(You)", fontSize = 10.sp, color = EmeraldGreen)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = if (isOwner) MinecraftGold.copy(alpha = 0.2f) else DeepCharcoal
                        ) {
                            Text(
                                text = member.role,
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 9.sp,
                                color = if (isOwner) MinecraftGold else TextSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        if (member.appRole != "MEMBER") {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = if (member.appRole == "ADMIN") NetherRed.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = member.appRole,
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 8.sp,
                                    color = if (member.appRole == "ADMIN") NetherRed else EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "• Joined ${formatDate(member.joinedAt)}",
                            fontSize = 9.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = TextMuted
                        )
                    }
                }
            }

            if (isCurrentUserOwner && !isSelf) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTransfer, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer Leader", tint = MinecraftGold, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PersonRemove, contentDescription = "Kick Member", tint = NetherRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ----------------- FULL TEAM DETAILS DIALOG -----------------

@Composable
fun TeamDetailsDialog(
    team: TeamDetailsDto,
    currentUserId: String,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onInvite: () -> Unit,
    onDisband: () -> Unit,
    onLeave: () -> Unit,
    onRemoveMember: (TeamMemberDto) -> Unit,
    onTransferOwnership: (TeamMemberDto) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 620.dp)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(12.dp),
            color = DeepCharcoal,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreen.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                TeamLogoIcon(logo = team.logo, modifier = Modifier.size(26.dp), tint = EmeraldGreen)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = team.name,
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 18.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(status = team.status)
                            }
                            Text(
                                text = "Founded: ${formatDate(team.createdAt)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = ObsidianBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Full Description
                Text(
                    text = "DESCRIPTION",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 11.sp,
                    color = EmeraldGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (!team.description.isNullOrBlank()) team.description else "No description provided.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Stats overview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = ObsidianBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("LEADER", fontFamily = SilkscreenFontFamily, fontSize = 9.sp, color = TextMuted)
                            Text(team.ownerName, fontFamily = SilkscreenFontFamily, fontSize = 12.sp, color = TextPrimary)
                            Text("@${team.ownerId}", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = EmeraldGreen)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = ObsidianBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("CLAN KILLS", fontFamily = SilkscreenFontFamily, fontSize = 9.sp, color = TextMuted)
                            Text("${team.totalKills}", fontFamily = SilkscreenFontFamily, fontSize = 14.sp, color = NetherRed)
                            Text("Recorded kills", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextMuted)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = ObsidianBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("PLAYTIME", fontFamily = SilkscreenFontFamily, fontSize = 9.sp, color = TextMuted)
                            Text("${team.totalPlaytimeMinutes / 60}h ${team.totalPlaytimeMinutes % 60}m", fontFamily = SilkscreenFontFamily, fontSize = 14.sp, color = EmeraldGreen)
                            Text("Total active", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Roster section
                Text(
                    text = "MEMBERS ROSTER (${team.members.size})",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 11.sp,
                    color = EmeraldGreen
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(team.members, key = { it.userId }) { m ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ObsidianBlack,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, ObsidianBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = m.displayName,
                                        fontFamily = SilkscreenFontFamily,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "@${m.userId}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = if (m.role == "OWNER") MinecraftGold.copy(alpha = 0.2f) else DeepCharcoal
                                    ) {
                                        Text(
                                            text = m.role,
                                            fontFamily = SilkscreenFontFamily,
                                            fontSize = 8.sp,
                                            color = if (m.role == "OWNER") MinecraftGold else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                if (team.isCurrentUserOwner && m.userId != currentUserId) {
                                    Row {
                                        IconButton(onClick = { onTransferOwnership(m) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer", tint = MinecraftGold, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { onRemoveMember(m) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.PersonRemove, contentDescription = "Kick", tint = NetherRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (team.isCurrentUserOwner) {
                        Button(
                            onClick = onInvite,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("INVITE", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                        }
                        Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal, contentColor = TextPrimary),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("EDIT", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                        }
                        Button(
                            onClick = onDisband,
                            colors = ButtonDefaults.buttonColors(containerColor = NetherRed.copy(alpha = 0.2f), contentColor = NetherRed),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DISBAND", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                        }
                    } else if (team.members.any { it.userId == currentUserId }) {
                        Button(
                            onClick = onLeave,
                            colors = ButtonDefaults.buttonColors(containerColor = NetherRed.copy(alpha = 0.2f), contentColor = NetherRed),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("LEAVE CLAN", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal, contentColor = TextPrimary),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CLOSE", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ----------------- CREATE TEAM DIALOG -----------------

@Composable
fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, logo: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedLogo by remember { mutableStateOf("shield") }
    var selectedStatus by remember { mutableStateOf("ACTIVE") }

    val logoOptions = listOf(
        "shield" to "Shield",
        "swords" to "Swords",
        "crown" to "Crown",
        "dragon" to "Dragon",
        "skull" to "Skull",
        "wolf" to "Wolf",
        "flame" to "Flame",
        "heart" to "Heart"
    )

    val statusOptions = listOf("ACTIVE", "RECRUITING", "PRIVATE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("CREATE NEW CLAN", fontFamily = SilkscreenFontFamily, color = EmeraldGreen, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Clan Name (3-24 characters)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Clan Description (Optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                Text("CHOOSE CLAN EMBLEM", fontFamily = SilkscreenFontFamily, fontSize = 10.sp, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    logoOptions.take(4).forEach { (logoKey, _) ->
                        LogoSelectButton(
                            logo = logoKey,
                            isSelected = selectedLogo == logoKey,
                            onClick = { selectedLogo = logoKey }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    logoOptions.drop(4).forEach { (logoKey, _) ->
                        LogoSelectButton(
                            logo = logoKey,
                            isSelected = selectedLogo == logoKey,
                            onClick = { selectedLogo = logoKey }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("CLAN RECRUITMENT STATUS", fontFamily = SilkscreenFontFamily, fontSize = 10.sp, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statusOptions.forEach { st ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (selectedStatus == st) EmeraldGreen.copy(alpha = 0.2f) else DeepCharcoal,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedStatus == st) EmeraldGreen else ObsidianBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedStatus = st }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = st,
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 9.sp,
                                    color = if (selectedStatus == st) EmeraldGreen else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name.trim(), desc.trim(), selectedLogo, selectedStatus) },
                enabled = name.trim().length in 3..24,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark)
            ) {
                Text("CREATE", fontFamily = SilkscreenFontFamily, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMuted)
            }
        },
        containerColor = DeepCharcoal
    )
}

// ----------------- EDIT TEAM DIALOG -----------------

@Composable
fun EditTeamDialog(
    currentTeam: TeamDetailsDto,
    onDismiss: () -> Unit,
    onSave: (desc: String, logo: String, status: String) -> Unit
) {
    var desc by remember { mutableStateOf(currentTeam.description ?: "") }
    var selectedLogo by remember { mutableStateOf(currentTeam.logo) }
    var selectedStatus by remember { mutableStateOf(currentTeam.status) }

    val logoOptions = listOf("shield", "swords", "crown", "dragon", "skull", "wolf", "flame", "heart")
    val statusOptions = listOf("ACTIVE", "RECRUITING", "FULL", "PRIVATE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("EDIT CLAN DETAILS", fontFamily = SilkscreenFontFamily, color = EmeraldGreen, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Clan Description") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                Text("CLAN EMBLEM", fontFamily = SilkscreenFontFamily, fontSize = 10.sp, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    logoOptions.take(4).forEach { logoKey ->
                        LogoSelectButton(
                            logo = logoKey,
                            isSelected = selectedLogo == logoKey,
                            onClick = { selectedLogo = logoKey }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    logoOptions.drop(4).forEach { logoKey ->
                        LogoSelectButton(
                            logo = logoKey,
                            isSelected = selectedLogo == logoKey,
                            onClick = { selectedLogo = logoKey }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("STATUS", fontFamily = SilkscreenFontFamily, fontSize = 10.sp, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statusOptions.forEach { st ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (selectedStatus == st) EmeraldGreen.copy(alpha = 0.2f) else DeepCharcoal,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedStatus == st) EmeraldGreen else ObsidianBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedStatus = st }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = st,
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 8.sp,
                                    color = if (selectedStatus == st) EmeraldGreen else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(desc.trim(), selectedLogo, selectedStatus) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark)
            ) {
                Text("SAVE", fontFamily = SilkscreenFontFamily, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMuted)
            }
        },
        containerColor = DeepCharcoal
    )
}

// ----------------- INVITE MEMBER DIALOG -----------------

@Composable
fun InviteMemberDialog(
    viewModel: TgzViewModel,
    onDismiss: () -> Unit,
    onSendInvite: (inviteeId: String) -> Unit
) {
    var searchInput by remember { mutableStateOf("") }
    val searchResults by viewModel.teamMemberSearchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearchingTeamMembers.collectAsStateWithLifecycle()

    LaunchedEffect(searchInput) {
        if (searchInput.trim().isNotEmpty()) {
            viewModel.searchTeamMembers(searchInput.trim())
        } else {
            viewModel.clearTeamMemberSearchResults()
        }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.clearTeamMemberSearchResults()
            onDismiss()
        },
        title = {
            Text("INVITE MEMBER TO CLAN", fontFamily = SilkscreenFontFamily, color = EmeraldGreen, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    label = { Text("Search by Unique ID or Name") },
                    placeholder = { Text("e.g. hunter_99, sg_karthi") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldGreen) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldGreen, modifier = Modifier.size(24.dp))
                    }
                } else if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchInput.isBlank()) "Type an ID or name to search registered members." else "No members found matching '$searchInput'.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(searchResults, key = { it.id }) { user ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ObsidianBlack,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = user.displayName, fontFamily = SilkscreenFontFamily, fontSize = 12.sp, color = TextPrimary)
                                        Text(text = "@${user.id}", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextMuted)
                                        if (user.alreadyInTeam) {
                                            Text(text = "Already in '${user.teamName ?: "a team"}'", fontSize = 9.sp, color = MinecraftGold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onSendInvite(user.id)
                                            viewModel.clearTeamMemberSearchResults()
                                            onDismiss()
                                        },
                                        enabled = !user.alreadyInTeam,
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("INVITE", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                viewModel.clearTeamMemberSearchResults()
                onDismiss()
            }) {
                Text("CANCEL", color = TextMuted)
            }
        },
        containerColor = DeepCharcoal
    )
}

// ----------------- HELPERS -----------------

@Composable
fun LogoSelectButton(
    logo: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) EmeraldGreen.copy(alpha = 0.25f) else ObsidianBlack,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) EmeraldGreen else ObsidianBorder
        ),
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            TeamLogoIcon(
                logo = logo,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) EmeraldGreen else TextSecondary
            )
        }
    }
}

@Composable
fun TeamLogoIcon(
    logo: String,
    modifier: Modifier = Modifier,
    tint: Color = EmeraldGreen
) {
    val icon: ImageVector = when (logo.lowercase()) {
        "shield" -> Icons.Default.Shield
        "swords" -> Icons.Default.MilitaryTech
        "crown" -> Icons.Default.EmojiEvents
        "dragon" -> Icons.Default.Pets
        "skull" -> Icons.Default.Warning
        "wolf" -> Icons.Default.Pets
        "flame" -> Icons.Default.LocalFireDepartment
        "heart" -> Icons.Default.Favorite
        else -> Icons.Default.Shield
    }
    Icon(icon, contentDescription = logo, tint = tint, modifier = modifier)
}

@Composable
fun StatusBadge(status: String) {
    val (color, label) = when (status.uppercase()) {
        "ACTIVE" -> EmeraldGreen to "ACTIVE"
        "RECRUITING" -> MinecraftGold to "RECRUITING"
        "FULL" -> NetherRed to "FULL"
        "PRIVATE" -> TextMuted to "PRIVATE"
        else -> EmeraldGreen to status
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = label,
            fontFamily = SilkscreenFontFamily,
            fontSize = 8.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Unknown"
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
}
