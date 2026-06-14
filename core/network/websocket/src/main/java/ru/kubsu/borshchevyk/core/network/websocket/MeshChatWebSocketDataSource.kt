package ru.kubsu.borshchevyk.core.network.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.database.dao.PendingEnvelopeDao
import ru.kubsu.borshchevyk.core.database.entity.PendingEnvelopeEntity
import ru.kubsu.borshchevyk.core.network.dto.EditMessageEvent
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.network.dto.ShortChatDto
import ru.kubsu.borshchevyk.core.network.dto.ShortUserDto
import ru.kubsu.borshchevyk.core.network.dto.TypingEvent
import ru.kubsu.borshchevyk.core.network.mesh.E2EEPayload
import ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope
import ru.kubsu.borshchevyk.core.network.mesh.MeshFloodingProtocol
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshChatWebSocketDataSource @Inject constructor(
    private val gossipProtocol: MeshFloodingProtocol,
    private val json: Json,
    private val signatureService: MeshSignatureService,
    private val pendingEnvelopeDao: PendingEnvelopeDao
) : ChatWebSocketDataSource {

    class NotForMeException : Exception("E2EE payload decryption failed. Envelope is likely not intended for this node.")

    private suspend fun decryptPayloadIfNeeded(payload: String): String {
        return if (payload.contains("\"encryptedSessionKey\"") && payload.contains("\"encryptedData\"")) {
            try {
                val e2eePayload = json.decodeFromString<E2EEPayload>(payload)
                val decryptedBytes = signatureService.decryptE2EE(e2eePayload)
                if (decryptedBytes == null) {
                    throw NotForMeException()
                }
                String(decryptedBytes, Charsets.UTF_8)
            } catch (e: kotlinx.serialization.SerializationException) {
                payload
            } catch (e: IllegalArgumentException) {
                payload
            }
        } else {
            payload
        }
    }

    private suspend fun saveAsPending(envelope: MeshEnvelope) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            pendingEnvelopeDao.insert(
                PendingEnvelopeEntity(
                    envelopeId = envelope.envelopeId,
                    originEndpointId = envelope.originEndpointId,
                    action = envelope.action,
                    payload = envelope.payload,
                    signature = envelope.signature
                )
            )
        }
    }

    override fun observeNewMessages(): Flow<NotificationDto.MessageDto> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "SEND_MESSAGE" || it.action == "EDIT_MESSAGE" }
            .mapNotNull { envelope ->
                try {
                    val decryptedPayloadString = decryptPayloadIfNeeded(envelope.payload)
                    if (envelope.action == "SEND_MESSAGE") {
                        val messageDto = json.decodeFromString<NotificationDto.MessageDto>(decryptedPayloadString)
                        messageDto.copy(
                            author = ShortUserDto(id = envelope.originEndpointId),
                            status = "RECEIVED_BY_USER",
                            source = "OFFLINE"
                        )
                    } else {
                        val editEvent = json.decodeFromString<EditMessageEvent>(decryptedPayloadString)
                        NotificationDto.MessageDto(
                            id = editEvent.messageId,
                            chat = ShortChatDto(id = "mesh_chat", name = ""), // Repository handles updating existing by ID
                            author = ShortUserDto(id = envelope.originEndpointId),
                            text = editEvent.text,
                            createdAt = Instant.now().toString(),
                            updatedAt = Instant.now().toString(),
                            status = "RECEIVED_BY_USER",
                            source = "OFFLINE"
                        )
                    }
                } catch (e: NotForMeException) {
                    null // Skip buffering, not meant for us
                } catch (e: Exception) {
                    android.util.Log.e("MeshChatWebSocket", "Failed to parse message event. Saving as pending: ${e.message}")
                    saveAsPending(envelope)
                    null
                }
            }
    }

    override fun observeChatEvents(): Flow<NotificationDto.ChatEventDto> {
        return gossipProtocol.incomingEnvelopes
            .filter { 
                it.action in setOf("UPDATE_CHAT", "HISTORY_CLEARED", "DELETED", "INVITE_USER", "KICK_USER") 
            }
            .mapNotNull { envelope ->
                try {
                    val decryptedPayloadString = decryptPayloadIfNeeded(envelope.payload)
                    val chatEvent = json.decodeFromString<NotificationDto.ChatEventDto>(decryptedPayloadString)
                    if (chatEvent.chat.type == ru.kubsu.borshchevyk.core.model.domain.ChatType.PRIVATE) {
                        // The sender correctly inverted the partnerName and partnerAvatarUrl before broadcasting,
                        // so we only need to securely enforce the partnerId matches the envelope's origin.
                        chatEvent.copy(
                            chat = chatEvent.chat.copy(
                                partnerId = envelope.originEndpointId
                            )
                        )
                    } else {
                        chatEvent
                    }
                } catch (e: NotForMeException) {
                    null // Skip buffering, not meant for us
                } catch (e: Exception) {
                    android.util.Log.e("MeshChatWebSocket", "Failed to parse chat event. Saving as pending: ${e.message}")
                    saveAsPending(envelope)
                    null
                }
            }
    }

    @Serializable
    private data class DeleteMessagePayload(val messageId: String, val forAll: Boolean)

    override fun observeDeletedMessages(): Flow<String> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "DELETE_MESSAGE" }
            .mapNotNull { envelope ->
                try {
                    val decryptedPayloadString = decryptPayloadIfNeeded(envelope.payload)
                    val payload = json.decodeFromString<DeleteMessagePayload>(decryptedPayloadString)
                    if (payload.forAll) payload.messageId else ""
                } catch (e: NotForMeException) {
                    null
                } catch (e: Exception) {
                    saveAsPending(envelope)
                    null
                }
            }
            .filter { it.isNotEmpty() }
    }

    override fun observeTyping(chatId: String): Flow<TypingEvent> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "TYPING" }
            .mapNotNull { envelope ->
                try {
                    val decryptedPayloadString = decryptPayloadIfNeeded(envelope.payload)
                    val typingEvent = json.decodeFromString<TypingEvent>(decryptedPayloadString)
                    typingEvent.copy(user = ShortUserDto(id = envelope.originEndpointId))
                } catch (e: NotForMeException) {
                    null
                } catch (e: Exception) {
                    // Usually typing events don't need persistent buffering, but for consistency we buffer if we can't decode
                    null 
                }
            }
    }

    override fun observeReactions(chatId: String): Flow<ReactionEvent> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "ADD_REACTION" || it.action == "REMOVE_REACTION" }
            .mapNotNull { envelope ->
                try {
                    val decryptedPayloadString = decryptPayloadIfNeeded(envelope.payload)
                    val reactionEvent = json.decodeFromString<ReactionEvent>(decryptedPayloadString)
                    reactionEvent.copy(
                        user = ShortUserDto(id = envelope.originEndpointId),
                        isAdded = envelope.action == "ADD_REACTION"
                    )
                } catch (e: NotForMeException) {
                    null
                } catch (e: Exception) {
                    saveAsPending(envelope)
                    null
                }
            }
    }

    override fun observePins(chatId: String): Flow<String> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "PIN_MESSAGE" }
            .mapNotNull { envelope ->
                try {
                    decryptPayloadIfNeeded(envelope.payload)
                } catch (e: NotForMeException) {
                    null
                } catch (e: Exception) {
                    saveAsPending(envelope)
                    null
                }
            }
    }

    override fun observeUnpins(chatId: String): Flow<String> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "UNPIN_MESSAGE" }
            .mapNotNull { envelope ->
                try {
                    decryptPayloadIfNeeded(envelope.payload)
                } catch (e: NotForMeException) {
                    null
                } catch (e: Exception) {
                    saveAsPending(envelope)
                    null
                }
            }
    }

    override fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "READ_MESSAGE" }
            .mapNotNull { envelope ->
                try {
                    val decryptedPayloadString = decryptPayloadIfNeeded(envelope.payload)
                    val readEvent = json.decodeFromString<ReadReceiptEvent>(decryptedPayloadString)
                    readEvent.copy(user = ShortUserDto(id = envelope.originEndpointId))
                } catch (e: NotForMeException) {
                    null
                } catch (e: Exception) {
                    saveAsPending(envelope)
                    null
                }
            }
    }

    override suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        val event = TypingEvent(user = ShortUserDto(id = "self"), isTyping = isTyping)
        val envelope = MeshEnvelope(
            envelopeId = UUID.randomUUID().toString(),
            originEndpointId = "self",
            action = "TYPING",
            payload = json.encodeToString(event)
        )
        gossipProtocol.broadcast(envelope)
    }

    override suspend fun connect() {
        // Handled globally by MeshConnectionManager
    }

    override suspend fun disconnect() {
        // Handled globally by MeshConnectionManager
    }
}
