package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ObsidianCard
import com.example.ui.components.VoxelHeartIcon
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.TgzViewModel

@Composable
fun LoginScreen(
    viewModel: TgzViewModel,
    authState: AuthUiState,
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var uniqueId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var customServerInput by remember { mutableStateOf(viewModel.getBackendUrl()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                VoxelHeartIcon(iconSize = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "TGZ AUTH",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 24.sp,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sign in to access your dashboard, clan teams & playtime crates",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            // Server Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Server: ${viewModel.getBackendUrl()}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "[Change]",
                    color = EmeraldGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        customServerInput = viewModel.getBackendUrl()
                        showServerDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            ObsidianCard(
                borderColor = EmeraldGreen.copy(alpha = 0.4f),
                accentGlow = EmeraldGreen
            ) {
                // Unique ID Field
                OutlinedTextField(
                    value = uniqueId,
                    onValueChange = { uniqueId = it.lowercase() },
                    label = { Text("Unique ID") },
                    placeholder = { Text("e.g. sg_karthi or hunter_99") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldGreen)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = TextMuted,
                        focusedContainerColor = DeepCharcoal,
                        unfocusedContainerColor = DeepCharcoal
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldGreen)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                                tint = TextMuted
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = TextMuted,
                        focusedContainerColor = DeepCharcoal,
                        unfocusedContainerColor = DeepCharcoal
                    )
                )

                // Error / Success Message
                if (authState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NetherRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = authState.errorMessage,
                            color = NetherRed,
                            fontSize = 12.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (uniqueId.isNotBlank() && password.isNotBlank()) {
                            viewModel.login(uniqueId, password, onSuccess = onLoginSuccess)
                        }
                    },
                    enabled = !authState.isLoading && uniqueId.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = OnEmeraldDark,
                        disabledContainerColor = EmeraldGreen.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (authState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnEmeraldDark, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "LOGIN",
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Switch to Signup
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Don't have an account?", color = TextMuted, fontSize = 13.sp)
                TextButton(onClick = onNavigateToSignup) {
                    Text(
                        text = "REGISTER AS MEMBER",
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (showServerDialog) {
            AlertDialog(
                onDismissRequest = { showServerDialog = false },
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
                            value = customServerInput,
                            onValueChange = { customServerInput = it },
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { customServerInput = "http://10.70.99.78:3000/" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Wi-Fi (PC IP)", fontSize = 11.sp, color = EmeraldGreen)
                            }
                            Button(
                                onClick = { customServerInput = "http://10.0.2.2:3000/" },
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
                            val trimmed = customServerInput.trim()
                            if (trimmed.isNotBlank()) {
                                viewModel.setBackendUrl(trimmed)
                            }
                            showServerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark)
                    ) {
                        Text("Save & Connect")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showServerDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = ObsidianBlack
            )
        }
    }
}

@Composable
fun SignupScreen(
    viewModel: TgzViewModel,
    authState: AuthUiState,
    onNavigateToLogin: () -> Unit,
    onSignupSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var uniqueId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                VoxelHeartIcon(iconSize = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "MEMBER SIGNUP",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 22.sp,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Register a new TGZ Member account with a Unique ID",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            ObsidianCard(
                borderColor = EmeraldGreen.copy(alpha = 0.4f),
                accentGlow = EmeraldGreen
            ) {
                // Unique ID Field
                OutlinedTextField(
                    value = uniqueId,
                    onValueChange = { uniqueId = it.lowercase() },
                    label = { Text("Unique ID *") },
                    placeholder = { Text("Letters, numbers & underscores") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldGreen)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = TextMuted,
                        focusedContainerColor = DeepCharcoal,
                        unfocusedContainerColor = DeepCharcoal
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Display Name
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. Hunter Champion") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = TextMuted,
                        focusedContainerColor = DeepCharcoal,
                        unfocusedContainerColor = DeepCharcoal
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min 8 chars) *") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldGreen)
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = TextMuted,
                        focusedContainerColor = DeepCharcoal,
                        unfocusedContainerColor = DeepCharcoal
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password *") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldGreen)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                                tint = TextMuted
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = TextMuted,
                        focusedContainerColor = DeepCharcoal,
                        unfocusedContainerColor = DeepCharcoal
                    )
                )

                // Error Message
                if (authState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NetherRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = authState.errorMessage,
                            color = NetherRed,
                            fontSize = 12.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = {
                        viewModel.signup(
                            id = uniqueId,
                            displayName = displayName.ifBlank { uniqueId },
                            pass = password,
                            confirmPass = confirmPassword,
                            onSuccess = onSignupSuccess
                        )
                    },
                    enabled = !authState.isLoading && uniqueId.isNotBlank() && password.length >= 8 && password == confirmPassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = OnEmeraldDark,
                        disabledContainerColor = EmeraldGreen.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (authState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnEmeraldDark, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "CREATE MEMBER ACCOUNT",
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Already registered?", color = TextMuted, fontSize = 13.sp)
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "SIGN IN",
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ForcedChangePasswordDialog(
    viewModel: TgzViewModel,
    authState: AuthUiState
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { /* Cannot dismiss without changing */ },
        title = {
            Text(
                text = "CHANGE BOOTSTRAP PASSWORD",
                fontFamily = SilkscreenFontFamily,
                color = EmeraldGreen,
                fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Security Requirement: You must change the initial server bootstrap password before continuing as Administrator.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Permanent Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                if (authState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authState.errorMessage,
                        color = NetherRed,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.changePassword(null, newPassword, confirmPassword)
                },
                enabled = newPassword.length >= 8 && newPassword == confirmPassword,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = OnEmeraldDark)
            ) {
                Text("SAVE NEW PASSWORD", fontFamily = SilkscreenFontFamily, fontSize = 12.sp)
            }
        },
        containerColor = DeepCharcoal
    )
}
