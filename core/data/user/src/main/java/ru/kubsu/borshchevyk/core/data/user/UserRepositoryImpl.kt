package ru.kubsu.borshchevyk.core.data.user

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.domain.user.UserRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateAvatarParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePrivacySettingsParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateProfileParam
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.network.client.getOrThrow
import ru.kubsu.borshchevyk.core.network.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.network.dto.UpdateAvatarRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.user.UserNetworkDataSource
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Implementation of [UserRepository] managing user profile data and privacy settings.
 *
 * Coordinates network requests and local database caching to provide offline-first profile viewing.
 *
 * @property networkDataSource Source for user-related REST API operations.
 * @property userDao Local Room database DAO for caching user entities.
 * @property ioDispatcher Coroutine dispatcher for executing I/O bound database and network operations.
 */
class UserRepositoryImpl @Inject constructor(
    private val networkDataSource: UserNetworkDataSource,
    private val userDao: UserDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    /**
     * Observes the user profile from the local database cache.
     *
     * @param userId The ID of the user.
     * @return A [Flow] emitting the [User] profile, or null if not present locally.
     */
    override fun observeUserProfile(userId: String): Flow<User?> = userDao.observeUser(userId).map { it?.toDomain() }

    /**
     * Synchronizes a user profile from the backend into the local database cache.
     *
     * @param userIdOrTag The ID or tag of the user to sync.
     */
    override suspend fun syncUserProfile(userIdOrTag: String) {
        withContext(ioDispatcher) {
            val userEntity = networkDataSource.getUserProfile(userIdOrTag).getOrThrow().toEntity()
            userDao.upsertUser(userEntity)
        }
    }

    /**
     * Retrieves the user profile. Attempts to fetch from cache first, then triggers a background sync.
     * If not found in cache, fetches directly from the network.
     *
     * @param userIdOrTag The ID or tag of the user to fetch.
     * @return The requested [User] profile.
     */
    override suspend fun getUserProfile(userIdOrTag: String): User = withContext(ioDispatcher) {
        val cached = userDao.getUser(userIdOrTag)?.toDomain()
        if (cached != null) {
            repositoryScope.launch {
                try {
                    syncUserProfile(userIdOrTag)
                } catch (e: Exception) {
                    Log.w("UserRepository", "Failed background sync for user $userIdOrTag, using cache", e)
                }
            }
            return@withContext cached
        }
        val userEntity = networkDataSource.getUserProfile(userIdOrTag).getOrThrow().toEntity()
        userDao.upsertUser(userEntity)
        userEntity.toDomain()
    }

    /**
     * Updates the current user's profile information.
     *
     * @param request Parameters for updating profile, such as name and bio.
     * @return The updated [User] profile.
     */
    override suspend fun updateProfile(request: DomainUpdateProfileParam): User = withContext(ioDispatcher) {
        val userEntity = networkDataSource.updateProfile(
            UpdateProfileRequest(
                firstName = request.firstName,
                lastName = request.lastName,
                bio = request.bio,
                avatarUrl = request.avatarUrl
            )
        ).getOrThrow().toEntity()
        userDao.upsertUser(userEntity)
        userEntity.toDomain()
    }

    /**
     * Updates the current user's avatar URL.
     *
     * @param request Parameters containing the new avatar URL.
     * @return The updated [User] profile.
     */
    override suspend fun updateAvatar(request: DomainUpdateAvatarParam): User = withContext(ioDispatcher) {
        val userEntity = networkDataSource.updateAvatar(
            UpdateAvatarRequest(avatarUrl = request.avatarUrl)
        ).getOrThrow().toEntity()
        userDao.upsertUser(userEntity)
        userEntity.toDomain()
    }

    /**
     * Retrieves the privacy settings for the current user.
     *
     * @return A [PrivacySettings] domain model.
     */
    override suspend fun getPrivacySettings(): PrivacySettings {
        return networkDataSource.getPrivacySettings().getOrThrow().toDomain()
    }

    /**
     * Updates the current user's privacy settings.
     *
     * @param request Parameters specifying the new privacy visibilities.
     * @return The newly updated [PrivacySettings] domain model.
     */
    override suspend fun updatePrivacySettings(request: DomainUpdatePrivacySettingsParam): PrivacySettings {
        return networkDataSource.updatePrivacySettings(
            UpdatePrivacySettingsRequest(
                emailVisibility = request.emailVisibility,
                searchByEmailVisibility = request.searchByEmailVisibility,
                profilePhotoVisibility = request.profilePhotoVisibility,
                inviteToChatVisibility = request.inviteToChatVisibility
            )
        ).getOrThrow().toDomain()
    }

    private fun PrivacySettingsResponse.toDomain(): PrivacySettings = PrivacySettings(
        userId = userId,
        emailVisibility = emailVisibility,
        searchByEmailVisibility = searchByEmailVisibility,
        profilePhotoVisibility = profilePhotoVisibility,
        inviteToChatVisibility = inviteToChatVisibility
    )
}
