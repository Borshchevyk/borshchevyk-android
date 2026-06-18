package ru.kubsu.borshchevyk.core.data.message

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.database.dao.PendingEnvelopeDao
import ru.kubsu.borshchevyk.core.database.dao.PublicKeyDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.PublicKeyEntity
import ru.kubsu.borshchevyk.core.database.entity.UserEntity
import ru.kubsu.borshchevyk.core.network.di.ApplicationScope
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.EnrichedUserResponse
import ru.kubsu.borshchevyk.core.network.mesh.MeshConnectionManager
import ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope
import ru.kubsu.borshchevyk.core.network.mesh.MeshFloodingProtocol
import ru.kubsu.borshchevyk.core.network.mesh.MeshMediaTransferManager
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import ru.kubsu.borshchevyk.core.network.user.MeshProfileBroadcaster
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshProfileListener @Inject constructor(
    private val gossipProtocol: MeshFloodingProtocol,
    private val userDao: UserDao,
    private val publicKeyDao: PublicKeyDao,
    private val pendingEnvelopeDao: PendingEnvelopeDao,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope,
    private val meshConnectionManager: MeshConnectionManager,
    private val signatureService: MeshSignatureService,
    private val meshMediaTransferManager: MeshMediaTransferManager
) : MeshProfileBroadcaster {
    private val TAG = "MeshProfileListener"

    /**
     * Maps user IDs to the timestamp of the last received profile update
     * to implement Last-Write-Wins (LWW) conflict resolution.
     * Uses ConcurrentHashMap for thread safety across multiple coroutine collections.
     */
    private val lastUpdateTimestamps = ConcurrentHashMap<String, Long>()

    fun startListening() {
        gossipProtocol.incomingEnvelopes
            .onEach { envelope ->
                if (envelope.action == "USER_PROFILE") {
                    Log.d(TAG, "INCOMING USER_PROFILE: ${envelope.payload}")
                    try {
                        val payload = json.decodeFromString<EnrichedUserResponse>(envelope.payload)
                        
                        // LWW: Ignore if we already have a newer or same-time update
                        val lastTimestamp = lastUpdateTimestamps[payload.id] ?: 0L
                        if (payload.timestamp <= lastTimestamp && lastTimestamp != 0L) {
                            Log.d(TAG, "Dropped older profile update for ${payload.id} (Received: ${payload.timestamp}, Current: $lastTimestamp)")
                            return@onEach
                        }

                        // SECURITY: Root of Trust verification
                        val incomingPubKey = payload.publicKey
                        val signature = envelope.signature
                        
                        if (incomingPubKey.isNullOrBlank() || signature == null) {
                            Log.e(TAG, "SECURITY ALERT: USER_PROFILE from ${payload.id} is missing public key or signature. Discarding.")
                            return@onEach
                        }

                        val dataToVerify = envelope.payload.toByteArray(Charsets.UTF_8)
                        val isValidProfile = signatureService.verifySignatureWithKey(incomingPubKey, signature, dataToVerify)
                        
                        if (!isValidProfile) {
                            Log.e(TAG, "SECURITY ALERT: USER_PROFILE from ${payload.id} failed signature verification against its own key. SPOOFING ATTEMPT! Discarding.")
                            return@onEach
                        }

                        withContext(ioDispatcher) {
                            val existingUser = userDao.getUser(payload.id)
                            
                            // Sanitize avatarUrl: remove any potential server URL prefix and normalize to mesh://
                            val sanitizedAvatarUrl = payload.avatarUrl?.let { url ->
                                val id = if (url.contains("mesh://")) url.substringAfter("mesh://")
                                         else url.substringAfter("https://dev.borshchevik.su/")
                                
                                if (id.startsWith("avatar/") || id.startsWith("avatar_")) "mesh://$id" else url
                            }

                            userDao.upsertUser(
                                UserEntity(
                                    userId = payload.id,
                                    email = existingUser?.email,
                                    tag = payload.tag ?: existingUser?.tag ?: "user_${payload.id.take(4)}",
                                    firstName = payload.firstName ?: existingUser?.firstName,
                                    lastName = payload.lastName ?: existingUser?.lastName,
                                    bio = existingUser?.bio,
                                    avatarUrl = sanitizedAvatarUrl, 
                                    avatars = existingUser?.avatars ?: emptyList()
                                )
                            )
                            
                            lastUpdateTimestamps[payload.id] = payload.timestamp
                            
                            // Proactively pull avatar if it's a mesh attachment
                            sanitizedAvatarUrl?.let { avatarUrl ->
                                if (avatarUrl.startsWith("mesh://")) {
                                    val attachmentId = avatarUrl.removePrefix("mesh://")
                                    if (attachmentId.startsWith("avatar/") || attachmentId.startsWith("avatar_")) {
                                        Log.d(TAG, "Proactively pulling avatar: $attachmentId")
                                        meshMediaTransferManager.pullFile(attachmentId)
                                    }
                                }
                            }
                            
                            // Save the securely verified public key
                            publicKeyDao.insertPublicKey(PublicKeyEntity(payload.id, incomingPubKey))
                            Log.d(TAG, "Saved VERIFIED public key for mesh user: ${payload.id}")
                            
                            // Retry pending envelopes for this user
                            val pendingEnvelopes = pendingEnvelopeDao.getPendingEnvelopesForUser(payload.id)
                            if (pendingEnvelopes.isNotEmpty()) {
                                    Log.d(TAG, "Found ${pendingEnvelopes.size} pending envelopes for ${payload.id}. Verifying and replaying...")
                                    pendingEnvelopes.forEach { pending ->
                                        val signature = pending.signature
                                        var isValid = false
                                        if (signature != null) {
                                            val dataToVerify = pending.payload.toByteArray(Charsets.UTF_8)
                                            isValid = signatureService.verifySignature(pending.originEndpointId, signature, dataToVerify)
                                        }

                                        if (isValid) {
                                            gossipProtocol.replayLocalEnvelope(
                                                MeshEnvelope(
                                                    envelopeId = pending.envelopeId,
                                                    originEndpointId = pending.originEndpointId,
                                                    action = pending.action,
                                                    payload = pending.payload,
                                                    signature = signature,
                                                    ttl = 0
                                                )
                                            )
                                        } else {
                                            Log.e(TAG, "SECURITY ALERT: Pending envelope ${pending.envelopeId} failed signature verification after key arrival. Discarding.")
                                        }
                                        pendingEnvelopeDao.delete(pending.envelopeId)
                                    }
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

    /**
     * Broadcasts the current user's profile to the Mesh network.
     * This implements [MeshProfileBroadcaster] for network layer triggers.
     */
    override suspend fun broadcastLocalProfile() {
        withContext(ioDispatcher) {
            val userId = signatureService.getUserId() ?: return@withContext
            val localUser = userDao.getUser(userId)
            val localPubKey = signatureService.getLocalPublicKey()
            
            val fallbackTag = if (userId.startsWith("offline_user_")) userId.removePrefix("offline_user_") else "Unknown"

            val profilePayload = EnrichedUserResponse(
                id = userId,
                firstName = localUser?.firstName,
                lastName = localUser?.lastName,
                tag = localUser?.tag ?: fallbackTag,
                // Sanitize: ensure only the mesh ID part is sent, normalized.
                avatarUrl = localUser?.avatarUrl?.let { url ->
                    val id = if (url.contains("mesh://")) url.substringAfter("mesh://")
                             else url.substringAfter("https://dev.borshchevik.su/")
                    
                    if (id.startsWith("avatar/") || id.startsWith("avatar_")) id else url
                },
                publicKey = localPubKey,
                timestamp = System.currentTimeMillis()
            )
            
            val payloadString = json.encodeToString(profilePayload)
            Log.d(TAG, "OUTGOING USER_PROFILE: $payloadString")
            val envelope = MeshEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "", 
                action = "USER_PROFILE",
                payload = payloadString
            )
            gossipProtocol.broadcast(envelope)
        }
    }
}
