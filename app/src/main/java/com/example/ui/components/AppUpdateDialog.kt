package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.data.AppVersionResponse
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRedBright
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun AppUpdateDialog(
    updateInfo: AppVersionResponse,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isForced = updateInfo.forceUpdate || BuildConfig.VERSION_CODE < updateInfo.minSupportedVersionCode

    Dialog(
        onDismissRequest = {
            if (!isForced) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForced,
            dismissOnClickOutside = !isForced
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, EmeraldGreen, RoundedCornerShape(16.dp)),
            color = DeepCharcoal
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldGreen.copy(alpha = 0.15f))
                        .border(1.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "UPDATE AVAILABLE",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 16.sp,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "v${BuildConfig.VERSION_NAME} → v${updateInfo.versionName}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = MinecraftGold,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (updateInfo.changelog.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp)),
                        color = ObsidianBlack
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "WHAT'S NEW",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = updateInfo.changelog,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isForced) {
                    Text(
                        text = "This update is required to continue using TGZ Lifestyle SMP.",
                        fontSize = 11.sp,
                        color = NetherRedBright,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        val url = updateInfo.apkUrl.trim()
                        if (url.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = OnEmeraldDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UPDATE NOW",
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 12.sp,
                        color = OnEmeraldDark,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isForced) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "LATER",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
