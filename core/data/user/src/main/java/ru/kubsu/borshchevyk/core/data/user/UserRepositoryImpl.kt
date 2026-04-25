package ru.kubsu.borshchevyk.core.data.user

import ru.kubsu.borshchevyk.core.domain.user.UserRepository
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.model.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.model.dto.UserProfileResponse
import ru.kubsu.borshchevyk.core.network.user.UserNetworkDataSource
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val networkDataSource: UserNetworkDataSource
) : UserRepository {

    private val userCache = mutableMapOf<String, User>()

    override suspend fun searchUsers(query: String): List<User> {
        val users = networkDataSource.searchUsers(query).map { it.toDomain() }
        users.forEach { userCache[it.userId] = it }
        return users
    }

    override suspend fun getUserProfile(userIdOrTag: String): User {
        // Simple cache hit check
        userCache[userIdOrTag]?.let { return it }
        
        val user = networkDataSource.getUserProfile(userIdOrTag).toDomain()
        userCache[user.userId] = user
        userCache[user.tag] = user
        return user
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): User {
        val user = networkDataSource.updateProfile(request).toDomain()
        userCache[user.userId] = user
        return user
    }

    override suspend fun updateAvatar(request: ru.kubsu.borshchevyk.core.model.dto.UpdateAvatarRequest): User {
        val user = networkDataSource.updateAvatar(request).toDomain()
        userCache[user.userId] = user
        return user
    }

    override suspend fun getPrivacySettings(): PrivacySettings {
        return networkDataSource.getPrivacySettings().toDomain()
    }

    override suspend fun updatePrivacySettings(request: UpdatePrivacySettingsRequest): PrivacySettings {
        return networkDataSource.updatePrivacySettings(request).toDomain()
    }

    private fun UserProfileResponse.toDomain(): User = User(
        userId = userId,
        email = email,
        tag = tag,
        firstName = firstName,
        lastName = lastName,
        bio = bio,
        avatarUrl = avatarUrl,
        avatars = avatars
    )

    private fun PrivacySettingsResponse.toDomain(): PrivacySettings = PrivacySettings(
        userId = userId,
        emailVisibility = emailVisibility,
        searchByEmailVisibility = searchByEmailVisibility,
        profilePhotoVisibility = profilePhotoVisibility,
        inviteToChatVisibility = inviteToChatVisibility
    )
}
