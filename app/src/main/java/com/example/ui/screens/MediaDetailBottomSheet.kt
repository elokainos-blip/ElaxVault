package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MediaDetailBottomSheet(
    item: MediaItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onMoveToAlbum: (String) -> Unit,
    availableAlbums: List<String>,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderSlate) },
        modifier = modifier.testTag("media_detail_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- 1. Large Image Header Frame ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    .background(BorderSlate)
            ) {
                val isAudio = item.mediaType.startsWith("audio")
                if (isAudio) {
                    var isPlaying by remember { mutableStateOf(false) }
                    val infiniteTransition = rememberInfiniteTransition(label = "audio_bars_detail")
                    val barAnimations = List(18) { index ->
                        infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = if (isPlaying) 1f else 0.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 400 + (index * 45) % 400,
                                    easing = FastOutSlowInEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bar_detail_$index"
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        PrimaryCyan.copy(alpha = 0.22f),
                                        BackgroundDark
                                    )
                                )
                            )
                            .testTag("audio_player_container_detail"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Audio Waveform element
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .padding(horizontal = 24.dp)
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 28.dp)
                                .testTag("audio_waveform_detail"),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            barAnimations.forEachIndexed { index, animState ->
                                val heightFract = if (isPlaying) animState.value else {
                                    when (index % 6) {
                                        0 -> 0.35f
                                        1 -> 0.7f
                                        2 -> 0.9f
                                        3 -> 0.55f
                                        4 -> 0.25f
                                        else -> 0.45f
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(heightFract)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (isPlaying) PrimaryCyan
                                            else Color.Gray.copy(alpha = 0.45f)
                                        )
                                )
                            }
                        }

                        // Centered Play/Pause Button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(PrimaryCyan)
                                .clickable { isPlaying = !isPlaying }
                                .testTag("audio_play_pause_button_detail"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = BackgroundDark,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                } else {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Shading bottom gradient to make overlays readable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                                startY = 300f
                            )
                        )
                )

                // Status chip positioned top right
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                ) {
                    val statusText = when (item.syncStatus) {
                        "SYNCED" -> "Cloud Saved"
                        "SYNCING" -> "Uploading..."
                        "FAILED" -> "Upload Error"
                        else -> "Local Vault Only"
                    }
                    val containerColor = when (item.syncStatus) {
                        "SYNCED" -> AccentGreen.copy(alpha = 0.2f)
                        "SYNCING" -> PrimaryCyan.copy(alpha = 0.2f)
                        "FAILED" -> Color.Red.copy(alpha = 0.2f)
                        else -> SoftGray.copy(alpha = 0.2f)
                    }
                    val textColor = when (item.syncStatus) {
                        "SYNCED" -> AccentGreen
                        "SYNCING" -> PrimaryCyan
                        "FAILED" -> Color.Red
                        else -> TextSecondary
                    }

                    Text(
                        text = statusText,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .background(containerColor, RoundedCornerShape(20.dp))
                            .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 2. Inferred smart info: Title & Album Folder ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    var showDropdown by remember { mutableStateOf(false) }

                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showDropdown = true }
                                .background(PrimaryCyan.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("change_album_trigger")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = PrimaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.primaryAlbum,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select album",
                                tint = PrimaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                        ) {
                            availableAlbums.toSet().forEach { albumName ->
                                DropdownMenuItem(
                                    text = { Text(albumName, color = TextPrimary) },
                                    onClick = {
                                        showDropdown = false
                                        onMoveToAlbum(albumName)
                                    },
                                    modifier = Modifier.testTag("album_select_option_${albumName.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // Delete FAB Button
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Red.copy(alpha = 0.15f),
                        contentColor = Color.Red
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_media_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete from vault",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderSlate, modifier = Modifier.padding(vertical = 14.dp))

            // --- 3. AI Smart summary (Gemini Caption) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Cognitive Caption",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. Location Context ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Inferred Location",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 5. Semantic Tags Chip List ---
            Text(
                text = "Semantic Tags",
                style = MaterialTheme.typography.bodySmall,
                color = SoftGray,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val tagsList = item.getTagsList()
            if (tagsList.isEmpty()) {
                Text(
                    text = "No categorizable tags harvested",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tagsList.forEach { tagText ->
                        Text(
                            text = "#$tagText",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(AccentGreen.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .border(1.dp, AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSlate, modifier = Modifier.padding(vertical = 14.dp))

            // --- 6. Technical properties list ---
            Text(
                text = "Technical Specifications",
                style = MaterialTheme.typography.bodySmall,
                color = SoftGray,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Properties list
            val formatTime = SimpleDateFormat("MMM dd, yyyy  •  HH:mm:ss", Locale.ROOT).format(Date(item.timestamp))
            val formatSize = when {
                item.size > 1024 * 1024 -> String.format(Locale.US, "%.2f MB", item.size / (1024.0 * 1024.0))
                item.size > 1024 -> String.format(Locale.US, "%.1f KB", item.size / 1024.0)
                else -> "${item.size} Bytes"
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Filename", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(item.displayName, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Import Date", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(formatTime, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("File Size", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(formatSize, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mime-Type", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(item.mediaType, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }

            // Cloud sync path if uploaded
            if (item.cloudUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Secure Cloud Endpoint", style = MaterialTheme.typography.labelSmall, color = PrimaryCyan, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.cloudUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftGray,
                            fontFamily = FontFamily.Monospace,
                            textDecoration = TextDecoration.Underline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// Simple legacy helper flow layout box to chain tag chips gracefully
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
