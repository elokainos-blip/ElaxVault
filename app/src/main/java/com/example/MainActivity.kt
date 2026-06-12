package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.screens.*
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MediaViewModel
import android.provider.OpenableColumns

class MainActivity : ComponentActivity() {
    
    private val viewModel: MediaViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val currentTab by viewModel.currentTab.collectAsState()
                val focusedMediaItem by viewModel.focusedMediaItem.collectAsState()
                var showImportDialog by remember { mutableStateOf(false) }
                val context = LocalContext.current

                // Launcher to choose media file from standard device gallery (PickVisualMedia)
                val directGalleryPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        var fileName = "imported_${System.currentTimeMillis()}.jpg"
                        var fileSize = 2024L
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
                    }
                }

                // Launcher to choose audio file from device storage
                val directAudioPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        var fileName = "audio_${System.currentTimeMillis()}.mp3"
                        var fileSize = 1048576L
                        val mimeType = context.contentResolver.getType(uri) ?: "audio/mpeg"
                        
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (cursor.moveToFirst()) {
                                if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                            }
                        }
                        viewModel.importLocalFile(uri, fileName, fileSize, mimeType)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = PrimaryCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ELAX VAULT",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            actions = {
                                if (currentTab == AppTab.GALLERY) {
                                    IconButton(
                                        onClick = { showImportDialog = true },
                                        modifier = Modifier.testTag("action_import_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Import menu",
                                            tint = PrimaryCyan
                                        )
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("main_bottom_navigation")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == AppTab.GALLERY,
                                onClick = { viewModel.selectTab(AppTab.GALLERY) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == AppTab.GALLERY) Icons.Default.PhotoLibrary else Icons.Default.PhotoLibrary,
                                        contentDescription = "Gallery"
                                    )
                                },
                                label = { Text("My Vault") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BackgroundDark,
                                    selectedTextColor = PrimaryCyan,
                                    indicatorColor = PrimaryCyan,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                ),
                                modifier = Modifier.testTag("tab_gallery_button")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.ALBUMS,
                                onClick = { viewModel.selectTab(AppTab.ALBUMS) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Label,
                                        contentDescription = "Smart Albums"
                                    )
                                },
                                label = { Text("Albums") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BackgroundDark,
                                    selectedTextColor = PrimaryCyan,
                                    indicatorColor = PrimaryCyan,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                ),
                                modifier = Modifier.testTag("tab_albums_button")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.FEED,
                                onClick = { viewModel.selectTab(AppTab.FEED) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Feed,
                                        contentDescription = "Feed Browser"
                                    )
                                },
                                label = { Text("Feed") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BackgroundDark,
                                    selectedTextColor = PrimaryCyan,
                                    indicatorColor = PrimaryCyan,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                ),
                                modifier = Modifier.testTag("tab_feed_button")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.CLOUD_SYNC,
                                onClick = { viewModel.selectTab(AppTab.CLOUD_SYNC) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Cloud Backup"
                                    )
                                },
                                label = { Text("Cloud Sync") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BackgroundDark,
                                    selectedTextColor = PrimaryCyan,
                                    indicatorColor = PrimaryCyan,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                ),
                                modifier = Modifier.testTag("tab_sync_button")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.SETTINGS,
                                onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BackgroundDark,
                                    selectedTextColor = PrimaryCyan,
                                    indicatorColor = PrimaryCyan,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                ),
                                modifier = Modifier.testTag("tab_settings_button")
                            )
                        }
                    },
                    floatingActionButton = {
                        // Display FLOATING action button when viewing photos to invite uploading!
                        if (currentTab == AppTab.GALLERY) {
                            FloatingActionButton(
                                onClick = { showImportDialog = true },
                                containerColor = PrimaryCyan,
                                contentColor = BackgroundDark,
                                shape = CircleShape,
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .testTag("add_media_fab")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Upload new photo",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Swipe transitions or cross-fade between active tabs
                        Crossfade(targetState = currentTab, label = "tab_fade") { tab ->
                            when (tab) {
                                AppTab.GALLERY -> GalleryScreen(viewModel = viewModel)
                                AppTab.ALBUMS -> AlbumsScreen(viewModel = viewModel)
                                AppTab.FEED -> FeedBrowserScreen(viewModel = viewModel)
                                AppTab.CLOUD_SYNC -> SyncScreen(viewModel = viewModel)
                                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }

                // --- 1. Selection Option Dialog (Real Picker vs AI Demo Content) ---
                if (showImportDialog) {
                    Dialog(onDismissRequest = { showImportDialog = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .testTag("import_media_dialog"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Organize New Media",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Each uploaded file is analyzed using neural frameworks to create searchable captions, albums, and tags.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // Option A: Open Phone Gallery
                                Button(
                                    onClick = {
                                        showImportDialog = false
                                        directGalleryPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("dialog_import_gallery"),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = BackgroundDark),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Choose from Device Gallery", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Option A2: Open Phone Audio Picker
                                Button(
                                    onClick = {
                                        showImportDialog = false
                                        directAudioPickerLauncher.launch("audio/*")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("dialog_import_audio"),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = BackgroundDark),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Choose Audio from Device", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Option B: Use Presets (Instant onboarding on emulator)
                                OutlinedButton(
                                    onClick = {
                                        showImportDialog = false
                                        viewModel.loadPresetDemoContent()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("dialog_import_preset"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCyan),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Inject Photo Presets", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { showImportDialog = false }) {
                                    Text("Cancel", color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // --- 2. Slid-up Media Detail Bottom Sheet ---
                focusedMediaItem?.let { item ->
                    val albumsList by viewModel.smartAlbums.collectAsState()
                    val albumNames = albumsList.map { it.name }
                    MediaDetailBottomSheet(
                        item = item,
                        onDismiss = { viewModel.focusMediaItem(null) },
                        onDelete = {
                            viewModel.deleteMediaItem(item)
                        },
                        onMoveToAlbum = { newAlbum ->
                            viewModel.moveMediaToAlbum(item, newAlbum)
                        },
                        availableAlbums = albumNames
                    )
                }
            }
        }
    }
}
