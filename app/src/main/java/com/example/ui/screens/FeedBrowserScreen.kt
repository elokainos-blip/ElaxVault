package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MediaViewModel
import java.util.*

// Representation of a high-fidelity Feed Album with modern visual attributes
data class FeedAlbum(
    val category: String, // FAMILY, TRIPS, DOCUMENTS, WORK
    val title: String,
    val description: String,
    val presetImages: List<FeedImage>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

data class FeedImage(
    val url: String,
    val fallbackText: String,
    val ocrBlocks: List<OcrBlock>
)

data class OcrBlock(
    val text: String,
    val label: String,
    val topPercent: Float,
    val leftPercent: Float,
    val widthPercent: Float,
    val heightPercent: Float
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedBrowserScreen(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allMedia by viewModel.allMedia.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    // 1. Core Filter State
    val filterPills = listOf("ALL", "FAMILY", "TRIPS", "DOCUMENTS", "WORK", "AUDIO")
    var selectedFilter by remember { mutableStateOf("ALL") }

    // 2. Active OCR State (for Text Capture overlay dialog)
    var activeOcrImage by remember { mutableStateOf<FeedImage?>(null) }
    var activeOcrTitle by remember { mutableStateOf("") }
    var showOcrOverlay by remember { mutableStateOf(false) }

    // 3. Static High-Quality Feed Data corresponding to required categories
    val feedAlbums = remember {
        listOf(
            FeedAlbum(
                category = "FAMILY",
                title = "Home & Cozy Moments",
                description = "Cherished snapshots with the ones who make life beautiful. Capturing our favorite furry companion's cozy afternoon routines and playful backyard picnics.",
                icon = Icons.Default.Favorite,
                presetImages = listOf(
                    FeedImage(
                        url = "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800",
                        fallbackText = "Calico Kitten enjoying morning rays",
                        ocrBlocks = listOf(
                            OcrBlock("ADOPTION CENTER", "Welcome Sign", 12f, 25f, 50f, 10f),
                            OcrBlock("PLEASE ADOPT ME! MEOW MEOW", "Kitten Cap", 45f, 15f, 70f, 14f),
                            OcrBlock("FELINE PROTECTION SOC.", "Stamp Seal", 80f, 20f, 60f, 8f)
                        )
                    ),
                    FeedImage(
                        url = "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=800",
                        fallbackText = "Playful Golden Retriever in park",
                        ocrBlocks = listOf(
                            OcrBlock("CHAMPION PET", "Badge", 15f, 35f, 30f, 8f),
                            OcrBlock("GOLDEN RETRIEVER CLUB", "Banner text", 78f, 10f, 80f, 12f)
                        )
                    )
                )
            ),
            FeedAlbum(
                category = "TRIPS",
                title = "Wanderlust Chronicles",
                description = "Sunsets over classic coastal piers, refreshing nature trails among giant sequoias, and dusty maps collected along spontaneous routes.",
                icon = Icons.Default.FlightTakeoff,
                presetImages = listOf(
                    FeedImage(
                        url = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
                        fallbackText = "Malibu beach during dynamic sunset hour",
                        ocrBlocks = listOf(
                            OcrBlock("MALIBU COFFEE CO.", "Coffee Stand", 15f, 20f, 45f, 10f),
                            OcrBlock("GOLDEN HOUR • CALIFORNIA", "Footer Stamp", 72f, 15f, 70f, 15f)
                        )
                    ),
                    FeedImage(
                        url = "https://images.unsplash.com/photo-1448375240586-882707db888b?w=800",
                        fallbackText = "Giants of Muir Woods giant Redwood trails",
                        ocrBlocks = listOf(
                            OcrBlock("MUIR WOODS NATIONAL FOREST", "Park Sign", 20f, 15f, 70f, 12f),
                            OcrBlock("COASTAL TRAIL ->", "Directions Card", 55f, 30f, 40f, 10f),
                            OcrBlock("PROTECT OUR REDWOODS", "Preservation sign", 82f, 25f, 50f, 8f)
                        )
                    )
                )
            ),
            FeedAlbum(
                category = "DOCUMENTS",
                title = "Secure Code & Blueprints",
                description = "High-precision layout captures, local diagram mappings, database schemas, and structured architecture designs stored offline.",
                icon = Icons.Default.Article,
                presetImages = listOf(
                    FeedImage(
                        url = "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=800",
                        fallbackText = "Complex app controller flowchart graphic",
                        ocrBlocks = listOf(
                            OcrBlock("APPLICATION CONTROLLER", "Header Node", 8f, 10f, 80f, 10f),
                            OcrBlock("CLEAN ARCHITECTURE / VIEWMODEL", "Process block", 43f, 12f, 76f, 14f),
                            OcrBlock("SQLITE / ROOM DATABASE SCHEMA v2", "Footer Node", 80f, 15f, 70f, 12f)
                        )
                    ),
                    FeedImage(
                        url = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=800",
                        fallbackText = "Blue blueprint house design floorplan mapping",
                        ocrBlocks = listOf(
                            OcrBlock("MASTER BEDROOM 14'x16'", "Blueprint room label", 30f, 10f, 50f, 8f),
                            OcrBlock("DO NOT SCALE DRAWING", "Drafting warning", 85f, 20f, 60f, 6f)
                        )
                    )
                )
            ),
            FeedAlbum(
                category = "WORK",
                title = "Creative Office & Sprints",
                description = "Behind the scenes snapshot logs. Freshly baked catering events, whiteboard mind maps, team layouts, and active work setups.",
                icon = Icons.Default.Work,
                presetImages = listOf(
                    FeedImage(
                        url = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800",
                        fallbackText = "Fresh hand tossed woodfire pepperoni pizza logo",
                        ocrBlocks = listOf(
                            OcrBlock("PIZZERIA", "Shop Banner", 18f, 30f, 40f, 10f),
                            OcrBlock("100% WOODFIRE ARTISANAL", "Ad stamp", 48f, 15f, 70f, 12f),
                            OcrBlock("SPECIAL PRESET MENU - $14.99", "Sales board", 82f, 20f, 60f, 8f)
                        )
                    ),
                    FeedImage(
                        url = "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800",
                        fallbackText = "Laptop working interface development console",
                        ocrBlocks = listOf(
                            OcrBlock("val activeApp = composeApp()", "Kotlin Code snippet", 25f, 10f, 75f, 10f),
                            OcrBlock("fun renderFeedBrowser()", "Function declaration", 55f, 12f, 70f, 10f)
                        )
                    )
                )
            ),
            FeedAlbum(
                category = "AUDIO",
                title = "Leisure Beats & Voice Memos",
                description = "Recorded audio snippets, ambient study sounds, voice dictations, and voice-annotated media captures saved securely in your media vault.",
                icon = Icons.Default.Audiotrack,
                presetImages = listOf(
                    FeedImage(
                        url = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
                        fallbackText = "Vintage microphone close-up with neon overlay",
                        ocrBlocks = listOf()
                    )
                )
            )
        )
    }

    // Combine user-imported database files with original presets to build rich real-time carousels!
    val consolidatedAlbums = remember(allMedia, feedAlbums) {
        feedAlbums.map { mockAlbum ->
            // Find user database media items assigned to this specific album category or containing equivalent tags
            val dbAssocItems = allMedia.filter { item ->
                item.primaryAlbum.equals(mockAlbum.category, ignoreCase = true) ||
                        item.tags.contains(mockAlbum.category, ignoreCase = true)
            }

            // Map database items to FeedImage representation so they function in the swipe carousel
            val dbFeedImages = dbAssocItems.map { dbItem ->
                FeedImage(
                    url = dbItem.uri,
                    fallbackText = dbItem.displayName,
                    ocrBlocks = listOf(
                        OcrBlock(dbItem.displayName.uppercase(), "File Title", 20f, 15f, 70f, 10f),
                        OcrBlock(dbItem.caption.uppercase(), "AI Caption Node", 55f, 10f, 80f, 15f),
                        OcrBlock("AI CATEGORY: ${dbItem.primaryAlbum.uppercase()}", "Album badge label", 82f, 20f, 60f, 8f)
                    )
                )
            }

            // Prepend new user pictures so they appear as the first element in the swiper
            mockAlbum.copy(presetImages = dbFeedImages + mockAlbum.presetImages)
        }
    }

    // Filtered album feed list based on selected filter pill
    val filteredAlbums = remember(consolidatedAlbums, selectedFilter) {
        if (selectedFilter == "ALL") {
            consolidatedAlbums
        } else {
            consolidatedAlbums.filter { it.category.equals(selectedFilter, ignoreCase = true) }
        }
    }

    // Layout Screen
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- SECTION 1: Top Navigation (Horizontal Scrolling Row of Filter Pills) ---
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filterPills.forEach { pill ->
                    val isSelected = selectedFilter == pill
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = pill },
                        label = {
                            Text(
                                text = pill,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryCyan,
                            selectedLabelColor = BackgroundDark,
                            selectedLeadingIconColor = BackgroundDark,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) PrimaryCyan else BorderSlate,
                            selectedBorderColor = PrimaryCyan,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("filter_pill_${pill.lowercase()}")
                    )
                }
            }
        }

        // --- SECTION 2: Vertical Feed ---
        if (filteredAlbums.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Feed,
                        contentDescription = "No items",
                        tint = SoftGray,
                        modifier = Modifier
                            .size(72.dp)
                            .padding(bottom = 12.dp)
                    )
                    Text(
                        text = "No Albums Found",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Import a picture with this category in Gallery to construct your live feed view.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("feed_lazy_column"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(filteredAlbums, key = { it.category }) { album ->
                    AlbumCardComponent(
                        album = album,
                        onTriggerOcr = { img, title ->
                            activeOcrImage = img
                            activeOcrTitle = title
                            showOcrOverlay = true
                        }
                    )
                }
            }
        }
    }

    // --- SECTION 3: Google Lens Style Text Capture OCR Overlay Dialog ---
    if (showOcrOverlay && activeOcrImage != null) {
        TextCaptureDialog(
            image = activeOcrImage!!,
            albumTitle = activeOcrTitle,
            onDismiss = {
                showOcrOverlay = false
                activeOcrImage = null
            },
            onSaveNote = { note ->
                // Injects notes log in database for physical state retention visualization
                viewModel.createNewAlbum(note.take(15)) // Save note preview reference
                Toast.makeText(context, "Saved Text Capture directly as note!", Toast.LENGTH_SHORT).show()
                showOcrOverlay = false
            }
        )
    }
}

// Visual Component for Album Card in the Feed
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumCardComponent(
    album: FeedAlbum,
    onTriggerOcr: (FeedImage, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val images = album.presetImages
    val pagerState = rememberPagerState(pageCount = { images.size })

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("album_card_${album.category.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            
            // --- Header bar of social card ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(PrimaryCyan.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = album.icon,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${album.category} • Vault Library",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "More options", tint = SoftGray)
                }
            }

            // --- Media Container (Carousel with Swipe support and Audio player custom representation) ---
            if (album.category == "AUDIO") {
                var isPlaying by remember { mutableStateOf(false) }
                val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")
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
                        label = "bar_$index"
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PrimaryCyan.copy(alpha = 0.22f),
                                    BackgroundDark
                                )
                            )
                        )
                        .testTag("audio_player_container_${album.category.lowercase()}"),
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
                            .testTag("audio_waveform"),
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
                            .testTag("audio_play_pause_button_${album.category.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = BackgroundDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Decorative tag
                    Text(
                        text = if (isPlaying) "PLAYING SECURE AUDIO" else "TAP TO STREAM AUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPlaying) PrimaryCyan else SoftGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 24.dp)
                            .testTag("audio_status_text")
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(BackgroundDark)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val img = images[page]
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = img.url,
                                contentDescription = img.fallbackText,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Glowing gradient fade overlay on top for clean contrast
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.4f)
                                            ),
                                            startY = 180f
                                        )
                                    )
                            )

                            // Floating Text Capture (OCR) Icon Button inside bottom right of each active image!
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                            ) {
                                FilledIconButton(
                                    onClick = { onTriggerOcr(img, album.title) },
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = PrimaryCyan,
                                        contentColor = BackgroundDark
                                    ),
                                    modifier = Modifier
                                        .size(42.dp)
                                        .testTag("ocr_capture_trigger_${album.category.lowercase()}_p$page")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CenterFocusWeak,
                                        contentDescription = "Trigger Text Capture AI OCR Tool",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Dot Indicators at the bottom center of Carousel if multi-images exist
                    if (images.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(images.size) { idx ->
                                val active = pagerState.currentPage == idx
                                Box(
                                    modifier = Modifier
                                        .size(if (active) 10.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (active) PrimaryCyan else Color.White.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }

            // --- Bottom metadata (Title & Conversational social-style description caption) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = album.title,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = album.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = BorderSlate, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Footnote interactive controls mimicking active social reactions/analytics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${album.presetImages.size} items in locker",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Encrypted Vault",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}

// Google Lens styled screen Dialog that acts as the Text Capture overlay
@Composable
fun TextCaptureDialog(
    image: FeedImage,
    albumTitle: String,
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val ocrBlocks = image.ocrBlocks

    // Active Highlight Section State
    var selectedBlock by remember { mutableStateOf<OcrBlock?>(ocrBlocks.firstOrNull()) }
    var activeLanguage by remember { mutableStateOf("ENGLISH") } // ENGLISH, SPANISH, FRENCH, JAPANESE
    var isTranslating by remember { mutableStateOf(false) }

    // Translation values mapping
    val transMap = remember {
        mapOf(
            "ADOPTION CENTER" to mapOf(
                "SPANISH" to "CENTRO DE ADOPCIÓN",
                "FRENCH" to "CENTRE D'ADOPTION",
                "JAPANESE" to "里親募集センター"
            ),
            "PLEASE ADOPT ME! MEOW MEOW" to mapOf(
                "SPANISH" to "¡POR FAVOR ADÓPTAME! MIAU MIAU",
                "FRENCH" to "S'IL VOUS PLAÎT ADOPTEZ-MOI! MIAOU MIAOU",
                "JAPANESE" to "私を養子にしてください！ニャーニャー"
            ),
            "FELINE PROTECTION SOC." to mapOf(
                "SPANISH" to "SOC. DE PROTECCIÓN FELINA",
                "FRENCH" to "SOC. DE PROTECTION DES FÉLINS",
                "JAPANESE" to "猫保護協会"
            ),
            "CHAMPION PET" to mapOf(
                "SPANISH" to "MASCOTA CAMPEÓN",
                "FRENCH" to "ANIMAL CHAMPION",
                "JAPANESE" to "チャンピオンペット"
            ),
            "GOLDEN RETRIEVER CLUB" to mapOf(
                "SPANISH" to "CLUB DE GOLDEN RETRIEVER",
                "FRENCH" to "CLUB DU GOLDEN RETRIEVER",
                "JAPANESE" to "ゴールデンレトリバークラブ"
            ),
            "MALIBU COFFEE CO." to mapOf(
                "SPANISH" to "CAFÉ DE MALIBU S.A.",
                "FRENCH" to "CAFÉ DE MALIBU CIE.",
                "JAPANESE" to "マリブコーヒー株式会社"
            ),
            "GOLDEN HOUR • CALIFORNIA" to mapOf(
                "SPANISH" to "HORA DORADA • CALIFORNIA",
                "FRENCH" to "HEURE DORÉE • CALIFORNIE",
                "JAPANESE" to "ゴールデンアワー・カリフォルニア"
            ),
            "MUIR WOODS NATIONAL FOREST" to mapOf(
                "SPANISH" to "BOSQUE NACIONAL MUIR WOODS",
                "FRENCH" to "FORÊT NATIONALE DE MUIR WOODS",
                "JAPANESE" to "ミュアウッズ国立森林公園"
            ),
            "COASTAL TRAIL ->" to mapOf(
                "SPANISH" to "RUTA COSTERA ->",
                "FRENCH" to "SENTIER CÔTIER ->",
                "JAPANESE" to "沿岸歩道 ->"
            ),
            "PROTECT OUR REDWOODS" to mapOf(
                "SPANISH" to "PROTEGE NUESTRAS SECUOYAS",
                "FRENCH" to "PROTÉGEZ NOS SÉQUOIAS",
                "JAPANESE" to "私たちのレッドウッドを守る"
            ),
            "APPLICATION CONTROLLER" to mapOf(
                "SPANISH" to "CONTROLADOR DE APLICACIÓN",
                "FRENCH" to "CONTRÔLEUR D'APPLICATION",
                "JAPANESE" to "アプリケーションコントローラー"
            ),
            "CLEAN ARCHITECTURE / VIEWMODEL" to mapOf(
                "SPANISH" to "ARQUITECTURA LIMPIA / VIEWMODEL",
                "FRENCH" to "ARCHITECTURE PROPRE / VIEWMODEL",
                "JAPANESE" to "クリーンアーキテクチャ・ビューモデル"
            ),
            "SQLITE / ROOM DATABASE SCHEMA v2" to mapOf(
                "SPANISH" to "ESQUEMA DE BASE DE DATOS ROOM v2",
                "FRENCH" to "SCHÉMA DE BASE DE DONNÉES ROOM v2",
                "JAPANESE" to "SQLITE ROOM データベーススキーマ v2"
            ),
            "MASTER BEDROOM 14'x16'" to mapOf(
                "SPANISH" to "DORMITORIO PRINCIPAL 14'x16'",
                "FRENCH" to "CHAMBRE PRINCIPALE 14'x16'",
                "JAPANESE" to "主寝室 14'x16'"
            ),
            "DO NOT SCALE DRAWING" to mapOf(
                "SPANISH" to "NO ESCALAR DIBUJO",
                "FRENCH" to "NE PAS METTRE À L'ÉCHELLE LE DESSIN",
                "JAPANESE" to "図面を縮尺調整しないでください"
            ),
            "PIZZERIA" to mapOf(
                "SPANISH" to "PIZZERÍA",
                "FRENCH" to "PIZZÉRIA",
                "JAPANESE" to "ピッツェリア"
            ),
            "100% WOODFIRE ARTISANAL" to mapOf(
                "SPANISH" to "100% ARTESANAL AL HORNO DE LEÑA",
                "FRENCH" to "100% ARTISANAL DE FEU DE BOIS",
                "JAPANESE" to "100% 薪窯職人手作り"
            ),
            "SPECIAL PRESET MENU - $14.99" to mapOf(
                "SPANISH" to "MENÚ ESPECIAL DE PREAJUSTE - $14.99",
                "FRENCH" to "MENU SPÉCIAL PRÉDÉFINI - $14.99",
                "JAPANESE" to "スペシャルプリセットメニュー $14.99"
            ),
            "val activeApp = composeApp()" to mapOf(
                "SPANISH" to "val activeApp = composeApp()",
                "FRENCH" to "val activeApp = composeApp()",
                "JAPANESE" to "val activeApp = composeApp()"
            ),
            "fun renderFeedBrowser()" to mapOf(
                "SPANISH" to "fun renderFeedBrowser()",
                "FRENCH" to "fun renderFeedBrowser()",
                "JAPANESE" to "fun renderFeedBrowser()"
            )
        )
    }

    // Coroutine lifecycle to handle simulated translation delay
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false // Allows full screen overlay immersion!
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .testTag("ocr_overlay_dialog")
        ) {
            
            // --- HEADER ACTION BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusWeak,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Elax Text Capture (OCR)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tapping a highlighted frame targets text",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftGray
                        )
                    }
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .testTag("btn_close_ocr")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Text Capture Scanner",
                        tint = Color.White
                    )
                }
            }

            // --- DEEP INTERACTIVE PREVIEW IMAGE CANVAS (MIDDLE) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.64f)
                    .padding(horizontal = 16.dp, vertical = 80.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .background(BackgroundDark),
                contentAlignment = Alignment.Center
            ) {
                // Background Picture under scanning
                AsyncImage(
                    model = image.url,
                    contentDescription = "Active scanning image content",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Layout absolute overlays representing localized scan text blocks!
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val scaleX = maxWidth
                    val scaleY = maxHeight

                    // Map elements to custom positional overlays
                    ocrBlocks.forEach { block ->
                        val isSelected = selectedBlock == block
                        
                        // Positional metrics mapped from percentages
                        val leftPx = scaleX * (block.leftPercent / 100f)
                        val topPx = scaleY * (block.topPercent / 100f)
                        val widthPx = scaleX * (block.widthPercent / 100f)
                        val heightPx = scaleY * (block.heightPercent / 100f)

                        Box(
                            modifier = Modifier
                                .absoluteOffset(x = leftPx, y = topPx)
                                .size(width = widthPx, height = heightPx)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) PrimaryCyan.copy(alpha = 0.32f)
                                    else Color.Yellow.copy(alpha = 0.15f)
                                )
                                .border(
                                    width = (if (isSelected) 2.dp else 1.dp),
                                    color = if (isSelected) PrimaryCyan else Color.Yellow.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    selectedBlock = block
                                }
                                .testTag("ocr_block_${block.text.lowercase().replace(" ", "_")}")
                        ) {
                            // Subtle lens indicator mark on left edge
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(3.dp)
                                    .background(if (isSelected) PrimaryCyan else Color.Yellow)
                            )
                        }
                    }
                }
            }

            // --- BOTTOM DETAILS & FUNCTIONAL ACTION SLID-UP PANEL ---
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("ocr_action_panel"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Title section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = PrimaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Captured Ocr Node String",
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Target Language Select Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    // Cycles active languages for translation demo
                                    val nextLang = when (activeLanguage) {
                                        "ENGLISH" -> "SPANISH"
                                        "SPANISH" -> "FRENCH"
                                        "FRENCH" -> "JAPANESE"
                                        else -> "ENGLISH"
                                    }
                                    isTranslating = true
                                    activeLanguage = nextLang
                                    // Short simulation to mimic neural translations
                                    isTranslating = false
                                }
                                .background(BorderSlate)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("ocr_language_toggle")
                        ) {
                            Icon(imageVector = Icons.Default.Translate, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeLanguage,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Captured text block display (with translation support!)
                    val rawText = selectedBlock?.text ?: "No Selection. Please click on a highlighted frame inside the scan preview above."
                    
                    val renderedText = remember(rawText, activeLanguage, isTranslating, transMap) {
                        if (isTranslating) {
                            "Translating neural parameters..."
                        } else if (activeLanguage == "ENGLISH") {
                            rawText
                        } else {
                            transMap[rawText]?.get(activeLanguage) ?: "[Translation translation layer lookup failed]"
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundDark, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = renderedText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_display_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ACTION BUTTON ROW (COPY TEXT, SAVE AS NOTE, TRANSLATE)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Action A: COPY TEXT (Real clipboard integration)
                        Button(
                            onClick = {
                                if (selectedBlock != null) {
                                    clipboardManager.setText(AnnotatedString(renderedText))
                                    Toast.makeText(context, "Text copied to device clipboard!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please select a frame first!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryCyan,
                                contentColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("ocr_action_copy")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Text", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        // Action B: SAVE AS NOTE (Physical integration into local state sync log notes!)
                        Button(
                            onClick = {
                                if (selectedBlock != null) {
                                    onSaveNote("[Captured note] $renderedText")
                                } else {
                                    Toast.makeText(context, "Please select a frame first!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("ocr_action_save_note")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        // Action C: TRANSLATE (Cycle languages or immediate spanish default)
                        Button(
                            onClick = {
                                isTranslating = true
                                activeLanguage = "SPANISH"
                                isTranslating = false
                                Toast.makeText(context, "Translated text to Spanish!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BorderSlate,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("ocr_action_translate")
                        ) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Translate", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
