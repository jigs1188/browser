package dev.mer.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.mer.extension.model.Extension
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtensionDao {

    @Query("SELECT * FROM extensions ORDER BY installedAt DESC")
    fun observeAll(): Flow<List<Extension>>

    @Query("SELECT * FROM extensions WHERE isEnabled = 1")
    fun observeEnabled(): Flow<List<Extension>>

    @Query("SELECT * FROM extensions WHERE isEnabled = 1")
    suspend fun getEnabled(): List<Extension>

    @Query("SELECT * FROM extensions WHERE id = :id")
    suspend fun getById(id: String): Extension?

    @Query("SELECT * FROM extensions WHERE id = :id")
    fun observeById(id: String): Flow<Extension?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(extension: Extension)

    @Update
    suspend fun update(extension: Extension)

    @Delete
    suspend fun delete(extension: Extension)

    @Query("UPDATE extensions SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}
