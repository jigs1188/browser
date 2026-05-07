package dev.mer.extension.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Domain model and Room entity for an installed extension.
 *
 * We use a single class as both domain model and entity for the MVP.
 * This avoids the mapper boilerplate of separate domain/data models
 * until the complexity justifies the separation.
 */
@Entity(tableName = "extensions")
data class Extension(
    @PrimaryKey
    val id: String,

    val name: String,
    val version: String,
    val description: String,

    /** Serialized JSON of the matches array */
    val matchPatterns: String,

    /** Serialized JSON of the scripts array */
    val scripts: String,

    /** Serialized JSON of the styles array */
    val styles: String,

    /** Serialized JSON of the permissions array */
    val permissions: String,

    /** "document_start" or "document_end" */
    val runAt: String,

    /** Absolute path to extension directory in internal storage */
    val directoryPath: String,

    /** Whether the user has enabled this extension */
    val isEnabled: Boolean = false,

    /** Timestamp of installation */
    val installedAt: Long = System.currentTimeMillis()
)
