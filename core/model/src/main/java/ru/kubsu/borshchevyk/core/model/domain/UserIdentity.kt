package ru.kubsu.borshchevyk.core.model.domain

/**
 * Domain model representing the current user's authenticated identity.
 *
 * @property userId the unique server identifier (UUID), if available
 * @property email the user's email address (only present in Online mode)
 * @property tag the user's public display name / unique tag
 * @property mode the authentication mode the user is currently operating in
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
