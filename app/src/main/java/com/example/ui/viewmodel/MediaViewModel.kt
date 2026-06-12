package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.MediaItem
import com.example.data.model.SyncLog
import com.example.data.network.GeminiService
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    GALLERY,
    ALBUMS,
    FEED,
    CLOUD_SYNC,
    SETTINGS
}

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "MediaViewModel"

    private val mediaDao = AppDatabase.getDatabase(application).mediaDao()
    private val geminiService = GeminiService(application)
    private val repository = MediaRepository(application, mediaDao, geminiService)

    // Current screen Tab
    private val _currentTab = MutableStateFlow(AppTab.GALLERY)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Gallery Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterMediaType = MutableStateFlow("ALL") // ALL, IMAGE, VIDEO
    val filterMediaType: StateFlow<String> = _filterMediaType.asStateFlow()

    // Selected album for detail filtering
    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum.asStateFlow()

    // Chosen Media File detail sheet state
    private val _focusedMediaItem = MutableStateFlow<MediaItem?>(null)
    val focusedMediaItem: StateFlow<MediaItem?> = _focusedMediaItem.asStateFlow()

    // Sync configuration options
    private val _autoSyncEnabled = MutableStateFlow(true)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    private val _wifiOnlyEnabled = MutableStateFlow(false)
    val wifiOnlyEnabled: StateFlow<Boolean> = _wifiOnlyEnabled.asStateFlow()

    private val _highResBackups = MutableStateFlow(true)
    val highResBackups: StateFlow<Boolean> = _highResBackups.asStateFlow()

    // Is active cloud sync running (animation flag)
    private val _syncingState = MutableStateFlow(false)
    val syncingState: StateFlow<Boolean> = _syncingState.asStateFlow()

    // Flow containing all database storage items
    val allMedia: StateFlow<List<MediaItem>> = repository.allMediaItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow containing all recent activity logs
    val recentLogs: StateFlow<List<SyncLog>> = repository.recentSyncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined flows to drive complex searching and filtering
    val filteredMedia: StateFlow<List<MediaItem>> = combine(allMedia, searchQuery, filterMediaType, selectedAlbum) { media, query, filter, album ->
        media.filter { item ->
            // Album constraint
            val matchesAlbum = album == null || item.primaryAlbum == album
            
            // Type matching
            val matchesType = when (filter) {
                "IMAGE" -> item.mediaType.startsWith("image")
                "VIDEO" -> item.mediaType.startsWith("video")
                "AUDIO" -> item.mediaType.startsWith("audio")
                else -> true
            }

            // Text search constraint (match name, tags, caption, location or album)
            val matchesQuery = query.isBlank() || 
                item.displayName.contains(query, ignoreCase = true) ||
                item.caption.contains(query, ignoreCase = true) ||
                item.location.contains(query, ignoreCase = true) ||
                item.primaryAlbum.contains(query, ignoreCase = true) ||
                item.tags.contains(query, ignoreCase = true)

            matchesAlbum && matchesType && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic map of Album details, combining AI-attributed and manual custom albums
    val smartAlbums: StateFlow<List<VisualAlbum>> = combine(allMedia, repository.allCustomAlbums) { items, custom ->
        val grouped = items.groupBy { it.primaryAlbum }
        val allAlbumNames = (grouped.keys + custom.map { it.name } + "Uncategorized").toSet()

        allAlbumNames.map { albumName ->
            val list = grouped[albumName] ?: emptyList()
            VisualAlbum(
                name = albumName,
                itemCount = list.size,
                latestCoverUri = list.firstOrNull()?.uri,
                lastModified = if (list.isEmpty()) {
                    custom.find { it.name == albumName }?.timestamp ?: 0L
                } else {
                    list.map { it.timestamp }.maxOrNull() ?: 0L
                }
            )
        }.filter {
            it.name != "Analyzing..." || it.itemCount > 0
        }.sortedWith(
            compareByDescending<VisualAlbum> { it.name == "Uncategorized" }
                .thenBy { it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Background job for periodic "auto-sync" checking
    private var syncPollingJob: Job? = null

    init {
        // Log startup and begin periodic sync runner
        viewModelScope.launch {
            repository.addSyncLog("Elax Storage Vault online. Secure client ready.", "INFO")
        }
        startSyncDaemon()
    }

    private fun startSyncDaemon() {
        syncPollingJob?.cancel()
        syncPollingJob = viewModelScope.launch {
            while (true) {
                delay(12000) // check every 12 seconds
                if (_autoSyncEnabled.value && !_syncingState.value) {
                    val pendingCount = allMedia.value.count { it.syncStatus == "PENDING" }
                    if (pendingCount > 0) {
                        triggerCloudSync()
                    }
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterMediaType(type: String) {
        _filterMediaType.value = type
    }

    fun selectAlbum(albumName: String?) {
        _selectedAlbum.value = albumName
        // If an album is opened, ensure visual tabs are contextualized
        if (albumName != null) {
            _currentTab.value = AppTab.GALLERY
        }
    }

    fun focusMediaItem(item: MediaItem?) {
        _focusedMediaItem.value = item
    }

    fun toggleAutoSync(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        viewModelScope.launch {
            repository.addSyncLog("Automated background synchronization turned ${if (enabled) "ON" else "OFF"}.", "INFO")
        }
    }

    fun toggleWifiOnly(enabled: Boolean) {
        _wifiOnlyEnabled.value = enabled
        viewModelScope.launch {
            repository.addSyncLog("Cellular networks toggle updated: ${if (enabled) "Wi-Fi Only" else "Cellular + Wi-Fi Enabled"}.", "INFO")
        }
    }

    fun toggleHighRes(enabled: Boolean) {
        _highResBackups.value = enabled
    }

    // Trigger manual or background synchronization sequence
    fun triggerCloudSync() {
        if (_syncingState.value) return
        
        viewModelScope.launch {
            _syncingState.value = true
            try {
                repository.runCloudSyncSequence()
            } catch (e: Exception) {
                Log.e(tag, "Sync thread error: ${e.message}")
            } finally {
                _syncingState.value = false
            }
        }
    }

    // Local file selector importer
    fun importLocalFile(uri: Uri, displayName: String, size: Long, mediaType: String) {
        viewModelScope.launch {
            repository.importMedia(uri, displayName, size, mediaType)
        }
    }

    // Insert public metadata demo assets directly inside the user's view
    fun loadPresetDemoContent() {
        viewModelScope.launch {
            repository.addSyncLog("Loading premium High-Quality simulated assets...", "INFO")
            
            val presets = listOf(
                PresetAsset(
                    url = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
                    name = "Malibu Sunset Pier.jpg",
                    size = 1450000,
                    type = "image/jpeg"
                ),
                PresetAsset(
                    url = "https://images.unsplash.com/photo-1448375240586-882707db888b?w=800",
                    name = "Muir Forest Giant Redwoods.jpg",
                    size = 2240000,
                    type = "image/jpeg"
                ),
                PresetAsset(
                    url = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800",
                    name = "Woodfire Pepperoni Deluxe.jpg",
                    size = 980000,
                    type = "image/jpeg"
                ),
                PresetAsset(
                    url = "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=800",
                    name = "Software Architecture Blueprint.jpg",
                    size = 1860000,
                    type = "image/jpeg"
                ),
                PresetAsset(
                    url = "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800",
                    name = "Fluffy Calico Kitten Playtime.jpg",
                    size = 790000,
                    type = "image/jpeg"
                ),
                PresetAsset(
                    url = "https://example.com/audio/cyberpunk_study_beat.mp3",
                    name = "Lofi Study Beats.mp3",
                    size = 5240000,
                    type = "audio/mpeg"
                ),
                PresetAsset(
                    url = "https://example.com/audio/meeting_dictation_notes.wav",
                    name = "Strategy Sync Voice Memo.wav",
                    size = 2850000,
                    type = "audio/wav"
                )
            )

            for (preset in presets) {
                repository.importMedia(
                    uri = Uri.parse(preset.url),
                    displayName = preset.name,
                    size = preset.size,
                    mediaType = preset.type
                )
                // Stagger loading slightly to make logs feel dynamic
                delay(300)
            }
        }
    }

    fun deleteMediaItem(item: MediaItem) {
        viewModelScope.launch {
            repository.deleteMedia(item)
            if (_focusedMediaItem.value?.id == item.id) {
                _focusedMediaItem.value = null
            }
        }
    }

    fun createNewAlbum(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createCustomAlbum(name.trim())
        }
    }

    fun moveMediaToAlbum(item: MediaItem, newAlbum: String) {
        viewModelScope.launch {
            repository.updateMediaAlbum(item, newAlbum)
            if (_focusedMediaItem.value?.id == item.id) {
                _focusedMediaItem.value = item.copy(primaryAlbum = newAlbum)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearVault()
            _focusedMediaItem.value = null
            _selectedAlbum.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncPollingJob?.cancel()
    }
}

data class VisualAlbum(
    val name: String,
    val itemCount: Int,
    val latestCoverUri: String?,
    val lastModified: Long
)

data class PresetAsset(
    val url: String,
    val name: String,
    val size: Long,
    val type: String
)
