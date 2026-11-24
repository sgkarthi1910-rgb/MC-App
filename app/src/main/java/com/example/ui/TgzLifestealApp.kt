package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.JoinServerModalSheet
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.VoxelHeartIcon
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.FaqScreen
import com.example.ui.screens.FeaturesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveStatusScreen
import com.example.ui.screens.PlayersScreen
import com.example.ui.screens.RanksScreen
import com.example.ui.screens.CoinsScreen
import com.example.ui.screens.StoreScreen
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianElevated
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
    LIVE_STATUS("status", "Live Status", "Status", Icons.Default.Dns),
    PLAYERS("players", "Players", "Players", Icons.Default.Groups),
    FEATURES("features", "Server Features", "Features", Icons.Default.AutoAwesome),
    RANKS("ranks", "Ranks", "Ranks", Icons.Default.MilitaryTech),
    COINS("coins", "Coins", "Coins", Icons.Default.MonetizationOn),
    STORE("store", "Store", "Store", Icons.Default.ShoppingBag),
    COMMUNITY("community", "Community", "Discord", Icons.Default.Forum),
    FAQ("faq", "FAQ", "FAQ", Icons.AutoMirrored.Filled.HelpOutline)
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
    val playerSearchQuery by viewModel.playerSearchQuery.collectAsStateWithLifecycle()
    val selectedStoreCategory by viewModel.selectedStoreCategory.collectAsStateWithLifecycle()
    val expandedFaqIds by viewModel.expandedFaqIds.collectAsStateWithLifecycle()

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

    fun copyServerIp() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(
            ClipData.newPlainText("TGZ Lifesteal Server IP", uiState.serverAddress)
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

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ObsidianBorderSubtle)
                    Spacer(modifier = Modifier.height(8.dp))

                    // All 8 Navigation Destinations
                    TgzDestination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.titleMedium
                                )
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
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedWidth = maxWidth >= 700.dp
            val primaryTabs = listOf(
                TgzDestination.HOME,
                TgzDestination.RANKS,
                TgzDestination.COINS,
                TgzDestination.STORE
            )

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
                            IconButton(
                                onClick = { viewModel.refreshNow() },
                                enabled = !uiState.isRefreshing,
                                modifier = Modifier.testTag("top_bar_refresh_button")
                            ) {
                                if (uiState.isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = EmeraldGreen
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh live status",
                                        tint = EmeraldGreen
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isExpandedWidth) {
                        NavigationBar(
                            containerColor = DeepCharcoal,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            primaryTabs.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = dest.icon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.shortLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = EmeraldGreen,
                                        selectedTextColor = EmeraldGreen,
                                        indicatorColor = EmeraldGreen.copy(alpha = 0.16f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${dest.route}")
                                )
                            }

                            // 5th tab opens full drawer for other sections
                            val isSecondaryRoute = primaryTabs.none { it.route == currentRoute }
                            NavigationBarItem(
                                selected = isSecondaryRoute,
                                onClick = { scope.launch { drawerState.open() } },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "More sections"
                                    )
                                },
                                label = {
                                    Text(
                                        text = "More",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MinecraftGold,
                                    selectedTextColor = MinecraftGold,
                                    indicatorColor = MinecraftGold.copy(alpha = 0.16f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("bottom_nav_more")
                            )
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
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TgzDestination.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = dest.icon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.shortLabel,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = EmeraldGreen,
                                        selectedTextColor = EmeraldGreen,
                                        indicatorColor = EmeraldGreen.copy(alpha = 0.16f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("rail_nav_${dest.route}")
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = TgzDestination.HOME.route,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        composable(TgzDestination.HOME.route) {
                            HomeScreen(
                                uiState = uiState,
                                ipCopied = ipCopied,
                                onJoinServer = {
                                    copyServerIp()
                                    showJoinSheet = true
                                },
                                onJoinDiscord = { openDiscordInvite() },
                                onCopyIp = { copyServerIp() },
                                onRefresh = { viewModel.refreshNow() },
                                onNavigateToRoute = { route -> navigateTo(route) }
                            )
                        }

                        composable(TgzDestination.LIVE_STATUS.route) {
                            LiveStatusScreen(
                                uiState = uiState,
                                ipCopied = ipCopied,
                                onCopyIp = { copyServerIp() },
                                onRefresh = { viewModel.refreshNow() },
                                onNavigateToPlayers = { navigateTo(TgzDestination.PLAYERS.route) },
                                onNavigateToCommunity = { navigateTo(TgzDestination.COMMUNITY.route) }
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
}
