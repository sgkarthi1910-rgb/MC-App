package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.SessionItemDto
import com.example.data.UserManagementItemDto
import com.example.ui.components.ObsidianCard
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.NetherRedBright
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
fun SettingsScreen(
    viewModel: TgzViewModel,
    onLogoutComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val memberSettings by viewModel.memberSettings.collectAsStateWithLifecycle()
    val adminSettings by viewModel.adminSettings.collectAsStateWithLifecycle()
    val userList by viewModel.userManagementList.collectAsStateWithLifecycle()
    val activeSessions by viewModel.activeSessions.collectAsStateWithLifecycle()
    val actionMessage by viewModel.settingsActionMessage.collectAsStateWithLifecycle()
    val isSettingsLoading by viewModel.isSettingsLoading.collectAsStateWithLifecycle()

    val currentBackendUrl by viewModel.backendUrl.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showServerConfigDialog by remember { mutableStateOf(false) }
    var customBackendInput by remember(currentBackendUrl) { mutableStateOf(currentBackendUrl) }

    var otaVersionCode by remember(adminSettings) { mutableStateOf(adminSettings["app_version_code"] ?: "3") }
    var otaVersionName by remember(adminSettings) { mutableStateOf(adminSettings["app_version_name"] ?: "1.2") }
    var otaApkUrl by remember(adminSettings) { mutableStateOf(adminSettings["app_apk_url"] ?: "https://github.com/sgkarthi1910-rgb/MC-App/releases/download/v1.2/TGZ-Lifestyle-v1.2.apk") }
    var otaChangelog by remember(adminSettings) { mutableStateOf(adminSettings["app_changelog"] ?: "New features and bug fixes.") }
    var otaForceUpdate by remember(adminSettings) { mutableStateOf(adminSettings["app_force_update"] == "true") }

    val isAdmin = authState.role == "ADMIN"
    val isStaff = authState.role == "ADMIN" || authState.role == "MODERATOR"

    LaunchedEffect(Unit) {
        viewModel.loadMemberSettings()
        viewModel.loadSessions(all = isAdmin)
        if (isAdmin) {
            viewModel.loadAdminSettings()
            viewModel.loadUserManagementList()
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
                        Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SETTINGS & ACCOUNT",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 18.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = {
                        viewModel.loadMemberSettings()
                        viewModel.loadSessions(all = isAdmin)
                        if (isAdmin) viewModel.loadAdminSettings()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EmeraldGreen)
                    }
                }
            }

            // Message Feedback
            if (actionMessage != null) {
                item {
                    ObsidianCard(borderColor = EmeraldGreen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = actionMessage!!, color = EmeraldGreen, fontSize = 13.sp)
                            TextButton(onClick = { viewModel.clearSettingsMessage() }) {
                                Text("DISMISS", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Tabs for Admin
            if (isAdmin) {
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
                            text = { Text("ACCOUNT", fontFamily = SilkscreenFontFamily, fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("USERS", fontFamily = SilkscreenFontFamily, fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("SYSTEM", fontFamily = SilkscreenFontFamily, fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // TAB 0: MEMBER ACCOUNT SETTINGS
                item {
                    ObsidianCard(
                        borderColor = EmeraldGreen.copy(alpha = 0.5f),
                        accentGlow = EmeraldGreen
                    ) {
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
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldGreen)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = authState.user?.displayName ?: "Survivor",
                                        fontFamily = SilkscreenFontFamily,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Unique ID: @${authState.user?.id ?: "unknown"}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
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
                                    fontSize = 10.sp,
                                    color = when (authState.role) {
                                        "ADMIN" -> NetherRed
                                        "MODERATOR" -> MinecraftGold
                                        else -> EmeraldGreen
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        val dateFormatted = remember(authState.user?.createdAt) {
                            authState.user?.createdAt?.let {
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                            } ?: "Official Genesis"
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Account created on $dateFormatted • Status: ${authState.user?.status ?: "ACTIVE"}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showChangePasswordDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal, contentColor = TextPrimary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CHANGE PASSWORD", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                            }

                            Button(
                                onClick = { viewModel.logout(onComplete = onLogoutComplete) },
                                colors = ButtonDefaults.buttonColors(containerColor = NetherRed.copy(alpha = 0.2f), contentColor = NetherRed),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("LOGOUT", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // BACKEND SERVER CONNECTION
                item {
                    ObsidianCard(borderColor = EmeraldGreen.copy(alpha = 0.3f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "BACKEND SERVER URL",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 12.sp,
                                    color = EmeraldGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = viewModel.getBackendUrl(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Button(
                                onClick = {
                                    customBackendInput = viewModel.getBackendUrl()
                                    showServerConfigDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal, contentColor = EmeraldGreen),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("CONFIG", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // APP VERSION & OTA UPDATE CHECK
                item {
                    ObsidianCard(borderColor = EmeraldGreen.copy(alpha = 0.3f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "APP VERSION",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 12.sp,
                                    color = EmeraldGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Button(
                                onClick = { viewModel.checkForAppUpdate() },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal, contentColor = EmeraldGreen),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("CHECK UPDATE", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // ACTIVE SESSIONS
                item {
                    Text(
                        text = "ACTIVE PERSISTENT SESSIONS (${activeSessions.size})",
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(activeSessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        onRevoke = { viewModel.revokeSession(session.id) }
                    )
                }
            } else if (selectedTab == 1 && isAdmin) {
                // TAB 1: USER & ROLE MANAGEMENT (ADMIN ONLY)
                item {
                    Text(
                        text = "USER & ROLE MANAGEMENT",
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(userList, key = { it.id }) { user ->
                    UserManagementRow(
                        user = user,
                        currentAdminId = authState.user?.id ?: "",
                        onPromoteModerator = { viewModel.updateUserRole(user.id, "MODERATOR") },
                        onDemoteMember = { viewModel.updateUserRole(user.id, "MEMBER") },
                        onSuspend = { viewModel.updateUserStatus(user.id, "SUSPENDED") },
                        onReactivate = { viewModel.updateUserStatus(user.id, "ACTIVE") }
                    )
                }
            } else if (selectedTab == 2 && isAdmin) {
                // TAB 2: SYSTEM & MINECRAFT PLUGIN SETTINGS (ADMIN ONLY)

                // ADMIN AUTHENTICATION DIAGNOSTIC (Requirement 5)
                item {
                    ObsidianCard(
                        borderColor = EmeraldGreen.copy(alpha = 0.6f),
                        accentGlow = EmeraldGreen
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ADMIN AUTHENTICATION DIAGNOSTIC",
                                        fontFamily = SilkscreenFontFamily,
                                        fontSize = 12.sp,
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = DeepCharcoal,
                                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "AUTHENTICATED",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        color = EmeraldGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)

                            SettingsDiagField("Current Account ID", "@${authState.user?.id ?: "N/A"}", TextPrimary)
                            SettingsDiagField("Current Account Role", authState.role, EmeraldGreen)
                            SettingsDiagField(
                                "Session Restoration Status",
                                if (authState.sessionRestoredSuccessfully) "Active (Validated & Restored)" else "Active (Session Restored)",
                                EmeraldGreen
                            )
                            SettingsDiagField(
                                "Admin Profile Loading Status",
                                if (authState.user != null) "Loaded (@${authState.user?.id})" else "Loading...",
                                EmeraldGreen
                            )
                            SettingsDiagField(
                                "Password Change Saved Status",
                                if (authState.mustChangePassword) "Temporary (Bootstrap Credentials)" else "Saved Permanently (Salted PBKDF2)",
                                if (authState.mustChangePassword) MinecraftGold else EmeraldGreen
                            )
                            SettingsDiagField("Persistent Storage Status", "Connected (Durable Store)", EmeraldGreen)
                            SettingsDiagField("Application Version", "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", TextSecondary)
                        }
                    }
                }

                item {
                    ObsidianCard(borderColor = MinecraftGold.copy(alpha = 0.4f)) {
                        Text(
                            text = "MINECRAFT SERVER PLUGIN INTEGRATION",
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 14.sp,
                            color = MinecraftGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Configure your Minecraft server plugin with the following credentials to securely push live player UUIDs, playtime, kills, and deaths into the TGZ Leaderboard:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val pluginKey = adminSettings["minecraft_plugin_api_key"] ?: "tgz_plugin_sec_99a8b7c6d5e4f3a2"
                        OutlinedTextField(
                            value = pluginKey,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("X-Plugin-Key Header") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Plugin API Key", pluginKey))
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = EmeraldGreen)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = ObsidianBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Target Endpoint: POST /api/minecraft/report-stats",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = EmeraldGreen
                        )
                    }
                }

                // OVER-THE-AIR (OTA) APP UPDATES (ADMIN ONLY)
                item {
                    ObsidianCard(borderColor = EmeraldGreen.copy(alpha = 0.5f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OVER-THE-AIR (OTA) APP UPDATES",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 14.sp,
                                color = EmeraldGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Release new versions to all devices via the Vercel backend without redistributing APK files manually. Devices check for updates automatically on startup.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = otaVersionCode,
                                onValueChange = { otaVersionCode = it },
                                label = { Text("Version Code (e.g. 2)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = ObsidianBorder
                                )
                            )

                            OutlinedTextField(
                                value = otaVersionName,
                                onValueChange = { otaVersionName = it },
                                label = { Text("Version Name (e.g. 1.1)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = ObsidianBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = otaApkUrl,
                            onValueChange = { otaApkUrl = it },
                            label = { Text("APK Download URL") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = ObsidianBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = otaChangelog,
                            onValueChange = { otaChangelog = it },
                            label = { Text("What's New / Release Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = ObsidianBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Require Immediate Update:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Button(
                                onClick = { otaForceUpdate = !otaForceUpdate },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (otaForceUpdate) NetherRed.copy(alpha = 0.25f) else DeepCharcoal,
                                    contentColor = if (otaForceUpdate) NetherRedBright else TextMuted
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (otaForceUpdate) "FORCED (YES)" else "OPTIONAL (NO)",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.updateAdminSettings(
                                    mapOf(
                                        "app_version_code" to otaVersionCode.trim(),
                                        "app_version_name" to otaVersionName.trim(),
                                        "app_apk_url" to otaApkUrl.trim(),
                                        "app_changelog" to otaChangelog.trim(),
                                        "app_force_update" to otaForceUpdate.toString(),
                                        "app_min_supported_version_code" to if (otaForceUpdate) otaVersionCode.trim() else "1"
                                    )
                                )
                            },
                            enabled = !isSettingsLoading,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSettingsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = OnEmeraldDark,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = OnEmeraldDark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PUBLISH UPDATE TO BACKEND",
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 12.sp,
                                    color = OnEmeraldDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (actionMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = actionMessage!!,
                                color = if (actionMessage!!.contains("Failed")) NetherRed else EmeraldGreen,
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordModal(
            onDismiss = { showChangePasswordDialog = false },
            onSubmit = { current, newPass, confirmPass ->
                viewModel.changePassword(current, newPass, confirmPass, onReLoginRequired = onLogoutComplete)
                showChangePasswordDialog = false
            }
        )
    }

    if (showServerConfigDialog) {
        AlertDialog(
            onDismissRequest = { showServerConfigDialog = false },
            title = {
                Text(
                    text = "BACKEND SERVER URL",
                    fontFamily = SilkscreenFontFamily,
                    color = EmeraldGreen,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set where this APK connects for Auth, Dashboards, Crates & Teams.\nPaste your live Vercel URL (e.g. https://your-project.vercel.app/), local Wi-Fi IP, or Emulator 10.0.2.2.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customBackendInput,
                        onValueChange = { customBackendInput = it },
                        label = { Text("Server URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DeepCharcoal,
                            unfocusedContainerColor = DeepCharcoal
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Quick Presets:", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { customBackendInput = "https://mc-app-ten.vercel.app/" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Vercel Cloud (Live)", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { customBackendInput = "http://10.70.99.78:3000/" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Wi-Fi IP", fontSize = 11.sp, color = TextSecondary)
                        }
                        Button(
                            onClick = { customBackendInput = "http://10.0.2.2:3000/" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Emulator", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = customBackendInput.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.setBackendUrl(trimmed)
                        }
                        showServerConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark)
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerConfigDialog = false }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = ObsidianBlack
        )
    }
}

@Composable
fun SessionRow(
    session: SessionItemDto,
    onRevoke: () -> Unit
) {
    val dateFormatted = remember(session.createdAt) {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(session.createdAt))
    }

    ObsidianCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Devices, contentDescription = null, tint = if (session.isCurrent) EmeraldGreen else TextMuted, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.deviceInfo ?: "Unknown Device",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        if (session.isCurrent) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(3.dp), color = EmeraldGreen.copy(alpha = 0.2f)) {
                                Text("THIS DEVICE", fontFamily = SilkscreenFontFamily, fontSize = 9.sp, color = EmeraldGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                    }
                    Text(
                        text = "IP: ${session.ipAddress ?: "unknown"} • Started $dateFormatted",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            if (!session.isCurrent) {
                TextButton(onClick = onRevoke) {
                    Text("REVOKE", color = NetherRed, fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun UserManagementRow(
    user: UserManagementItemDto,
    currentAdminId: String,
    onPromoteModerator: () -> Unit,
    onDemoteMember: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit
) {
    ObsidianCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = user.displayName, fontFamily = SilkscreenFontFamily, fontSize = 13.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "@${user.id}", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = when (user.role) {
                            "ADMIN" -> NetherRed
                            "MODERATOR" -> MinecraftGold
                            else -> EmeraldGreen
                        }.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = user.role,
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 9.sp,
                            color = when (user.role) {
                                "ADMIN" -> NetherRed
                                "MODERATOR" -> MinecraftGold
                                else -> EmeraldGreen
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "Status: ${user.status} • Active Sessions: ${user.activeSessions}",
                    fontSize = 11.sp,
                    color = if (user.status == "ACTIVE") TextSecondary else NetherRed
                )
            }

            // Controls (Cannot modify own admin role)
            if (user.id != currentAdminId && user.role != "ADMIN") {
                Row {
                    if (user.role == "MEMBER") {
                        TextButton(onClick = onPromoteModerator) {
                            Text("+MOD", color = MinecraftGold, fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                        }
                    } else if (user.role == "MODERATOR") {
                        TextButton(onClick = onDemoteMember) {
                            Text("-MOD", color = TextMuted, fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                        }
                    }

                    if (user.status == "ACTIVE") {
                        TextButton(onClick = onSuspend) {
                            Text("SUSPEND", color = NetherRed, fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                        }
                    } else {
                        TextButton(onClick = onReactivate) {
                            Text("RESTORE", color = EmeraldGreen, fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePasswordModal(
    onDismiss: () -> Unit,
    onSubmit: (current: String, newPass: String, confirmPass: String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CHANGE PASSWORD", fontFamily = SilkscreenFontFamily, color = EmeraldGreen, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("New Password (min 8 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(current, newPass, confirmPass) },
                enabled = current.isNotBlank() && newPass.length >= 8 && newPass == confirmPass,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark)
            ) {
                Text("UPDATE", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextMuted) }
        },
        containerColor = DeepCharcoal
    )
}

@Composable
fun SettingsDiagField(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
