package ru.kubsu.borshchevyk.core.data.message

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.UserEntity
import ru.kubsu.borshchevyk.core.network.di.ApplicationScope
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.UserProfileMeshPayload
import ru.kubsu.borshchevyk.core.network.mesh.MeshGossipProtocol
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.encodeToString
import ru.kubsu.borshchevyk.core.network.mesh.MeshConnectionManager
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import java.util.UUID

@Singleton
class MeshProfileListener @Inject constructor(
    private val gossipProtocol: MeshGossipProtocol,
    private val userDao: UserDao,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope,
    private val meshConnectionManager: MeshConnectionManager,
    private val signatureService: MeshSignatureService
) {
    private val TAG = "MeshProfileListener"

    fun startListening() {
        gossipProtocol.incomingEnvelopes
            .onEach { envelope ->
                if (envelope.action == "USER_PROFILE") {
                    try {
                        val payload = json.decodeFromString<UserProfileMeshPayload>(envelope.payload)
                        withContext(ioDispatcher) {
                            val existingUser = userDao.getUser(payload.id)
                            userDao.upsertUser(
                                UserEntity(
                                    userId = payload.id,
                                    email = existingUser?.email,
                                    tag = payload.tag ?: existingUser?.tag ?: "user_${payload.id.take(4)}",
                                    firstName = payload.firstName ?: existingUser?.firstName,
                                    lastName = payload.lastName ?: existingUser?.lastName,
                                    bio = existingUser?.bio,
                                    avatarUrl = payload.avatarUrl ?: existingUser?.avatarUrl,
                                    avatars = existingUser?.avatars ?: emptyList()
                                )
                            )
                            Log.d(TAG, "Saved mesh user profile: ${payload.id}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse USER_PROFILE", e)
                    }
                }
            }
            .launchIn(scope)

        meshConnectionManager.connectedEndpoints
            .onEach { endpoints ->
                if (endpoints.isNotEmpty()) {
                    broadcastLocalProfile()
                }
            }
            .launchIn(scope)
    }

    private suspend fun broadcastLocalProfile() {
        withContext(ioDispatcher) {
            val userId = signatureService.getUserId() ?: return@withContext
            val localUser = userDao.getUser(userId) ?: return@withContext
            
            val profilePayload = UserProfileMeshPayload(
                id = localUser.userId,
                firstName = localUser.firstName,
                lastName = localUser.lastName,
                tag = localUser.tag,
                avatarUrl = localUser.avatarUrl
            )
            
            val payloadString = json.encodeToString(profilePayload)
            val envelope = ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "", 
                action = "USER_PROFILE",
                payload = payloadString
            )
            gossipProtocol.broadcast(envelope)
        }
    }
}
