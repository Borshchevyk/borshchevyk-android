package ru.kubsu.borshchevyk.core.data.user

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.PrivacySettingsDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.toDomain
import ru.kubsu.borshchevyk.core.database.entity.toEntity
import ru.kubsu.borshchevyk.core.domain.user.UserRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateAvatarParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePrivacySettingsParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateProfileParam
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.client.TransportModeManager
import ru.kubsu.borshchevyk.core.network.client.getOrThrow
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.network.dto.UpdateAvatarRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.network.user.UserNetworkDataSource
import javax.inject.Inject

/**
 * Implementation of [UserRepository] managing user profile data and privacy settings.
 *
 * Coordinates network requests and local database caching to provide offline-first profile viewing.
 *
 * @property networkDataSource Source for user-related REST API operations.
 * @property userDao Local Room database DAO for caching user entities.
 * @property privacySettingsDao Local Room database DAO for caching privacy settings.
 * @property transportModeManager Manager to determine the current network mode (Mesh vs Global).
 * @property ioDispatcher Coroutine dispatcher for executing I/O bound database and network operations.
 */
class UserRepositoryImpl @Inject constructor(
    private val networkDataSource: UserNetworkDataSource,
    private val userDao: UserDao,
    private val privacySettingsDao: PrivacySettingsDao,
    private val transportModeManager: TransportModeManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override fun observeUserProfile(userId: String): Flow<User?> = userDao.observeUser(userId).map { it?.toDomain() }

    override suspend fun syncUserProfile(userIdOrTag: String) {
        withContext(ioDispatcher) {
            val userEntity = networkDataSource.getUserProfile(userIdOrTag).getOrThrow().toEntity()
            userDao.upsertUser(userEntity)
        }
    }

    override suspend fun getUserProfile(userIdOrTag: String): User = withContext(ioDispatcher) {
        val cached = userDao.getUser(userIdOrTag)?.toDomain()
        if (cached != null) {
            if (transportModeManager.networkMode.value == NetworkMode.GLOBAL) {
                repositoryScope.launch {
                    try {
                        syncUserProfile(userIdOrTag)
                    } catch (e: Exception) {
                        Log.w("UserRepository", "Failed background sync for user $userIdOrTag, using cache", e)
                    }
                }
            }
            return@withContext cached
        }
        val userEntity = networkDataSource.getUserProfile(userIdOrTag).getOrThrow().toEntity()
        userDao.upsertUser(userEntity)
        userEntity.toDomain()
    }

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

    override suspend fun updateAvatar(request: DomainUpdateAvatarParam): User = withContext(ioDispatcher) {
        val userEntity = networkDataSource.updateAvatar(
            UpdateAvatarRequest(avatarUrl = request.avatarUrl)
        ).getOrThrow().toEntity()
        userDao.upsertUser(userEntity)
        userEntity.toDomain()
    }

    override suspend fun getPrivacySettings(): PrivacySettings = withContext(ioDispatcher) {
        val isGlobal = transportModeManager.networkMode.value == NetworkMode.GLOBAL
        
        // In Mesh mode or as a first step, we try to get settings from the network data source
        // (which resolves locally in Mesh mode).
        val networkResponse = networkDataSource.getPrivacySettings().getOrThrow()
        val userId = networkResponse.userId
        
        val cached = privacySettingsDao.getPrivacySettings(userId)?.toDomain()
        if (cached != null) {
            if (isGlobal) {
                repositoryScope.launch {
                    try {
                        syncPrivacySettings()
                    } catch (e: Exception) {
                        Log.w("UserRepository", "Failed background sync for privacy settings, using cache", e)
                    }
                }
            }
            return@withContext cached
        }

        val settings = networkResponse.toDomain()
        privacySettingsDao.upsertPrivacySettings(settings.toEntity())
        settings
    }

    override suspend fun updatePrivacySettings(request: DomainUpdatePrivacySettingsParam): PrivacySettings = withContext(ioDispatcher) {
        val settings = networkDataSource.updatePrivacySettings(
            UpdatePrivacySettingsRequest(
                emailVisibility = request.emailVisibility,
                searchByEmailVisibility = request.searchByEmailVisibility,
                profilePhotoVisibility = request.profilePhotoVisibility,
                inviteToChatVisibility = request.inviteToChatVisibility
            )
        ).getOrThrow().toDomain()
        
        privacySettingsDao.upsertPrivacySettings(settings.toEntity())
        settings
    }

    private suspend fun syncPrivacySettings() {
        val settings = networkDataSource.getPrivacySettings().getOrThrow().toDomain()
        privacySettingsDao.upsertPrivacySettings(settings.toEntity())
    }

    private fun PrivacySettingsResponse.toDomain(): PrivacySettings = PrivacySettings(
        userId = userId,
        emailVisibility = emailVisibility,
        searchByEmailVisibility = searchByEmailVisibility,
        profilePhotoVisibility = profilePhotoVisibility,
        inviteToChatVisibility = inviteToChatVisibility
    )
}
