package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.JoinServerModalSheet
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.VoxelHeartIcon
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AnnouncementsScreen
import com.example.ui.screens.CoinsScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.FaqScreen
import com.example.ui.screens.FeaturesScreen
import com.example.ui.screens.ForcedChangePasswordDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LiveStatusScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MemberDashboardScreen
import com.example.ui.screens.MessagingScreen
import com.example.ui.screens.ModeratorDashboardScreen
import com.example.ui.screens.PlayersScreen
import com.example.ui.screens.PlaytimeRewardsScreen
import com.example.ui.screens.RanksScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignupScreen
import com.example.ui.screens.StoreScreen
import com.example.ui.screens.TeamsScreen
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TgzViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TgzDestination(
    val route: String,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector
) {
    HOME("home", "Home", "Home", Icons.Default.Home),
    DASHBOARD("dashboard", "Dashboard", "Dash", Icons.Default.Home),
    REWARDS("rewards", "Playtime Rewards", "Crates", Icons.Default.CardGiftcard),
    LEADERBOARD("leaderboard", "Leaderboard", "Rank", Icons.Default.Leaderboard),
    ANNOUNCEMENTS("announcements", "Announcements", "News", Icons.Default.Notifications),
    MESSAGES("messages", "Messages", "Chat", Icons.Default.Forum),
    TEAMS("teams", "Clan Teams", "Clans", Icons.Default.Groups),
    SETTINGS("settings", "Settings", "Settings", Icons.Default.Settings),
    LIVE_STATUS("status", "Live Status", "Status", Icons.Default.Dns),
    PLAYERS("players", "Players", "Players", Icons.Default.Groups),
    FEATURES("features", "Server Features", "Features", Icons.Default.AutoAwesome),
    RANKS("ranks", "Ranks", "Ranks", Icons.Default.MilitaryTech),
    COINS("coins", "Coins", "Coins", Icons.Default.MonetizationOn),
    STORE("store", "Store", "Store", Icons.Default.ShoppingBag),
    COMMUNITY("community", "Community", "Discord", Icons.Default.Forum),
    FAQ("faq", "FAQ", "FAQ", Icons.AutoMirrored.Filled.HelpOutline),
    LOGIN("login", "Sign In", "Sign In", Icons.Default.Person),
    SIGNUP("signup", "Register", "Join", Icons.Default.PersonAdd)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TgzLifestealApp(
    viewModel: TgzViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: TgzDestination.HOME.route

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val playerSearchQuery by viewModel.playerSearchQuery.collectAsStateWithLifecycle()
    val selectedStoreCategory by viewModel.selectedStoreCategory.collectAsStateWithLifecycle()
    val expandedFaqIds by viewModel.expandedFaqIds.collectAsStateWithLifecycle()
    val unreadMessagesCount by viewModel.unreadMessagesCount.collectAsStateWithLifecycle()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showJoinSheet by remember { mutableStateOf(false) }
    var ipCopied by remember { mutableStateOf(false) }

    LaunchedEffect(ipCopied) {
        if (ipCopied) {
            delay(2_200L)
            ipCopied = false
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToLoginClearingStack() {
        navController.navigate(TgzDestination.LOGIN.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToDashboardClearingLogin() {
        navController.navigate(TgzDestination.DASHBOARD.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun copyServerIp() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(
            ClipData.newPlainText("TGZ Lifestyle Server IP", uiState.serverAddress)
        )
        ipCopied = true
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = "Copied ${uiState.serverAddress} to clipboard",
                duration = SnackbarDuration.Short
            )
        }
    }

    fun copyBedrockIp() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val bedrockAddress = "node2.vyroxsmp.online:10011"
        clipboard?.setPrimaryClip(
            ClipData.newPlainText("TGZ Lifestyle Bedrock Server IP", bedrockAddress)
        )
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = "Copied Bedrock IP $bedrockAddress to clipboard",
                duration = SnackbarDuration.Short
            )
        }
    }

    fun openDiscordInvite(reason: String? = null) {
        val inviteUrl = uiState.discordInviteUrl
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(inviteUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val opened = runCatching {
            context.startActivity(intent)
        }.isSuccess

        if (!opened) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Open $inviteUrl in your browser or Discord app",
                    duration = SnackbarDuration.Long
                )
            }
        } else if (!reason.isNullOrBlank()) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = "Redirecting to TGZ Discord for $reason…",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Forced admin password change prompt
    if (authState.mustChangePassword && authState.role == "ADMIN") {
        ForcedChangePasswordDialog(viewModel = viewModel, authState = authState)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DeepCharcoal,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(290.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    // Drawer Brand Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(ObsidianBlack, RoundedCornerShape(10.dp))
                                .border(1.dp, EmeraldGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            VoxelHeartIcon(tint = NetherRed, iconSize = 22.dp)
                        }
                        Column {
                            Text(
                                text = "TGZ LIFESTYLE",
                                fontFamily = SilkscreenFontFamily,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = uiState.serverAddress,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = EmeraldGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // User Identity Card in Drawer
                    if (authState.isLoggedIn && authState.user != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .background(ObsidianBlack, RoundedCornerShape(8.dp))
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = authState.user!!.displayName,
                                            fontFamily = SilkscreenFontFamily,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "@${authState.user!!.id}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = EmeraldGreen
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (authState.role) {
                                        "ADMIN" -> NetherRed
                                        "MODERATOR" -> MinecraftGold
                                        else -> EmeraldGreen
                                    }.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        when (authState.role) {
                                            "ADMIN" -> NetherRed
                                            "MODERATOR" -> MinecraftGold
                                            else -> EmeraldGreen
                                        }
                                    )
                                ) {
                                    Text(
                                        text = authState.role,
                                        fontFamily = SilkscreenFontFamily,
                                        fontSize = 9.sp,
                                        color = when (authState.role) {
                                            "ADMIN" -> NetherRed
                                            "MODERATOR" -> MinecraftGold
                                            else -> EmeraldGreen
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ObsidianBorderSubtle)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tailored Navigation Drawer Items
                    val drawerDestinations = if (authState.isLoggedIn) {
                        listOf(
                            TgzDestination.DASHBOARD,
                            TgzDestination.REWARDS,
                            TgzDestination.LEADERBOARD,
                            TgzDestination.ANNOUNCEMENTS,
                            TgzDestination.MESSAGES,
                            TgzDestination.TEAMS,
                            TgzDestination.STORE,
                            TgzDestination.RANKS,
                            TgzDestination.COINS,
                            TgzDestination.LIVE_STATUS,
                            TgzDestination.SETTINGS
                        )
                    } else {
                        listOf(
                            TgzDestination.HOME,
                            TgzDestination.LIVE_STATUS,
                            TgzDestination.PLAYERS,
                            TgzDestination.RANKS,
                            TgzDestination.COINS,
                            TgzDestination.STORE,
                            TgzDestination.REWARDS,
                            TgzDestination.LEADERBOARD,
                            TgzDestination.COMMUNITY,
                            TgzDestination.FAQ,
                            TgzDestination.LOGIN
                        )
                    }

                    drawerDestinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationDrawerItem(
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = destination.label,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (destination == TgzDestination.MESSAGES && unreadMessagesCount > 0) {
                                        Surface(shape = CircleShape, color = NetherRed) {
                                            Text(
                                                text = "$unreadMessagesCount",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            selected = selected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navigateTo(destination.route)
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = EmeraldGreen.copy(alpha = 0.16f),
                                selectedIconColor = EmeraldGreen,
                                selectedTextColor = EmeraldGreen,
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_nav_${destination.route}")
                        )
                    }

                    // Logout in Drawer if Logged In
                    if (authState.isLoggedIn) {
                        Spacer(modifier = Modifier.height(10.dp))
                        NavigationDrawerItem(
                            label = { Text("Logout", color = NetherRed) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.logout { navigateToLoginClearingStack() }
                            },
                            icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = NetherRed) },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = NetherRed,
                                unselectedTextColor = NetherRed
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedWidth = maxWidth >= 700.dp
            val primaryTabs = if (authState.isLoggedIn) {
                listOf(
                    TgzDestination.DASHBOARD,
                    TgzDestination.REWARDS,
                    TgzDestination.LEADERBOARD,
                    TgzDestination.MESSAGES,
                    TgzDestination.TEAMS
                )
            } else {
                listOf(
                    TgzDestination.HOME,
                    TgzDestination.REWARDS,
                    TgzDestination.LEADERBOARD,
                    TgzDestination.ANNOUNCEMENTS,
                    TgzDestination.LOGIN
                )
            }

            Scaffold(
                containerColor = ObsidianBlack,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = ObsidianElevated,
                            contentColor = EmeraldGreen,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ObsidianBlack.copy(alpha = 0.96f),
                            titleContentColor = TextPrimary
                        ),
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("top_bar_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open navigation menu",
                                    tint = TextPrimary
                                )
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VoxelHeartIcon(tint = NetherRed, iconSize = 18.dp)
                                Text(
                                    text = "TGZ LIFESTYLE",
                                    fontFamily = SilkscreenFontFamily,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            val mc = uiState.minecraftStatus
                            val dotColor = when {
                                mc == null -> MinecraftGold
                                mc.isOnline -> EmeraldGreen
                                else -> NetherRed
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                PulsingStatusDot(color = dotColor, dotSize = 7.dp)
                                Text(
                                    text = when {
                                        mc == null -> "…"
                                        mc.isOnline -> "${mc.onlinePlayers} ON"
                                        else -> "OFF"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = dotColor
                                )
                            }

                            if (!authState.isLoggedIn) {
                                androidx.compose.material3.Button(
                                    onClick = { navigateTo(TgzDestination.LOGIN.route) },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen,
                                        contentColor = OnEmeraldDark
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = "Sign In", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SIGN IN", fontFamily = SilkscreenFontFamily, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                            } else {
                                IconButton(onClick = { navigateTo(TgzDestination.SETTINGS.route) }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isExpandedWidth) {
                        NavigationBar(
                            containerColor = DeepCharcoal,
                            contentColor = TextPrimary,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            primaryTabs.forEach { destination ->
                                val selected = currentRoute == destination.route
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = destination.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = destination.shortLabel,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    selected = selected,
                                    onClick = { navigateTo(destination.route) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = EmeraldGreen,
                                        selectedTextColor = EmeraldGreen,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = EmeraldGreen.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.testTag("nav_${destination.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedWidth) {
                        NavigationRail(
                            containerColor = DeepCharcoal,
                            contentColor = TextPrimary,
                            modifier = Modifier.width(84.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            primaryTabs.forEach { destination ->
                                val selected = currentRoute == destination.route
                                NavigationRailItem(
                                    icon = {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = destination.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = destination.shortLabel,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    selected = selected,
                                    onClick = { navigateTo(destination.route) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = EmeraldGreen,
                                        selectedTextColor = EmeraldGreen,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = EmeraldGreen.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.testTag("rail_${destination.route}")
                                )
                            }
                        }
                    }

                    if (authState.isRestoringSession) {
                        // Secure Session Restoration Loading View (Requirement 2 & 3)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ObsidianBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = DeepCharcoal,
                                    border = BorderStroke(2.dp, EmeraldGreen),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "RESTORING SECURE SESSION",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 15.sp,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Validating session & loading admin profile...",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                CircularProgressIndicator(
                                    color = EmeraldGreen,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    } else {
                        // Main App NavHost
                        NavHost(
                            navController = navController,
                            startDestination = if (authState.isLoggedIn) TgzDestination.DASHBOARD.route else TgzDestination.HOME.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                        // 1. HOME
                        composable(TgzDestination.HOME.route) {
                            HomeScreen(
                                uiState = uiState,
                                authState = authState,
                                ipCopied = ipCopied,
                                onJoinServer = { showJoinSheet = true },
                                onJoinDiscord = { openDiscordInvite() },
                                onCopyIp = { copyServerIp() },
                                onRefresh = { viewModel.refreshNow() },
                                onNavigateToRoute = { route -> navigateTo(route) }
                            )
                        }

                        // 2. ROLE-BASED DASHBOARD (Strictly requires active authentication)
                        composable(TgzDestination.DASHBOARD.route) {
                            if (!authState.isLoggedIn || authState.user == null) {
                                LaunchedEffect(Unit) {
                                    navigateToLoginClearingStack()
                                }
                            } else {
                                when (authState.role) {
                                    "ADMIN" -> AdminDashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToRoute = { route -> navigateTo(route) }
                                    )
                                    "MODERATOR" -> ModeratorDashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToRoute = { route -> navigateTo(route) }
                                    )
                                    else -> MemberDashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToRoute = { route -> navigateTo(route) }
                                    )
                                }
                            }
                        }

                        // 3. AUTHENTICATION
                        composable(TgzDestination.LOGIN.route) {
                            LoginScreen(
                                viewModel = viewModel,
                                authState = authState,
                                onNavigateToSignup = { navigateTo(TgzDestination.SIGNUP.route) },
                                onLoginSuccess = { navigateToDashboardClearingLogin() }
                            )
                        }

                        composable(TgzDestination.SIGNUP.route) {
                            SignupScreen(
                                viewModel = viewModel,
                                authState = authState,
                                onNavigateToLogin = { navigateTo(TgzDestination.LOGIN.route) },
                                onSignupSuccess = { navigateToDashboardClearingLogin() }
                            )
                        }

                        // 4. ANNOUNCEMENTS
                        composable(TgzDestination.ANNOUNCEMENTS.route) {
                            AnnouncementsScreen(viewModel = viewModel)
                        }

                        // 5. MESSAGING
                        composable(TgzDestination.MESSAGES.route) {
                            MessagingScreen(viewModel = viewModel)
                        }

                        // 6. PLAYTIME REWARDS (5 CRATES ONLY)
                        composable(TgzDestination.REWARDS.route) {
                            PlaytimeRewardsScreen(viewModel = viewModel)
                        }

                        // 7. LEADERBOARD
                        composable(TgzDestination.LEADERBOARD.route) {
                            LeaderboardScreen(viewModel = viewModel)
                        }

                        // 8. TEAMS
                        composable(TgzDestination.TEAMS.route) {
                            TeamsScreen(viewModel = viewModel)
                        }

                        // 9. SETTINGS
                        composable(TgzDestination.SETTINGS.route) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onLogoutComplete = { navigateToLoginClearingStack() }
                            )
                        }

                        // 10. EXISTING PAGES (Preserved 100%)
                        composable(TgzDestination.LIVE_STATUS.route) {
                            val javaStatus by viewModel.javaServerStatus.collectAsStateWithLifecycle()
                            val bedrockStatus by viewModel.bedrockServerStatus.collectAsStateWithLifecycle()
                            LiveStatusScreen(
                                uiState = uiState,
                                ipCopied = ipCopied,
                                onCopyIp = { copyServerIp() },
                                onRefresh = { viewModel.refreshNow() },
                                onNavigateToPlayers = { navigateTo(TgzDestination.PLAYERS.route) },
                                onNavigateToCommunity = { navigateTo(TgzDestination.COMMUNITY.route) },
                                javaStatus = javaStatus,
                                bedrockStatus = bedrockStatus,
                                onCopyBedrockIp = { copyBedrockIp() },
                                onRefreshJava = { viewModel.fetchJavaServerStatus() },
                                onRefreshBedrock = { viewModel.fetchBedrockServerStatus() }
                            )
                        }

                        composable(TgzDestination.PLAYERS.route) {
                            PlayersScreen(
                                uiState = uiState,
                                searchQuery = playerSearchQuery,
                                onSearchChange = { viewModel.updatePlayerSearch(it) },
                                onRefresh = { viewModel.refreshNow() },
                                onCopyIp = { copyServerIp() }
                            )
                        }

                        composable(TgzDestination.FEATURES.route) {
                            FeaturesScreen()
                        }

                        composable(TgzDestination.RANKS.route) {
                            RanksScreen(
                                onBuyOnDiscord = { rankName -> openDiscordInvite(rankName) }
                            )
                        }

                        composable(TgzDestination.COINS.route) {
                            CoinsScreen(
                                onBuyCoins = { amount, price ->
                                    openDiscordInvite("Coins Purchase: $amount Coins ($price)")
                                }
                            )
                        }

                        composable(TgzDestination.STORE.route) {
                            StoreScreen(
                                selectedCategory = selectedStoreCategory,
                                onSelectCategory = { viewModel.selectStoreCategory(it) },
                                onBuyOnDiscord = { packName -> openDiscordInvite(packName) }
                            )
                        }

                        composable(TgzDestination.COMMUNITY.route) {
                            CommunityScreen(
                                uiState = uiState,
                                onJoinDiscord = { openDiscordInvite() },
                                onRefresh = { viewModel.refreshNow() }
                            )
                        }

                        composable(TgzDestination.FAQ.route) {
                            FaqScreen(
                                expandedFaqIds = expandedFaqIds,
                                onToggleFaq = { viewModel.toggleFaqExpanded(it) },
                                onJoinDiscord = { openDiscordInvite() }
                            )
                        }
                    }
                }
            }
            }
        }
    }

    if (showJoinSheet) {
        JoinServerModalSheet(
            serverAddress = uiState.serverAddress,
            isOnline = uiState.minecraftStatus?.isOnline,
            copied = ipCopied,
            onCopyIp = { copyServerIp() },
            onOpenDiscord = {
                showJoinSheet = false
                openDiscordInvite()
            },
            onDismiss = { showJoinSheet = false }
        )
    }

    if (appUpdateInfo != null) {
        AppUpdateDialog(
            updateInfo = appUpdateInfo!!,
            onDismiss = { viewModel.dismissAppUpdate() }
        )
    }
}
