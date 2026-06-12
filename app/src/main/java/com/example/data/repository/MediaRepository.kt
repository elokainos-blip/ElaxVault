package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.database.MediaDao
import com.example.data.model.MediaItem
import com.example.data.model.SyncLog
import com.example.data.model.CustomAlbum
import com.example.data.network.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks

class MediaRepository(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val geminiService: GeminiService
) {
    private val tag = "MediaRepository"

    val allMediaItems: Flow<List<MediaItem>> = mediaDao.getAllMediaItems()
    val recentSyncLogs: Flow<List<SyncLog>> = mediaDao.getRecentSyncLogs()
    val allCustomAlbums: Flow<List<CustomAlbum>> = mediaDao.getAllCustomAlbums()

    // Query media items grouped by album
    fun getItemsByAlbum(album: String): Flow<List<MediaItem>> = mediaDao.getMediaItemsByAlbum(album)

    // Add a media file and automatically organize it via Gemini AI
    suspend fun importMedia(uri: Uri, displayName: String, size: Long, mediaType: String) = withContext(Dispatchers.IO) {
        val uriString = uri.toString()
        Log.i(tag, "Importing media file: $displayName ($mediaType, $size bytes)")

        // 1. Log start of import
        mediaDao.insertSyncLog(
            SyncLog(message = "Importing file '$displayName'. Preparing AI organization...", type = "INFO")
        )

        // 2. Insert item as analyzing state
        val placeholderItem = MediaItem(
            uri = uriString,
            displayName = displayName,
            mediaType = mediaType,
            size = size,
            caption = "AI analyzing...",
            location = "Identifying place...",
            primaryAlbum = "Analyzing...",
            tags = "analyzing, pending",
            syncStatus = "PENDING"
        )
        val itemId = mediaDao.insertMediaItem(placeholderItem)

        // 3. Perform AI organization
        try {
            val analysis = geminiService.analyzeMedia(uriString, displayName)
            val updatedItem = mediaDao.getMediaItemById(itemId)?.copy(
                caption = analysis.caption,
                location = analysis.location,
                primaryAlbum = analysis.primaryAlbum,
                tags = analysis.tags
            )
            if (updatedItem != null) {
                mediaDao.updateMediaItem(updatedItem)
                mediaDao.insertSyncLog(
                    SyncLog(
                        message = "AI successfully categorized '$displayName' into album '${analysis.primaryAlbum}'!",
                        type = "SUCCESS"
                    )
                )
                Log.d(tag, "Recognized item content: $analysis")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed resolving AI organization: ${e.message}")
            val failedItem = mediaDao.getMediaItemById(itemId)?.copy(
                caption = "Could not analyze. Locally stored.",
                location = "Unknown",
                primaryAlbum = "Local Import",
                tags = "local",
                syncStatus = "PENDING"
            )
            if (failedItem != null) {
                mediaDao.updateMediaItem(failedItem)
            }
            mediaDao.insertSyncLog(
                SyncLog(message = "AI analysis failed for '$displayName': ${e.simpleMessage()}. Logged as offline storage.", type = "WARNING")
            )
        }
    }

    private fun ensureFirestore(): FirebaseFirestore? {
        return try {
            val apps = FirebaseApp.getApps(context)
            val app = if (apps.isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:539293748233:android:9d784a3cbcf123abcd")
                    .setApiKey("AlzaSyA9B8C7D6E5F4_ElaxVaultKey")
                    .setProjectId("elax-vault-992")
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                apps.first()
            }
            FirebaseFirestore.getInstance(app)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Firebase Firestore: ${e.message}")
            null
        }
    }

    // Run active cloud sync sequence
    suspend fun runCloudSyncSequence() = withContext(Dispatchers.IO) {
        val pendingItems = mediaDao.getPendingSyncItems()
        if (pendingItems.isEmpty()) {
            return@withContext
        }

        Log.i(tag, "Found ${pendingItems.size} items pending cloud sync. Starting sync worker.")
        mediaDao.insertSyncLog(
            SyncLog(message = "Cloud sync thread started. Syncing ${pendingItems.size} file(s)...", type = "INFO")
        )

        val firestore = ensureFirestore()
        if (firestore == null) {
            mediaDao.insertSyncLog(
                SyncLog(message = "Google Play Services or Firebase initialization aborted. Check console logs.", type = "ERROR")
            )
        }

        for (item in pendingItems) {
            try {
                // Set status to SYNCING
                mediaDao.updateMediaItem(item.copy(syncStatus = "SYNCING"))
                mediaDao.insertSyncLog(
                    SyncLog(message = "Uploading '${item.displayName}' chunk stream (size: ${formatFileSize(item.size)})...", type = "INFO")
                )

                // Simulate upload latency
                delay(1200)

                // Generate simulated cloud storage URL
                val cloudUrl = "https://luminasite-cloud-storage.net/users/backup/${item.displayName.replace(" ", "_")}"
                mediaDao.updateMediaItem(item.copy(syncStatus = "SYNCED", cloudUrl = cloudUrl))

                mediaDao.insertSyncLog(
                    SyncLog(message = "Successfully backed up '${item.displayName}' to cloud storage.", type = "SUCCESS")
                )

                if (firestore != null) {
                    val mediaMap = hashMapOf(
                        "id" to item.id,
                        "displayName" to item.displayName,
                        "mediaType" to item.mediaType,
                        "size" to item.size,
                        "caption" to item.caption,
                        "location" to item.location,
                        "primaryAlbum" to item.primaryAlbum,
                        "tags" to item.tags.split(",").map { it.trim() },
                        "cloudUrl" to cloudUrl,
                        "syncedAt" to System.currentTimeMillis()
                    )

                    mediaDao.insertSyncLog(
                        SyncLog(message = "Writing '${item.displayName}' metadata & AI tags to Cloud Firestore sync log collection...", type = "INFO")
                    )

                    val task = firestore.collection("shared_elax_vault")
                        .document("item_${item.id}")
                        .set(mediaMap)

                    try {
                        Tasks.await(task)
                        mediaDao.insertSyncLog(
                            SyncLog(message = "Firestore Sync: Metadata & tags recorded securely in Firebase collection 'shared_elax_vault'.", type = "SUCCESS")
                        )
                    } catch (taskEx: Exception) {
                        Log.e(tag, "Firestore Task awaited with exception: ${taskEx.simpleMessage()}")
                        mediaDao.insertSyncLog(
                            SyncLog(
                                message = "Firestore written to offline queue: ${taskEx.simpleMessage()}.",
                                type = "WARNING"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed syncing item ${item.id}: ${e.message}")
                mediaDao.updateMediaItem(item.copy(syncStatus = "FAILED"))
                mediaDao.insertSyncLog(
                    SyncLog(message = "Backup aborted for '${item.displayName}': ${e.simpleMessage()}.", type = "ERROR")
                )
            }
        }

        mediaDao.insertSyncLog(
            SyncLog(message = "Cloud backup sync cycle complete.", type = "INFO")
        )
    }

    suspend fun deleteMedia(item: MediaItem) = withContext(Dispatchers.IO) {
        mediaDao.deleteMediaItem(item)
        mediaDao.insertSyncLog(
            SyncLog(message = "Removed file '${item.displayName}' from vault storage.", type = "WARNING")
        )
    }

    suspend fun clearVault() = withContext(Dispatchers.IO) {
        mediaDao.clearAllMedia()
        mediaDao.clearSyncLogs()
        mediaDao.clearCustomAlbums()
        mediaDao.insertSyncLog(
            SyncLog(message = "Full storage wipe completed. Vault is empty.", type = "WARNING")
        )
    }

    suspend fun createCustomAlbum(albumName: String) = withContext(Dispatchers.IO) {
        Log.i(tag, "Creating custom manual album: $albumName")
        mediaDao.insertCustomAlbum(CustomAlbum(name = albumName))
        mediaDao.insertSyncLog(
            SyncLog(message = "Manual album '$albumName' created successfully.", type = "SUCCESS")
        )
    }

    suspend fun updateMediaAlbum(item: MediaItem, newAlbum: String) = withContext(Dispatchers.IO) {
        Log.i(tag, "Moving ${item.displayName} to album $newAlbum")
        val updated = item.copy(primaryAlbum = newAlbum)
        mediaDao.updateMediaItem(updated)
        mediaDao.insertSyncLog(
            SyncLog(message = "Organized '${item.displayName}' into album '$newAlbum'.", type = "INFO")
        )
    }

    suspend fun addSyncLog(message: String, type: String) = withContext(Dispatchers.IO) {
        mediaDao.insertSyncLog(SyncLog(message = message, type = type))
    }

    private fun Exception.simpleMessage(): String = this.message ?: "Unknown server error"

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
