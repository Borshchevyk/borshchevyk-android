package ru.kubsu.borshchevyk.core.network.user

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.database.dao.PrivacySettingsDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.PrivacySettingsEntity
import ru.kubsu.borshchevyk.core.database.entity.UserEntity
import ru.kubsu.borshchevyk.core.model.domain.Visibility
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.PrivacySettingsResponse
import ru.kubsu.borshchevyk.core.network.dto.UpdateAvatarRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdatePrivacySettingsRequest
import ru.kubsu.borshchevyk.core.network.dto.UpdateProfileRequest
import ru.kubsu.borshchevyk.core.network.dto.UserProfileResponse
import ru.kubsu.borshchevyk.core.network.mesh.MeshMediaTransferManager
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import java.io.File
import javax.inject.Inject

/**
 * Mesh-specific implementation of [UserNetworkDataSource].
 *
 * In Mesh mode, user data and privacy settings are predominantly local.
 * This implementation updates the local database directly for "me" operations
 * and broadcasts updates to the Mesh network via [MeshProfileBroadcaster].
 */
class MeshUserNetworkDataSource @Inject constructor(
    private val userDao: UserDao,
    private val privacySettingsDao: PrivacySettingsDao,
    private val signatureService: MeshSignatureService,
    private val profileBroadcaster: MeshProfileBroadcaster,
    private val meshMediaTransferManager: MeshMediaTransferManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserNetworkDataSource {

    override suspend fun getUserProfile(userIdOrTag: String): NetworkResult<UserProfileResponse> {
        return withContext(ioDispatcher) {
            val user = userDao.getUser(userIdOrTag)
            if (user != null) {
                NetworkResult.Success(user.toResponse())
            } else {
                NetworkResult.Exception(Exception("User $userIdOrTag not found in local mesh cache"))
            }
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): NetworkResult<UserProfileResponse> {
        return withContext(ioDispatcher) {
            val localUserId = signatureService.getUserId() ?: "self"
            val user = userDao.getUser(localUserId) ?: UserEntity(
                userId = localUserId,
                email = null,
                tag = localUserId,
                firstName = null,
                lastName = null,
                bio = null,
                avatarUrl = null,
                avatars = emptyList()
            )
            
            val updatedUser = user.copy(
                firstName = request.firstName ?: user.firstName,
                lastName = request.lastName ?: user.lastName,
                bio = request.bio ?: user.bio,
                avatarUrl = request.avatarUrl ?: user.avatarUrl
            )
            
            userDao.upsertUser(updatedUser)
            
            // Broadcast updated profile to Mesh network
            profileBroadcaster.broadcastLocalProfile()
            
            NetworkResult.Success(updatedUser.toResponse())
        }
    }

    override suspend fun updateAvatar(request: UpdateAvatarRequest): NetworkResult<UserProfileResponse> {
        return withContext(ioDispatcher) {
            val localUserId = signatureService.getUserId() ?: "self"
            val user = userDao.getUser(localUserId)
            if (user != null) {
                val avatarUrl = request.avatarUrl
                
                // If it's a local file, register it for P2P sharing
                if (avatarUrl.startsWith("/")) {
                    val file = File(avatarUrl)
                    if (file.exists()) {
                        val attachmentId = "avatar_${localUserId}_${System.currentTimeMillis()}"
                        meshMediaTransferManager.shareLocalFile(
                            attachmentId = attachmentId,
                            file = file,
                            contentType = "image/jpeg", // Assuming JPEG for avatars
                            originalFilename = file.name
                        )
                        // Update avatarUrl to the mesh-specific attachment ID
                        val updatedUser = user.copy(avatarUrl = attachmentId)
                        userDao.upsertUser(updatedUser)
                        profileBroadcaster.broadcastLocalProfile()
                        return@withContext NetworkResult.Success(updatedUser.toResponse())
                    }
                }

                val updatedUser = user.copy(avatarUrl = avatarUrl)
                userDao.upsertUser(updatedUser)
                
                // Broadcast updated profile to Mesh network
                profileBroadcaster.broadcastLocalProfile()
                
                NetworkResult.Success(updatedUser.toResponse())
            } else {
                NetworkResult.Exception(Exception("Current user profile not found for avatar update"))
            }
        }
    }

    override suspend fun getPrivacySettings(): NetworkResult<PrivacySettingsResponse> {
        return withContext(ioDispatcher) {
            val localUserId = signatureService.getUserId() ?: "self"
            val settings = privacySettingsDao.getPrivacySettings(localUserId)
            if (settings != null) {
                NetworkResult.Success(
                    PrivacySettingsResponse(
                        userId = settings.userId,
                        emailVisibility = settings.emailVisibility,
                        searchByEmailVisibility = settings.searchByEmailVisibility,
                        profilePhotoVisibility = settings.profilePhotoVisibility,
                        inviteToChatVisibility = settings.inviteToChatVisibility
                    )
                )
            } else {
                NetworkResult.Success(
                    PrivacySettingsResponse(
                        userId = localUserId,
                        emailVisibility = Visibility.NOBODY,
                        searchByEmailVisibility = Visibility.EVERYONE,
                        profilePhotoVisibility = Visibility.EVERYONE,
                        inviteToChatVisibility = Visibility.EVERYONE
                    )
                )
            }
        }
    }

    override suspend fun updatePrivacySettings(request: UpdatePrivacySettingsRequest): NetworkResult<PrivacySettingsResponse> {
        return withContext(ioDispatcher) {
            val localUserId = signatureService.getUserId() ?: "self"
            val current = privacySettingsDao.getPrivacySettings(localUserId)
            
            val updated = PrivacySettingsEntity(
                userId = localUserId,
                emailVisibility = request.emailVisibility ?: current?.emailVisibility ?: Visibility.NOBODY,
                searchByEmailVisibility = request.searchByEmailVisibility ?: current?.searchByEmailVisibility ?: Visibility.EVERYONE,
                profilePhotoVisibility = request.profilePhotoVisibility ?: current?.profilePhotoVisibility ?: Visibility.EVERYONE,
                inviteToChatVisibility = request.inviteToChatVisibility ?: current?.inviteToChatVisibility ?: Visibility.EVERYONE
            )
            
            privacySettingsDao.upsertPrivacySettings(updated)
            NetworkResult.Success(
                PrivacySettingsResponse(
                    userId = updated.userId,
                    emailVisibility = updated.emailVisibility,
                    searchByEmailVisibility = updated.searchByEmailVisibility,
                    profilePhotoVisibility = updated.profilePhotoVisibility,
                    inviteToChatVisibility = updated.inviteToChatVisibility
                )
            )
        }
    }

    private fun UserEntity.toResponse() = UserProfileResponse(
        userId = userId,
        email = email,
        tag = tag,
        firstName = firstName,
        lastName = lastName,
        bio = bio,
        avatarUrl = avatarUrl,
        avatars = avatars
    )
}
