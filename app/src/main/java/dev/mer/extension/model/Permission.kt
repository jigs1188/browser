package dev.mer.extension.model

/**
 * Known permissions that extensions can request.
 *
 * MVP implements STORAGE and AI. Others are defined for manifest validation
 * and future use — requesting them currently fails gracefully.
 */
enum class Permission(val key: String) {
    STORAGE("storage"),
    AI("ai"),
    NOTIFICATIONS("notifications"),
    TABS("tabs"),
    CLIPBOARD("clipboard"),
    NETWORK("network");

    companion object {
        private val keyMap = entries.associateBy { it.key }

        fun fromKey(key: String): Permission? = keyMap[key]

        fun isKnown(key: String): Boolean = key in keyMap
    }
}
