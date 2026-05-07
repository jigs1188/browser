package dev.mer.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a saved bookmark.
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val url: String,
    val title: String,
    val addedAt: Long = System.currentTimeMillis()
)
