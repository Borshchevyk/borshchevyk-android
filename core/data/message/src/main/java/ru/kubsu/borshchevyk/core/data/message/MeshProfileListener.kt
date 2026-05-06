package ru.kubsu.borshchevyk.core.data.message

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.database.dao.PublicKeyDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.PublicKeyEntity
import ru.kubsu.borshchevyk.core.database.entity.UserEntity
import ru.kubsu.borshchevyk.core.network.di.ApplicationScope
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.EnrichedUserResponse
import ru.kubsu.borshchevyk.core.network.mesh.MeshConnectionManager
import ru.kubsu.borshchevyk.core.network.mesh.MeshFloodingProtocol
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshProfileListener @Inject constructor(
    private val gossipProtocol: MeshFloodingProtocol,
    private val userDao: UserDao,
    private val publicKeyDao: PublicKeyDao,
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
                    Log.d(TAG, "INCOMING USER_PROFILE: ${envelope.payload}")
                    try {
                        val payload = json.decodeFromString<EnrichedUserResponse>(envelope.payload)
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
                            
                            // Save the public key if provided
                            val incomingPubKey = payload.publicKey
                            if (!incomingPubKey.isNullOrBlank()) {
                                publicKeyDao.insertPublicKey(PublicKeyEntity(payload.id, incomingPubKey))
                                Log.d(TAG, "Saved public key for mesh user: ${payload.id}")
                            }
                            
                            Log.d(TAG, "Saved mesh user profile to DB: ID=${payload.id}, Tag=${payload.tag}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse USER_PROFILE. Payload was: ${envelope.payload}", e)
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
            val localUser = userDao.getUser(userId)
            val localPubKey = signatureService.getLocalPublicKey()
            
            // If the user hasn't synced with the DB yet, we still must send our tag and pubkey
            // so others can verify our signatures and find us in search!
            val fallbackTag = if (userId.startsWith("offline_user_")) userId.removePrefix("offline_user_") else "Unknown"

            val profilePayload = EnrichedUserResponse(
                id = userId,
                firstName = localUser?.firstName,
                lastName = localUser?.lastName,
                tag = localUser?.tag ?: fallbackTag,
                avatarUrl = localUser?.avatarUrl,
                publicKey = localPubKey
            )
            
            val payloadString = json.encodeToString(profilePayload)
            Log.d(TAG, "OUTGOING USER_PROFILE: $payloadString")
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
