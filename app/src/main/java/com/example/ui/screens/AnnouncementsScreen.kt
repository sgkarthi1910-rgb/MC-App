package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.data.AnnouncementDto
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
fun AnnouncementsScreen(
    viewModel: TgzViewModel,
    modifier: Modifier = Modifier
) {
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val isLoading by viewModel.isAnnouncementsLoading.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedAnnouncement by remember { mutableStateOf<AnnouncementDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAnnouncements()
    }

    val isStaff = authState.role == "ADMIN" || authState.role == "MODERATOR"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        if (selectedAnnouncement != null) {
            // DETAIL VIEW
            AnnouncementDetailView(
                announcement = selectedAnnouncement!!,
                canDelete = isStaff,
                onBack = { selectedAnnouncement = null },
                onDelete = {
                    viewModel.deleteAnnouncement(selectedAnnouncement!!.id)
                    selectedAnnouncement = null
                }
            )
        } else {
            // LIST VIEW
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ANNOUNCEMENTS",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 18.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isStaff) {
                                Button(
                                    onClick = { showCreateDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PUBLISH", fontFamily = SilkscreenFontFamily, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            IconButton(onClick = { viewModel.loadAnnouncements() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EmeraldGreen)
                            }
                        }
                    }
                }

                if (isLoading && announcements.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = EmeraldGreen)
                        }
                    }
                } else if (announcements.isEmpty()) {
                    item {
                        ObsidianCard {
                            Text(
                                text = "No announcements published yet.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(announcements, key = { it.id }) { ann ->
                        AnnouncementItemCard(
                            announcement = ann,
                            onClick = { selectedAnnouncement = ann }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateAnnouncementDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, content, category, isPinned ->
                viewModel.createAnnouncement(title, content, category, isPinned) {
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
fun AnnouncementItemCard(
    announcement: AnnouncementDto,
    onClick: () -> Unit
) {
    val dateFormatted = remember(announcement.createdAt) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(announcement.createdAt))
    }

    ObsidianCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        borderColor = if (announcement.isPinned == 1) MinecraftGold.copy(alpha = 0.5f) else ObsidianBorder,
        accentGlow = if (announcement.isPinned == 1) MinecraftGold else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (announcement.isPinned == 1) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MinecraftGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (announcement.category) {
                        "UPDATE" -> EmeraldGreen.copy(alpha = 0.2f)
                        "EVENT" -> MinecraftGold.copy(alpha = 0.2f)
                        else -> DeepCharcoal
                    }
                ) {
                    Text(
                        text = announcement.category,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = when (announcement.category) {
                            "UPDATE" -> EmeraldGreen
                            "EVENT" -> MinecraftGold
                            else -> TextSecondary
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (announcement.isRead == 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = CircleShape, color = NetherRed, modifier = Modifier.size(8.dp)) {}
                }
            }

            Text(
                text = dateFormatted,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = announcement.title,
            fontFamily = SilkscreenFontFamily,
            fontSize = 15.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = announcement.content,
            fontSize = 13.sp,
            color = TextSecondary,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Posted by ${announcement.authorName}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
fun AnnouncementDetailView(
    announcement: AnnouncementDto,
    canDelete: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatted = remember(announcement.createdAt) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(announcement.createdAt))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ALL ANNOUNCEMENTS", fontFamily = SilkscreenFontFamily, fontSize = 12.sp, color = EmeraldGreen)
            }

            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NetherRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        ObsidianCard(
            borderColor = if (announcement.isPinned == 1) MinecraftGold else EmeraldGreen.copy(alpha = 0.4f),
            accentGlow = if (announcement.isPinned == 1) MinecraftGold else EmeraldGreen
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (announcement.isPinned == 1) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MinecraftGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = "[${announcement.category}]",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = EmeraldGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = announcement.title,
                fontFamily = SilkscreenFontFamily,
                fontSize = 19.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Published by ${announcement.authorName} • $dateFormatted",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = announcement.content,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun CreateAnnouncementDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, content: String, category: String, isPinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("GENERAL") }
    var isPinned by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("PUBLISH ANNOUNCEMENT", fontFamily = SilkscreenFontFamily, color = EmeraldGreen, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it.uppercase() },
                    label = { Text("Category (UPDATE, EVENT, GENERAL, STORE)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors = CheckboxDefaults.colors(checkedColor = MinecraftGold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pin to top of list & member dashboard", fontSize = 12.sp, color = TextSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, content, category, isPinned) },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark)
            ) {
                Text("PUBLISH", fontFamily = SilkscreenFontFamily, fontSize = 12.sp)
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
