package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "media_items")
@JsonClass(generateAdapter = true)
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val displayName: String,
    val mediaType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val size: Long = 0,
    val caption: String = "Analyzing...",
    val location: String = "Detecting location...",
    val primaryAlbum: String = "Uncategorized",
    val tags: String = "",
    val syncStatus: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val cloudUrl: String? = null
) {
    fun getTagsList(): List<String> = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
}

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val type: String // INFO, SUCCESS, WARNING, ERROR
)

@Entity(tableName = "custom_albums")
data class CustomAlbum(
    @PrimaryKey val name: String,
    val timestamp: Long = System.currentTimeMillis()
)
