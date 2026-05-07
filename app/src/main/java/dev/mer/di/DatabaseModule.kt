package dev.mer.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.mer.storage.AppDatabase
import dev.mer.storage.BookmarkDao
import dev.mer.storage.ExtensionDao
import dev.mer.storage.HistoryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mer_database"
        )
            .fallbackToDestructiveMigration()  // MVP: OK to lose data on schema change
            .build()
    }

    @Provides
    fun provideExtensionDao(database: AppDatabase): ExtensionDao {
        return database.extensionDao()
    }

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao {
        return database.bookmarkDao()
    }
}
