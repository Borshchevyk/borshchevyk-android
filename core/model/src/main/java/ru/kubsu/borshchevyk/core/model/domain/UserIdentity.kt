package ru.kubsu.borshchevyk.core.model.domain

/**
 * Domain model representing the current user's authenticated identity.
 *
 * Encapsulates the core identification details needed throughout the app session.
 *
 * @property userId The unique server identifier (UUID), if available.
 * @property email The user's email address (only present in Online mode).
 * @property tag The user's public display name or unique handle.
 * @property mode The current operating mode of the messenger for this identity.
 */
data class UserIdentity(
    val userId: String,
    val email: String?,
    val tag: String,
    val mode: AuthMode
)

/**
 * Represents the two primary operational modes of the messenger.
 */
enum class AuthMode {
    /**
     * P2P mesh network mode. No central server is used.
     */
    OFFLINE,
    
    /**
     * Standard client-server mode utilizing the global backend.
     */
    ONLINE
}
