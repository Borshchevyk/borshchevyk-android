package ru.kubsu.borshchevyk.core.data.user

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.domain.user.UserRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateAvatarParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePrivacySettingsParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateProfileParam
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.domain.getOrThrow
import ru.kubsu.borshchevyk.core.model.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.model.dto.UpdateAvatarRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.user.UserNetworkDataSource
import javax.inject.Inject

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.util.Log

class UserRepositoryImpl @Inject constructor(
    private val networkDataSource: UserNetworkDataSource,
    private val userDao: UserDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override suspend fun searchUsers(query: String): List<User> {
        val users = networkDataSource.searchUsers(query).getOrThrow().map { it.toEntity() }
        userDao.upsertUsers(users)
        return users.map { it.toDomain() }
    }

    override fun observeUserProfile(userId: String): Flow<User?> = userDao.observeUser(userId).map { it?.toDomain() }

    override suspend fun syncUserProfile(userIdOrTag: String) {
        withContext(ioDispatcher) {
            val userEntity = networkDataSource.getUserProfile(userIdOrTag).getOrThrow().toEntity()
            userDao.upsertUser(userEntity)
        }
    }

    override suspend fun getUserProfile(userIdOrTag: String): User {
        val cached = userDao.getUser(userIdOrTag)?.toDomain()
        if (cached != null) {
            repositoryScope.launch {
                try {
                    syncUserProfile(userIdOrTag)
                } catch (e: Exception) {
                    Log.w("UserRepository", "Failed background sync for user $userIdOrTag, using cache", e)
                }
            }
            return cached
        }
        val userEntity = networkDataSource.getUserProfile(userIdOrTag).getOrThrow().toEntity()
        userDao.upsertUser(userEntity)
        return userEntity.toDomain()
    }

    override suspend fun updateProfile(request: DomainUpdateProfileParam): User {
        val userEntity = networkDataSource.updateProfile(
            UpdateProfileRequest(
                firstName = request.firstName,
                lastName = request.lastName,
                bio = request.bio,
                avatarUrl = request.avatarUrl
            )
        ).getOrThrow().toEntity()
        userDao.upsertUser(userEntity)
        return userEntity.toDomain()
    }

    override suspend fun updateAvatar(request: DomainUpdateAvatarParam): User {
        val userEntity = networkDataSource.updateAvatar(
            UpdateAvatarRequest(avatarUrl = request.avatarUrl)
        ).getOrThrow().toEntity()
        userDao.upsertUser(userEntity)
        return userEntity.toDomain()
    }

    override suspend fun getPrivacySettings(): PrivacySettings {
        return networkDataSource.getPrivacySettings().getOrThrow().toDomain()
    }

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
