package dev.mer.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.mer.extension.model.Extension

@Database(
    entities = [Extension::class, HistoryEntry::class, BookmarkEntry::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun extensionDao(): ExtensionDao
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
}
