package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ConversationDetailResponse
import com.example.data.ConversationDto
import com.example.data.MessageDto
import com.example.data.UserSearchDto
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
fun MessagingScreen(
    viewModel: TgzViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentConv by viewModel.currentConversation.collectAsStateWithLifecycle()
    val isMessagingLoading by viewModel.isMessagingLoading.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    var showNewChatDialog by remember { mutableStateOf(false) }
    var activePartnerId by remember { mutableStateOf<String?>(null) }
    var conversationFilter by remember { mutableStateOf("") }
    var showDetailsPanel by remember { mutableStateOf(true) }
    var confirmClearConvDialog by remember { mutableStateOf(false) }

    val currentUserId = authState.user?.id ?: ""
    val isStaff = authState.role == "ADMIN" || authState.role == "MODERATOR"

    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    // Keep activePartnerId synchronized if conversation changes
    LaunchedEffect(currentConv) {
        if (currentConv != null) {
            activePartnerId = currentConv!!.partner.id
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isDesktopOrTablet = maxWidth >= 720.dp

            if (isDesktopOrTablet) {
                // DESKTOP / TABLET 3-PART DISCORD-STYLE VIEW
                Row(modifier = Modifier.fillMaxSize()) {
                    // 1. LEFT SIDEBAR (CONVERSATIONS LIST & SEARCH)
                    ConversationSidebar(
                        modifier = Modifier
                            .width(290.dp)
                            .fillMaxHeight(),
                        conversations = conversations,
                        activePartnerId = activePartnerId,
                        filterText = conversationFilter,
                        onFilterChange = { conversationFilter = it },
                        isLoading = isMessagingLoading,
                        onNewChatClick = { showNewChatDialog = true },
                        onSelectConversation = { partnerId ->
                            activePartnerId = partnerId
                            viewModel.openConversation(partnerId)
                        },
                        onRefresh = { viewModel.loadConversations() }
                    )

                    VerticalDivider(color = ObsidianBorder, thickness = 1.dp)

                    // 2. MAIN CHAT AREA
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (currentConv != null && activePartnerId != null) {
                            DiscordChatMainView(
                                modifier = Modifier.fillMaxSize(),
                                currentUserId = currentUserId,
                                isStaff = isStaff,
                                conversation = currentConv!!,
                                isMobile = false,
                                onBack = {
                                    activePartnerId = null
                                    viewModel.closeConversation()
                                },
                                onSendMessage = { text ->
                                    viewModel.sendMessage(activePartnerId!!, text)
                                },
                                onDeleteMessage = { msgId ->
                                    viewModel.deleteMessage(msgId, activePartnerId)
                                },
                                onToggleDetails = { showDetailsPanel = !showDetailsPanel },
                                onClearConversation = { confirmClearConvDialog = true }
                            )
                        } else {
                            DiscordEmptyChatPlaceholder(onStartChat = { showNewChatDialog = true })
                        }
                    }

                    // 3. OPTIONAL DETAILS PANEL (RIGHT SIDEBAR)
                    if (currentConv != null && activePartnerId != null && showDetailsPanel) {
                        VerticalDivider(color = ObsidianBorder, thickness = 1.dp)
                        ConversationDetailsPanel(
                            modifier = Modifier
                                .width(240.dp)
                                .fillMaxHeight(),
                            partner = currentConv!!.partner,
                            onClearHistory = { confirmClearConvDialog = true },
                            onClose = { showDetailsPanel = false }
                        )
                    }
                }
            } else {
                // MOBILE RESPONSIVE VIEW: Toggle between list & full-screen chat
                if (activePartnerId != null && currentConv != null) {
                    DiscordChatMainView(
                        modifier = Modifier.fillMaxSize(),
                        currentUserId = currentUserId,
                        isStaff = isStaff,
                        conversation = currentConv!!,
                        isMobile = true,
                        onBack = {
                            activePartnerId = null
                            viewModel.closeConversation()
                            viewModel.loadConversations()
                        },
                        onSendMessage = { text ->
                            viewModel.sendMessage(activePartnerId!!, text)
                        },
                        onDeleteMessage = { msgId ->
                            viewModel.deleteMessage(msgId, activePartnerId)
                        },
                        onToggleDetails = { showDetailsPanel = !showDetailsPanel },
                        onClearConversation = { confirmClearConvDialog = true }
                    )
                } else {
                    ConversationSidebar(
                        modifier = Modifier.fillMaxSize(),
                        conversations = conversations,
                        activePartnerId = activePartnerId,
                        filterText = conversationFilter,
                        onFilterChange = { conversationFilter = it },
                        isLoading = isMessagingLoading,
                        onNewChatClick = { showNewChatDialog = true },
                        onSelectConversation = { partnerId ->
                            activePartnerId = partnerId
                            viewModel.openConversation(partnerId)
                        },
                        onRefresh = { viewModel.loadConversations() }
                    )
                }
            }
        }
    }

    // ----------------- DIALOGS -----------------

    // 1. New Chat / Search User Dialog
    if (showNewChatDialog) {
        DiscordUserSearchDialog(
            viewModel = viewModel,
            onDismiss = { showNewChatDialog = false },
            onSelectUser = { targetUserId ->
                showNewChatDialog = false
                activePartnerId = targetUserId
                viewModel.openConversation(targetUserId)
            }
        )
    }

    // 2. Confirm Clear Conversation Dialog
    if (confirmClearConvDialog && activePartnerId != null) {
        AlertDialog(
            onDismissRequest = { confirmClearConvDialog = false },
            title = {
                Text("CLEAR CONVERSATION?", fontFamily = SilkscreenFontFamily, color = NetherRed, fontSize = 16.sp)
            },
            text = {
                Text(
                    text = "Are you sure you want to clear this entire chat history? This cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConversation(activePartnerId!!) {
                            confirmClearConvDialog = false
                            activePartnerId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NetherRed, contentColor = Color.White)
                ) {
                    Text("CLEAR", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearConvDialog = false }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = DeepCharcoal
        )
    }
}

// ----------------- COMPONENT 1: CONVERSATIONS SIDEBAR -----------------

@Composable
fun ConversationSidebar(
    modifier: Modifier = Modifier,
    conversations: List<ConversationDto>,
    activePartnerId: String?,
    filterText: String,
    onFilterChange: (String) -> Unit,
    isLoading: Boolean,
    onNewChatClick: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = modifier
            .background(DeepCharcoal)
            .padding(12.dp)
    ) {
        // Sidebar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Forum, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MESSAGES",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onNewChatClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NEW", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search bar
        OutlinedTextField(
            value = filterText,
            onValueChange = onFilterChange,
            placeholder = { Text("Search conversations...", fontSize = 12.sp, color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (filterText.isNotEmpty()) {
                    IconButton(onClick = { onFilterChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(14.dp))
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
                focusedContainerColor = ObsidianBlack,
                unfocusedContainerColor = ObsidianBlack
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = ObsidianBorder)
        Spacer(modifier = Modifier.height(6.dp))

        // Conversations List
        val filtered = remember(conversations, filterText) {
            if (filterText.isBlank()) conversations
            else conversations.filter {
                it.partnerName.contains(filterText, ignoreCase = true) ||
                        it.partnerId.contains(filterText, ignoreCase = true) ||
                        (it.lastMessage ?: "").contains(filterText, ignoreCase = true)
            }
        }

        if (isLoading && conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldGreen, modifier = Modifier.size(28.dp))
            }
        } else if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (filterText.isNotBlank()) "No conversations found" else "No direct chats yet",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = SilkscreenFontFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Click NEW to message any TGZ player",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it.id }) { conv ->
                    DiscordConversationRow(
                        conv = conv,
                        isSelected = conv.partnerId == activePartnerId,
                        onClick = { onSelectConversation(conv.partnerId) }
                    )
                }
            }
        }
    }
}

@Composable
fun DiscordConversationRow(
    conv: ConversationDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dateText = remember(conv.lastMessageTime) {
        conv.lastMessageTime?.let {
            val now = System.currentTimeMillis()
            val diff = now - it
            if (diff < 86400000L) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
            } else {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it))
            }
        } ?: ""
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) EmeraldGreen.copy(alpha = 0.18f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) EmeraldGreen.copy(alpha = 0.6f) else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online indicator
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    shape = CircleShape,
                    color = ObsidianBlack,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) EmeraldGreen else ObsidianBorder
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = conv.partnerName.take(1).uppercase(),
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 14.sp,
                            color = if (isSelected) EmeraldGreen else TextPrimary
                        )
                    }
                }

                // Presence dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(EmeraldGreen, CircleShape)
                        .border(1.5.dp, DeepCharcoal, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // User Info & snippet
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = conv.partnerName,
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 12.sp,
                        color = if (isSelected) EmeraldGreen else TextPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Text(
                        text = dateText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = conv.lastMessage ?: "Started a conversation",
                        fontSize = 11.sp,
                        color = if (conv.unreadCount > 0) TextPrimary else TextSecondary,
                        fontWeight = if (conv.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    if (conv.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = NetherRed,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${conv.unreadCount}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- COMPONENT 2: DISCORD MAIN CHAT VIEW -----------------

@Composable
fun DiscordChatMainView(
    modifier: Modifier = Modifier,
    currentUserId: String,
    isStaff: Boolean,
    conversation: ConversationDetailResponse,
    isMobile: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onToggleDetails: () -> Unit,
    onClearConversation: () -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(conversation.messages.size) {
        if (conversation.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversation.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // TOP CHAT HEADER
        Surface(
            color = DeepCharcoal,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMobile) {
                        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldGreen)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Partner avatar
                    Surface(
                        shape = CircleShape,
                        color = ObsidianBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.6f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = conversation.partner.displayName.take(1).uppercase(),
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 14.sp,
                                color = EmeraldGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = conversation.partner.displayName,
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "@${conversation.partner.id}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                            if (conversation.partner.role != "MEMBER") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = if (conversation.partner.role == "ADMIN") NetherRed.copy(alpha = 0.2f) else MinecraftGold.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, if (conversation.partner.role == "ADMIN") NetherRed else MinecraftGold)
                                ) {
                                    Text(
                                        text = conversation.partner.role,
                                        fontFamily = SilkscreenFontFamily,
                                        fontSize = 8.sp,
                                        color = if (conversation.partner.role == "ADMIN") NetherRed else MinecraftGold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(EmeraldGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (conversation.partner.teamName != null) "Clan: ${conversation.partner.teamName}" else "Active player",
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Header Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClearConversation, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleDetails, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "Profile Details", tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // MESSAGES STREAM
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First greeting item
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DeepCharcoal,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This is the start of your direct chat with ${conversation.partner.displayName}",
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "@${conversation.partner.id} • TGZ Lifesteal Community",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.5f), modifier = Modifier.widthIn(max = 240.dp))
                }
            }

            // Message Items
            items(conversation.messages, key = { it.id }) { msg ->
                val isMe = msg.senderId == currentUserId
                DiscordMessageBubble(
                    message = msg,
                    isMe = isMe,
                    isStaff = isStaff,
                    onDelete = { onDeleteMessage(msg.id) }
                )
            }
        }

        // MESSAGE COMPOSER
        Surface(
            color = DeepCharcoal,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = {
                        Text(
                            text = "Message @${conversation.partner.displayName} (Enter to send, Shift+Enter newline)",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                if (!keyEvent.isShiftPressed && messageInput.isNotBlank()) {
                                    onSendMessage(messageInput.trim())
                                    messageInput = ""
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = ObsidianBlack,
                        unfocusedContainerColor = ObsidianBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            onSendMessage(messageInput.trim())
                            messageInput = ""
                        }
                    },
                    enabled = messageInput.isNotBlank(),
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (messageInput.isNotBlank()) EmeraldGreen else ObsidianBorder,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageInput.isNotBlank()) OnEmeraldDark else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ----------------- COMPONENT 3: MESSAGE BUBBLE -----------------

@Composable
fun DiscordMessageBubble(
    message: MessageDto,
    isMe: Boolean,
    isStaff: Boolean,
    onDelete: () -> Unit
) {
    val timeFormatted = remember(message.createdAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.createdAt))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isMe) 12.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 12.dp
            ),
            color = if (isMe) EmeraldGreen.copy(alpha = 0.22f) else DeepCharcoal,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isMe) EmeraldGreen.copy(alpha = 0.5f) else ObsidianBorder
            ),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header with sender and optional delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isMe) "You" else (message.senderName ?: message.senderId),
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMe) EmeraldGreen else Color(0xFF38BDF8)
                        )
                        if (!isMe && message.senderRole != null && message.senderRole != "MEMBER") {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = if (message.senderRole == "ADMIN") NetherRed else MinecraftGold
                            ) {
                                Text(
                                    text = message.senderRole,
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 7.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                    }

                    // Delete own message or staff moderate
                    if (isMe || isStaff) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Message",
                                tint = NetherRed.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message Text
                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp and read receipt
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isRead == 1) Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = if (message.isRead == 1) "Read" else "Sent",
                            tint = if (message.isRead == 1) EmeraldGreen else TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------- COMPONENT 4: OPTIONAL RIGHT DETAILS PANEL -----------------

@Composable
fun ConversationDetailsPanel(
    modifier: Modifier = Modifier,
    partner: com.example.data.ConversationPartnerDto,
    onClearHistory: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = modifier
            .background(DeepCharcoal)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("USER PROFILE", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, color = TextMuted)
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar
        Surface(
            shape = CircleShape,
            color = ObsidianBlack,
            border = androidx.compose.foundation.BorderStroke(2.dp, EmeraldGreen),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = partner.displayName.take(1).uppercase(),
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 24.sp,
                    color = EmeraldGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = partner.displayName,
            fontFamily = SilkscreenFontFamily,
            fontSize = 15.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "@${partner.id}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = EmeraldGreen
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (partner.role == "ADMIN") NetherRed.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (partner.role == "ADMIN") NetherRed else EmeraldGreen)
        ) {
            Text(
                text = partner.role,
                fontFamily = SilkscreenFontFamily,
                fontSize = 9.sp,
                color = if (partner.role == "ADMIN") NetherRed else EmeraldGreen,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = ObsidianBorder)
        Spacer(modifier = Modifier.height(14.dp))

        // Shared Clan info
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ObsidianBlack,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CLAN INFO", fontFamily = SilkscreenFontFamily, fontSize = 10.sp, color = TextMuted)
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (partner.teamName != null) {
                    Text(text = partner.teamName, fontFamily = SilkscreenFontFamily, fontSize = 12.sp, color = TextPrimary)
                    if (partner.isSharedTeam) {
                        Text(text = "Shared Clan Member", fontSize = 10.sp, color = MinecraftGold)
                    }
                } else {
                    Text(text = "No Clan Affiliation", fontSize = 11.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onClearHistory,
            colors = ButtonDefaults.buttonColors(containerColor = NetherRed.copy(alpha = 0.2f), contentColor = NetherRed),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("CLEAR CHAT", fontFamily = SilkscreenFontFamily, fontSize = 10.sp)
        }
    }
}

// ----------------- COMPONENT 5: EMPTY CHAT PLACEHOLDER -----------------

@Composable
fun DiscordEmptyChatPlaceholder(
    onStartChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = DeepCharcoal,
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Forum, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(34.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "YOUR CONVERSATIONS",
                fontFamily = SilkscreenFontFamily,
                fontSize = 16.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Select a conversation from the sidebar or click NEW to message any registered TGZ player.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.widthIn(max = 380.dp),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStartChat,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("START NEW CONVERSATION", fontFamily = SilkscreenFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------- COMPONENT 6: USER SEARCH & NEW CHAT DIALOG -----------------

@Composable
fun DiscordUserSearchDialog(
    viewModel: TgzViewModel,
    onDismiss: () -> Unit,
    onSelectUser: (String) -> Unit
) {
    var searchInput by remember { mutableStateOf("") }
    val searchResults by viewModel.userSearchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearchingUsers.collectAsStateWithLifecycle()

    LaunchedEffect(searchInput) {
        viewModel.searchUsers(searchInput.trim())
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.clearUserSearchResults()
            onDismiss()
        },
        title = {
            Text("START DIRECT MESSAGE", fontFamily = SilkscreenFontFamily, color = EmeraldGreen, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    label = { Text("Search player by Unique ID or name") },
                    placeholder = { Text("e.g. hunter_99 or sg_karthi") },
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
                            text = if (searchInput.isBlank()) "No players found." else "No active players match '$searchInput'.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(searchResults, key = { it.id }) { user ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ObsidianBlack,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.clearUserSearchResults()
                                        onSelectUser(user.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = DeepCharcoal,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = user.displayName.take(1).uppercase(),
                                                    fontFamily = SilkscreenFontFamily,
                                                    fontSize = 12.sp,
                                                    color = EmeraldGreen
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.displayName,
                                                    fontFamily = SilkscreenFontFamily,
                                                    fontSize = 12.sp,
                                                    color = TextPrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "@${user.id}",
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }
                                            if (user.teamName != null) {
                                                Text(
                                                    text = "Clan: ${user.teamName}",
                                                    fontSize = 9.sp,
                                                    color = MinecraftGold
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.clearUserSearchResults()
                                            onSelectUser(user.id)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("CHAT", fontFamily = SilkscreenFontFamily, fontSize = 9.sp)
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
                viewModel.clearUserSearchResults()
                onDismiss()
            }) {
                Text("CANCEL", color = TextMuted)
            }
        },
        containerColor = DeepCharcoal
    )
}
