package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MediaViewModel
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val allMedia by viewModel.allMedia.collectAsState()
    val isAutoSync by viewModel.autoSyncEnabled.collectAsState()
    val isWifiOnly by viewModel.wifiOnlyEnabled.collectAsState()
    val isHighRes by viewModel.highResBackups.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Space / Storage statistics calculations
    val totalCount = allMedia.size
    val imageCount = allMedia.count { it.mediaType.startsWith("image") }
    val videoCount = allMedia.count { it.mediaType.startsWith("video") }
    val audioCount = allMedia.count { it.mediaType.startsWith("audio") }
    val totalBytes = allMedia.sumOf { it.size }
    
    val formattedStorageSize = when {
        totalBytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", totalBytes.toFloat() / (1024 * 1024))
        totalBytes >= 1024 -> String.format(Locale.getDefault(), "%.2f KB", totalBytes.toFloat() / 1024)
        else -> "$totalBytes Bytes"
    }

    // Default limit simulation: 15 MB max local database storage simulation
    val storageLimitBytes = 15L * 1024L * 1024L
    val storageUsageFraction = if (totalBytes == 0L) 0f else (totalBytes.toFloat() / storageLimitBytes).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("settings_screen_container")
    ) {
        // --- Header Banner ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PrimaryCyan.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings logo",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "System preferences",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vault & Cloud Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Storage Statistics Card ---
        Text(
            text = "STORAGE RESOURCE MANAGEMENT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SoftGray,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_storage_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vault Occupied Space",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = formattedStorageSize,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { storageUsageFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = PrimaryCyan,
                    trackColor = BorderSlate
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0 B",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "15.0 MB Allocated Capacity",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSlate)

                // Sub-Breakdowns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = SoftGray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Images", style = MaterialTheme.typography.labelSmall, color = SoftGray)
                        Text(text = "$imageCount items", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = SoftGray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Videos", style = MaterialTheme.typography.labelSmall, color = SoftGray)
                        Text(text = "$videoCount items", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = SoftGray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Audios", style = MaterialTheme.typography.labelSmall, color = SoftGray)
                        Text(text = "$audioCount items", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Synchronization Settings Card ---
        Text(
            text = "CLOUDSYNC PREFERENCES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SoftGray,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Auto sync option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automated Cloud Backup",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Background queue analyzes and auto-syncs newly imported media.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isAutoSync,
                        onCheckedChange = { viewModel.toggleAutoSync(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = PrimaryCyan,
                            uncheckedThumbColor = SoftGray,
                            uncheckedTrackColor = BorderSlate
                        ),
                        modifier = Modifier.testTag("switch_auto_sync")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSlate)

                // Wifi only option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wi-Fi Only Syncing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Preserve cellular bandwidth; only transfer files when on Wi-Fi connection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isWifiOnly,
                        onCheckedChange = { viewModel.toggleWifiOnly(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = PrimaryCyan,
                            uncheckedThumbColor = SoftGray,
                            uncheckedTrackColor = BorderSlate
                        ),
                        modifier = Modifier.testTag("switch_wifi_only")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSlate)

                // High Quality backups option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lossless Media Upload",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Store original resolution assets instead of compressed web previews.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isHighRes,
                        onCheckedChange = { viewModel.toggleHighRes(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = PrimaryCyan,
                            uncheckedThumbColor = SoftGray,
                            uncheckedTrackColor = BorderSlate
                        ),
                        modifier = Modifier.testTag("switch_high_res")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Demo Data / Onboarding Assistance Card ---
        Text(
            text = "ONBOARDING ASSIST",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SoftGray,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Elax Workspace Onboarding",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Simulate clean user vault items including voice recordings, software blueprints, forest captures, and feline kittens instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = { viewModel.loadPresetDemoContent() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("settings_load_presets_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCyan.copy(alpha = 0.15f),
                        contentColor = PrimaryCyan
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load Safe Workspace Presets", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Danger / Destructive Actions Zone ---
        Text(
            text = "DANGER ZONE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AccentOrange,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Delete Local Vault Cache",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Danger: This will erase all local database records, tags, custom albums, and imported assets permanently. This operation cannot be reversed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("settings_clear_vault_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4159).copy(alpha = 0.15f),
                        contentColor = Color(0xFFFF5266)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Destructively Wipe Vault", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // About Block
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Elax Media Vault • Build v2.4.10a\nLocal Sandbox Environment Framework",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))
    }

    // Confirmation dialog before clear-all destructive action
    if (showClearConfirmDialog) {
        Dialog(onDismissRequest = { showClearConfirmDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("wipe_confirm_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5266).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning sign",
                            tint = Color(0xFFFF5266),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Are you absolutely sure?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This erases all imported local databases, custom tags, smart categories, and audio-image files. There is no backup system to restore them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showClearConfirmDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("wipe_cancel_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SoftGray
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                        ) {
                            Text("Keep Files")
                        }

                        Button(
                            onClick = {
                                viewModel.clearAllData()
                                showClearConfirmDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("wipe_execute_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF4159),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Wipe Vault")
                        }
                    }
                }
            }
        }
    }
}
