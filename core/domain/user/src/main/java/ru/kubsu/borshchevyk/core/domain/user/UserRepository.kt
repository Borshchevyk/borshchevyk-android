package ru.kubsu.borshchevyk.core.domain.user

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateAvatarParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePrivacySettingsParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateProfileParam
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User

/**
 * Repository interface for managing user profiles and privacy settings.
 *
 * This repository handles operations related to fetching, observing, and updating
 * user information. It acts as a single source of truth for user data, potentially
 * abstracting away the differences between local caching, remote server API calls,
 * and P2P mesh network synchronization.
 */
interface UserRepository {
    /**
     * Observes the user profile for the given [userId].
     *
     * Returns a [Flow] that emits updates to the user profile whenever the local cache
     * or data source changes. Emits `null` if the user is not found locally.
     *
     * @param userId The unique identifier of the user to observe.
     * @return A [Flow] emitting the [User] profile or `null`.
     */
    fun observeUserProfile(userId: String): Flow<User?>

    /**
     * Triggers a synchronization of the user profile from the remote source.
     *
     * This method fetches the latest user profile data using either the user's ID
     * or their unique tag (@username) and updates the local cache.
     *
     * @param userIdOrTag The unique identifier or tag of the user.
     */
    suspend fun syncUserProfile(userIdOrTag: String)

    /**
     * Fetches the user profile for the given [userIdOrTag].
     *
     * This method attempts to retrieve the user profile. Depending on the implementation,
     * it might fetch from the local cache first, and if not found, from the remote source.
     *
     * @param userIdOrTag The unique identifier or tag of the user.
     * @return The [User] profile.
     * @throws Exception if the user cannot be found or if a network error occurs.
     */
    suspend fun getUserProfile(userIdOrTag: String): User

    /**
     * Updates the profile information of the current user.
     *
     * @param request The parameters containing the updated profile information.
     * @return The updated [User] profile.
     */
    suspend fun updateProfile(request: DomainUpdateProfileParam): User

    /**
     * Updates the avatar of the current user.
     *
     * @param request The parameters containing the new avatar data (e.g., file path or byte array).
     * @return The updated [User] profile reflecting the new avatar.
     */
    suspend fun updateAvatar(request: DomainUpdateAvatarParam): User
/**
 * Retrieves the privacy settings for the current user.
 *
 * @return The [PrivacySettings] of the user.
 */
suspend fun getPrivacySettings(): PrivacySettings

/**
 * Observes the privacy settings for the given [userId].
 *
 * @param userId The unique identifier of the user.
 * @return A [Flow] emitting the [PrivacySettings] or `null`.
 */
fun observePrivacySettings(userId: String): Flow<PrivacySettings?>

/**
 * Updates the privacy settings of the current user.
...
     *
     * @param request The parameters containing the updated privacy settings.
     * @return The updated [PrivacySettings].
     */
    suspend fun updatePrivacySettings(request: DomainUpdatePrivacySettingsParam): PrivacySettings
}
