package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MediaViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyncScreen(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val allMedia by viewModel.allMedia.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val isAutoSync by viewModel.autoSyncEnabled.collectAsState()
    val isWifiOnly by viewModel.wifiOnlyEnabled.collectAsState()
    val isHighRes by viewModel.highResBackups.collectAsState()
    val isSyncing by viewModel.syncingState.collectAsState()

    // Dynamically calculate storage states based on imported database elements
    val totalBackupBytes = allMedia.filter { it.syncStatus == "SYNCED" }.sumOf { it.size }
    val pendingCount = allMedia.count { it.syncStatus == "PENDING" }
    
    // Limits of mockup cloud server: 100MB
    val cloudLimitBytes = 100L * 1024L * 1024L
    val usedPercentage = if (totalBackupBytes == 0L) 0f else (totalBackupBytes.toFloat() / cloudLimitBytes).coerceAtMost(1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // --- 1. Cloud Storage Capacity Progress ---
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = PrimaryCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Cloud Encryption Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "256-bit AES",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = AccentGreen,
                        modifier = Modifier
                            .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Capacity: ${formatBytes(totalBackupBytes)} used",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Limit: ${formatBytes(cloudLimitBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                LinearProgressIndicator(
                    progress = { usedPercentage },
                    color = if (usedPercentage > 0.85f) AccentOrange else PrimaryCyan,
                    trackColor = BorderSlate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Automated sync backs up files directly into securely fragmented distributed servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Live Backup Sync Trigger Circle ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Sync Status",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isSyncing) "Uploading to server..." else if (pendingCount > 0) "$pendingCount file(s) waiting for sync" else "All cloud streams synced!",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSyncing) PrimaryCyan else if (pendingCount > 0) AccentOrange else AccentGreen
                    )
                }

                // Spits rotating animation trigger
                val infiniteTransition = rememberInfiniteTransition(label = "rotate_sync")
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotate_sync_anim"
                )

                IconButton(
                    onClick = { viewModel.triggerCloudSync() },
                    enabled = !isSyncing,
                    modifier = Modifier
                        .size(54.dp)
                        .background(if (isSyncing) BorderSlate else MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .testTag("force_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync now",
                        tint = if (isSyncing) SoftGray else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(if (isSyncing) rotationAngle else 0f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Settings Toggles ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                // Auto Sync
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CloudQueue, contentDescription = null, tint = SoftGray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Automatic Sync", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Sync media when network is detected", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
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
                        modifier = Modifier.testTag("auto_sync_toggle")
                    )
                }

                HorizontalDivider(color = BorderSlate, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))

                // Wi-Fi Only Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = SoftGray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Wi-Fi Connection Only", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Restricts uploads on mobile data", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
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
                        modifier = Modifier.testTag("wifi_only_toggle")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. Simulated Server Activities Terminal Console ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Backup Logging Console",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            TextButton(onClick = { viewModel.clearAllData() }) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear All", style = MaterialTheme.typography.bodySmall, color = AccentOrange)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(12.dp))
                .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (recentLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$ guest_root: awaiting server connection...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = SoftGray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("sync_logs_terminal"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true // lists newest on bottom which feels like a scrolling terminal!
                ) {
                    items(recentLogs, key = { it.id }) { log ->
                        TerminalLogLine(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalLogLine(log: SyncLog) {
    val timeString = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT).format(Date(log.timestamp))
    
    val color = when (log.type) {
        "SUCCESS" -> AccentGreen
        "WARNING" -> AccentOrange
        "ERROR" -> Color.Red
        else -> TextPrimary
    }

    val typePrefix = when (log.type) {
        "SUCCESS" -> "[OK] "
        "WARNING" -> "[WARN]"
        "ERROR" -> "[ERR] "
        else -> "[INFO]"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$timeString  ",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = SoftGray
        )
        Text(
            text = "$typePrefix  ",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = log.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = color,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0.0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}
