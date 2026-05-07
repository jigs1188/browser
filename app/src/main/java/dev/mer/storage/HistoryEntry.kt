package dev.mer.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a browsing history entry.
 * Stores each page visit with title, URL, and timestamp.
 */
@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val url: String,
    val title: String,
    val visitedAt: Long = System.currentTimeMillis()
)
