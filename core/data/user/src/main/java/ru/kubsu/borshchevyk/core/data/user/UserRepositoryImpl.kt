package ru.kubsu.borshchevyk.core.data.user

import ru.kubsu.borshchevyk.core.domain.user.UserRepository
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.model.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.model.dto.UserProfileResponse
import ru.kubsu.borshchevyk.core.network.UserNetworkDataSource
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val networkDataSource: UserNetworkDataSource
) : UserRepository {

    override suspend fun searchUsers(query: String): List<User> {
        return networkDataSource.searchUsers(query).map { it.toDomain() }
    }

    override suspend fun getUserProfile(userIdOrTag: String): User {
        return networkDataSource.getUserProfile(userIdOrTag).toDomain()
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): User {
        return networkDataSource.updateProfile(request).toDomain()
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
        avatarUrl = avatarUrl
    )

    private fun PrivacySettingsResponse.toDomain(): PrivacySettings = PrivacySettings(
        userId = userId,
        emailVisibility = emailVisibility,
        searchByEmailVisibility = searchByEmailVisibility,
        profilePhotoVisibility = profilePhotoVisibility,
        inviteToChatVisibility = inviteToChatVisibility
    )
}
