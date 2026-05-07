package dev.mer.extension.repository

import dev.mer.extension.model.Extension
import dev.mer.storage.ExtensionDao
import dev.mer.storage.ExtensionFileManager
import dev.mer.storage.ExtensionStorage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for extension data.
 * Coordinates between Room (metadata), file system (assets), and storage (kv data).
 *
 * This is one of the few places where a repository genuinely adds value:
 * uninstall must clean up three separate stores atomically.
 */
@Singleton
class ExtensionRepository @Inject constructor(
    private val dao: ExtensionDao,
    private val fileManager: ExtensionFileManager,
    private val storage: ExtensionStorage
) {
    fun observeAll(): Flow<List<Extension>> = dao.observeAll()

    fun observeEnabled(): Flow<List<Extension>> = dao.observeEnabled()

    suspend fun getEnabled(): List<Extension> = dao.getEnabled()

    suspend fun getById(id: String): Extension? = dao.getById(id)

    fun observeById(id: String): Flow<Extension?> = dao.observeById(id)

    suspend fun install(extension: Extension) {
        dao.insert(extension)
    }

    suspend fun setEnabled(extensionId: String, enabled: Boolean) {
        dao.setEnabled(extensionId, enabled)
    }

    suspend fun uninstall(extension: Extension) {
        dao.delete(extension)
        fileManager.deleteExtension(extension.id)
        storage.clearAll(extension.id)
    }

    suspend fun update(extension: Extension) {
        dao.update(extension)
    }
}
