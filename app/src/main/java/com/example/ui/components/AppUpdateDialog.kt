package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class UpdateUiState {
    IDLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    NEEDS_PERMISSION,
    ERROR
}

@Composable
fun AppUpdateDialog(
    updateInfo: AppVersionResponse,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isForced = updateInfo.forceUpdate || BuildConfig.VERSION_CODE < updateInfo.minSupportedVersionCode

    var state by remember { mutableStateOf(UpdateUiState.IDLE) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }

    fun triggerInstall(file: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to opening browser with direct URL if FileProvider fails
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.apkUrl.trim())).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ignored: Exception) {}
        }
    }

    fun startInAppDownload() {
        val targetUrl = updateInfo.apkUrl.trim()
        if (targetUrl.isBlank()) {
            errorMessage = "Update package URL is empty."
            state = UpdateUiState.ERROR
            return
        }

        state = UpdateUiState.DOWNLOADING
        downloadProgress = 0f
        downloadedBytes = 0L
        totalBytes = 0L
        errorMessage = null

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder()
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build()

                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "TGZ-Android/${BuildConfig.VERSION_NAME}")
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw Exception("Download failed with HTTP ${response.code}")
                    }

                    val body = response.body ?: throw Exception("Empty response body from server")
                    val rawLength = body.contentLength()
                    val expectedLength = if (rawLength > 0) rawLength else 20_424_819L
                    withContext(Dispatchers.Main) {
                        totalBytes = expectedLength
                    }

                    val apkFile = File(context.cacheDir, "TGZ-Lifestyle-v${updateInfo.versionName}.apk")
                    if (apkFile.exists()) {
                        apkFile.delete()
                    }

                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(apkFile)
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastUpdate = System.currentTimeMillis()

                    inputStream.use { input ->
                        outputStream.use { output ->
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead

                                val now = System.currentTimeMillis()
                                if (now - lastUpdate > 80 || totalRead == expectedLength) {
                                    lastUpdate = now
                                    val prog = (totalRead.toFloat() / expectedLength).coerceIn(0f, 1f)
                                    withContext(Dispatchers.Main) {
                                        downloadedBytes = totalRead
                                        downloadProgress = prog
                                    }
                                }
                            }
                            output.flush()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        downloadedApkFile = apkFile
                        downloadProgress = 1f
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                            state = UpdateUiState.NEEDS_PERMISSION
                        } else {
                            state = UpdateUiState.READY_TO_INSTALL
                            triggerInstall(apkFile)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        errorMessage = e.localizedMessage ?: "Failed to download update package."
                        state = UpdateUiState.ERROR
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isForced && state != UpdateUiState.DOWNLOADING) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForced && state != UpdateUiState.DOWNLOADING,
            dismissOnClickOutside = !isForced && state != UpdateUiState.DOWNLOADING
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
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (state) {
                                UpdateUiState.READY_TO_INSTALL -> EmeraldGreen.copy(alpha = 0.2f)
                                UpdateUiState.NEEDS_PERMISSION -> MinecraftGold.copy(alpha = 0.2f)
                                UpdateUiState.ERROR -> NetherRedBright.copy(alpha = 0.2f)
                                else -> EmeraldGreen.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            1.dp,
                            when (state) {
                                UpdateUiState.READY_TO_INSTALL -> EmeraldGreen
                                UpdateUiState.NEEDS_PERMISSION -> MinecraftGold
                                UpdateUiState.ERROR -> NetherRedBright
                                else -> EmeraldGreen.copy(alpha = 0.4f)
                            },
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (state) {
                            UpdateUiState.READY_TO_INSTALL -> Icons.Default.CheckCircle
                            UpdateUiState.NEEDS_PERMISSION -> Icons.Default.Security
                            UpdateUiState.ERROR -> Icons.Default.Error
                            else -> Icons.Default.SystemUpdate
                        },
                        contentDescription = "Update",
                        tint = when (state) {
                            UpdateUiState.READY_TO_INSTALL -> EmeraldGreen
                            UpdateUiState.NEEDS_PERMISSION -> MinecraftGold
                            UpdateUiState.ERROR -> NetherRedBright
                            else -> EmeraldGreen
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when (state) {
                        UpdateUiState.DOWNLOADING -> "DOWNLOADING UPDATE"
                        UpdateUiState.READY_TO_INSTALL -> "READY TO INSTALL"
                        UpdateUiState.NEEDS_PERMISSION -> "PERMISSION REQUIRED"
                        UpdateUiState.ERROR -> "DOWNLOAD FAILED"
                        else -> "UPDATE AVAILABLE"
                    },
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 15.sp,
                    color = when (state) {
                        UpdateUiState.NEEDS_PERMISSION -> MinecraftGold
                        UpdateUiState.ERROR -> NetherRedBright
                        else -> EmeraldGreen
                    },
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

                // Changelog only in IDLE state to keep UI focused
                if (state == UpdateUiState.IDLE && updateInfo.changelog.isNotBlank()) {
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
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // DOWNLOADING STATE VIEW
                if (state == UpdateUiState.DOWNLOADING) {
                    val downloadedMb = downloadedBytes.toFloat() / (1024 * 1024)
                    val totalMb = totalBytes.toFloat() / (1024 * 1024)
                    val percent = (downloadProgress * 100).toInt()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldGreen,
                            trackColor = ObsidianBlack
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$percent%",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f MB / %.1f MB", downloadedMb, totalMb),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Downloading update package directly in app...",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // NEEDS PERMISSION VIEW
                if (state == UpdateUiState.NEEDS_PERMISSION) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MinecraftGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        color = ObsidianBlack
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "INSTALL PERMISSION NEEDED",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 10.sp,
                                color = MinecraftGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Android requires permission to install updates directly. Please tap below to enable 'Allow from this source', then return to complete the update.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ERROR VIEW
                if (state == UpdateUiState.ERROR) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, NetherRedBright.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        color = ObsidianBlack
                    ) {
                        Text(
                            text = errorMessage ?: "An unexpected error occurred during download.",
                            fontSize = 11.sp,
                            color = NetherRedBright,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Action Buttons based on state
                when (state) {
                    UpdateUiState.IDLE -> {
                        Button(
                            onClick = { startInAppDownload() },
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
                                text = "UPDATE VIA APP",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 12.sp,
                                color = OnEmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    UpdateUiState.DOWNLOADING -> {
                        // Button is hidden or shows disabled state while actively streaming
                    }

                    UpdateUiState.READY_TO_INSTALL -> {
                        Button(
                            onClick = {
                                downloadedApkFile?.let { triggerInstall(it) } ?: startInAppDownload()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = OnEmeraldDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INSTALL UPDATE NOW",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 12.sp,
                                color = OnEmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    UpdateUiState.NEEDS_PERMISSION -> {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MinecraftGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = DeepCharcoal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GRANT INSTALL PERMISSION",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 11.sp,
                                color = DeepCharcoal,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                downloadedApkFile?.let { triggerInstall(it) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ALREADY GRANTED? TAP TO INSTALL",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 10.sp,
                                color = OnEmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    UpdateUiState.ERROR -> {
                        Button(
                            onClick = { startInAppDownload() },
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
                                text = "RETRY DOWNLOAD",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 12.sp,
                                color = OnEmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Fallback direct browser download button
                if (state != UpdateUiState.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            val url = updateInfo.apkUrl.trim()
                            if (url.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Download via Browser",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                if (!isForced && state != UpdateUiState.DOWNLOADING) {
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
