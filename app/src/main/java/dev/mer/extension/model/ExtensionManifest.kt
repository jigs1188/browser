package dev.mer.extension.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the parsed manifest.json of an extension.
 * Uses kotlinx-serialization for JSON deserialization.
 */
@Serializable
data class ExtensionManifest(
    @SerialName("manifest_version")
    val manifestVersion: Int = 1,

    val name: String,
    val version: String,
    val description: String = "",

    val matches: List<String>,

    val scripts: List<String> = emptyList(),
    val styles: List<String> = emptyList(),

    val permissions: List<String> = emptyList(),

    @SerialName("run_at")
    val runAt: String = "document_end"
)
