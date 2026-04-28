package ru.kubsu.borshchevyk.core.domain.user

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateAvatarParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePrivacySettingsParam
import ru.kubsu.borshchevyk.core.model.domain.DomainUpdateProfileParam
import ru.kubsu.borshchevyk.core.model.domain.PrivacySettings
import ru.kubsu.borshchevyk.core.model.domain.User

interface UserRepository {
    suspend fun searchUsers(query: String): List<User>
    fun observeUserProfile(userId: String): Flow<User?>
    suspend fun syncUserProfile(userIdOrTag: String)
    suspend fun getUserProfile(userIdOrTag: String): User
    suspend fun updateProfile(request: DomainUpdateProfileParam): User
    suspend fun updateAvatar(request: DomainUpdateAvatarParam): User
    suspend fun getPrivacySettings(): PrivacySettings
    suspend fun updatePrivacySettings(request: DomainUpdatePrivacySettingsParam): PrivacySettings
}
