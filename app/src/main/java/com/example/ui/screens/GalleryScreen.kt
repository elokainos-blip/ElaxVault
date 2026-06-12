package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MediaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredMedia by viewModel.filteredMedia.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterMediaType.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()

    // Activity launcher to trigger the Android PhotoPicker (safe, permission-less picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Resolve file name and size details from contentResolver
            var fileName = "ImportedImage_${System.currentTimeMillis()}.jpg"
            var fileSize = 1500L
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
            viewModel.importLocalFile(uri, fileName, fileSize, mimeType)
            // Auto start the syncing process immediately after selection!
            viewModel.triggerCloudSync()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Album Filter Alert Bar (if viewing a specific album) ---
        AnimatedVisibility(
            visible = selectedAlbum != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Album filter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Viewing Album: ${selectedAlbum ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { viewModel.selectAlbum(null) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear album filter",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // --- 2. Advanced Search Controls ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search icon",
                    tint = SoftGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Search vault, tags, locations...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftGray
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* empty */ }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("gallery_search_input")
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.setSearchQuery("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Clear search",
                            tint = SoftGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // --- Custom Image & Video Upload Component ---
        GalleryUploadComponent(
            viewModel = viewModel,
            onSelectMedia = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            }
        )

        // --- 3. Filter Category Chips (All, Images, Videos, Audio) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("ALL" to "All Media", "IMAGE" to "Images", "VIDEO" to "Videos", "AUDIO" to "Audio").forEach { (key, label) ->
                val isSelected = filterType == key
                SuggestionChip(
                    onClick = { viewModel.setFilterMediaType(key) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        labelColor = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        enabled = true
                    ),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("filter_chip_$key")
                )
            }
        }

        // --- 4. Main Gallery Grid Content Area ---
        if (filteredMedia.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Determine if database overall is empty, or search yields 0 items
                if (viewModel.allMedia.collectAsState().value.isEmpty()) {
                    // Visual Onboarding Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(BorderSlate, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = "Import media",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Add Pictures to Your Vault",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upload photos from your Android library or tap below to inject sample high-quality files with instant AI-driven organization.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.loadPresetDemoContent() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("load_presets_button")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Populate with AI Demo Presets", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick File from Device", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // No items matched Search Query
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No results",
                            tint = SoftGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching files found", color = TextPrimary)
                        Text("Try searching another tag or location", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            // Renders standard grid of Media items
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("gallery_items_grid")
            ) {
                items(filteredMedia, key = { it.id }) { item ->
                    GalleryGridItem(
                        item = item,
                        onClick = { viewModel.focusMediaItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryGridItem(
    item: MediaItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
            .background(BorderSlate)
            .clickable(onClick = onClick)
    ) {
        val isAudio = item.mediaType.startsWith("audio")

        if (isAudio) {
            // Elegant placeholder pattern for Audio files
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(BackgroundDark, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Audio track",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Thumbnail Image
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
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        startY = 150f
                    )
                )
        )

        // Indication overlay for video type
        if (item.mediaType.startsWith("video")) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(20.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video file",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Beautiful Sync Indicator top right overlay
        Box(
            modifier = Modifier
                .padding(6.dp)
                .align(Alignment.TopEnd)
        ) {
            when (item.syncStatus) {
                "PENDING" -> {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, SoftGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Pending backup",
                            tint = SoftGray,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
                "SYNCING" -> {
                    // Display rotating cyan sync dot animation
                    ProgressDot()
                }
                "SYNCED" -> {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(AccentGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Synced to cloud",
                            tint = BackgroundDark,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                "FAILED" -> {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(AccentOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Backup failed",
                            tint = Color.White,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }
        }

        // Subtext labeling title
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .align(Alignment.BottomStart)
        )
    }
}

@Composable
fun ProgressDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_anim"
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .background(PrimaryCyan.copy(alpha = scale), CircleShape)
            .border(1.1.dp, PrimaryCyan, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(PrimaryCyan, CircleShape)
        )
    }
}

@Composable
fun GalleryUploadComponent(
    viewModel: MediaViewModel,
    onSelectMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allMedia by viewModel.allMedia.collectAsState()
    val isSyncing by viewModel.syncingState.collectAsState()
    
    val pendingCount = allMedia.count { it.syncStatus == "PENDING" }
    
    // Animate background pulse outline color if syncing is in progress
    val infiniteTransition = rememberInfiniteTransition(label = "border_glow")
    val borderGlowColor by infiniteTransition.animateColor(
        initialValue = BorderSlate,
        targetValue = if (isSyncing) PrimaryCyan else BorderSlate,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_anim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .testTag("gallery_upload_dropzone"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSyncing) 1.5.dp else 1.dp,
            color = borderGlowColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectMedia() }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload secure media",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Secure Ingest Hub",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Backup Photos & Videos directly to Vault",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftGray,
                            fontSize = 11.sp
                        )
                    }
                }
                
                // Pulsing dot indicator
                if (isSyncing) {
                    Box(
                        modifier = Modifier
                            .background(PrimaryCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(PrimaryCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ENCRYPTING",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                    }
                } else if (pendingCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$pendingCount QUEUED",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentOrange,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(AccentGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SECURE",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dashed Area / Tap target area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSyncing) PrimaryCyan.copy(alpha = 0.5f) else BorderSlate,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 20.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Select media button",
                        tint = if (isSyncing) PrimaryCyan else SoftGray,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap here to select Images or Videos",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Real-time AI cataloging & tags generated instantly on selection",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                    )
                }
            }

            // Sync indicators / dynamic tracking footer inside the component itself
            if (isSyncing || pendingCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isSyncing) "Securing storage stream..." else "Ready to trigger cloud bank",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSyncing) PrimaryCyan else SoftGray,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (pendingCount > 0 && !isSyncing) {
                        TextButton(
                            onClick = { viewModel.triggerCloudSync() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("upload_sync_now_btn")
                        ) {
                            Text(
                                "Start Syncing Now",
                                color = PrimaryCyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Continuous progress animator
                val progressAnim = remember { Animatable(0f) }
                LaunchedEffect(isSyncing) {
                    if (isSyncing) {
                        progressAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1400, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )
                    } else {
                        progressAnim.snapTo(0f)
                    }
                }
                
                LinearProgressIndicator(
                    progress = { if (isSyncing) progressAnim.value else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = PrimaryCyan,
                    trackColor = BorderSlate
                )
            }
        }
    }
}
