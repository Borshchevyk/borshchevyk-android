package ru.kubsu.borshchevyk.core.network.message

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.di.ApplicationScope
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.EditMessageEvent
import ru.kubsu.borshchevyk.core.network.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.EnrichedUserResponse
import ru.kubsu.borshchevyk.core.network.dto.MessageAttachmentResponse
import ru.kubsu.borshchevyk.core.network.dto.MessageResponse
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.network.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.ShortChatDto
import ru.kubsu.borshchevyk.core.network.dto.ShortUserDto
import ru.kubsu.borshchevyk.core.network.media.MeshMediaNetworkDataSource
import ru.kubsu.borshchevyk.core.network.mesh.E2EEPayload
import ru.kubsu.borshchevyk.core.network.mesh.MeshFloodingProtocol
import ru.kubsu.borshchevyk.core.network.mesh.MeshSignatureService
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope as GossipEnvelope

class MeshMessageNetworkDataSource @Inject constructor(
    private val json: Json,
    private val gossipProtocol: MeshFloodingProtocol,
    private val mediaDataSource: MeshMediaNetworkDataSource,
    private val signatureService: MeshSignatureService,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MessageNetworkDataSource {

    private val messageReadersCache = java.util.concurrent.ConcurrentHashMap<String, MutableSet<EnrichedUserResponse>>()

    private suspend fun encryptPayloadIfNeeded(chatId: String, payloadString: String): String {
        val partnerPubKey = signatureService.getPartnerPublicKeyForChat(chatId) ?: return payloadString
        val e2eePayload = signatureService.encryptE2EE(payloadString.toByteArray(Charsets.UTF_8), partnerPubKey)
        return if (e2eePayload != null) {
            json.encodeToString(e2eePayload)
        } else {
            payloadString
        }
    }

    private suspend fun decryptPayloadIfNeeded(payload: String): String {
        return if (payload.contains("\"encryptedSessionKey\"") && payload.contains("\"encryptedData\"")) {
            try {
                val e2eePayload = json.decodeFromString<E2EEPayload>(payload)
                val decryptedBytes = signatureService.decryptE2EE(e2eePayload)
                decryptedBytes?.let { String(it, Charsets.UTF_8) } ?: payload
            } catch (e: kotlinx.serialization.SerializationException) {
                payload
            } catch (e: IllegalArgumentException) {
                payload
            }
        } else {
            payload
        }
    }

    init {
        gossipProtocol.incomingEnvelopes.onEach { envelope ->
            if (envelope.action == "READ_MESSAGE") {
                try {
                    val decryptedPayloadString = decryptPayloadIfNeeded(envelope.payload)
                    val payload = json.decodeFromString<ReadReceiptEvent>(decryptedPayloadString)
                    val readers = messageReadersCache.getOrPut(payload.messageId) { java.util.concurrent.ConcurrentHashMap.newKeySet() }
                    readers.add(
                        EnrichedUserResponse(
                            id = payload.user.id
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.launchIn(scope)
    }

    suspend fun broadcastUserProfile(profile: EnrichedUserResponse) {
        withContext(ioDispatcher) {
            val payloadString = json.encodeToString(profile)
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "", // Filled by GossipProtocol
                action = "USER_PROFILE",
                payload = payloadString
            )
            gossipProtocol.broadcast(envelope)
        }
    }

    override suspend fun sendMessage(chatId: String, request: SendMessageRequest): NetworkResult<MessageResponse> {
        return withContext(ioDispatcher) {
            val attachments = request.attachmentIds?.mapNotNull { mediaDataSource.getCachedAttachment(it) } ?: emptyList()
            val userId = signatureService.getUserId() ?: "self"
            
            val messageDto = NotificationDto.MessageDto(
                id = UUID.randomUUID().toString(),
                chat = ShortChatDto(id = chatId, name = ""),
                author = ShortUserDto(id = userId), // Endpoint ID will be filled by GossipProtocol but DTO needs it
                text = request.text,
                createdAt = Instant.now().toString(),
                attachments = attachments.map {
                    MessageAttachmentResponse(
                        id = it.id,
                        type = it.type,
                        originalFilename = it.originalFilename,
                        extension = it.extension,
                        sizeBytes = it.sizeBytes
                    )
                },
                status = "SENT"
            )
            val payloadString = json.encodeToString(messageDto)

            val finalPayload = encryptPayloadIfNeeded(chatId, payloadString)

            val envelope = GossipEnvelope(
                envelopeId = messageDto.id,
                originEndpointId = "", // Filled by GossipProtocol
                action = "SEND_MESSAGE",
                payload = finalPayload
            )
            gossipProtocol.broadcast(envelope)
            
            val response = MessageResponse(
                id = messageDto.id,
                chat = messageDto.chat,
                author = messageDto.author,
                text = messageDto.text ?: "",
                createdAt = messageDto.createdAt,
                source = MessageSource.OFFLINE,
                status = null,
                attachments = messageDto.attachments,
                parentMessageId = request.parentMessageId,
                reactions = emptyList()
            )
            NetworkResult.Success(response)
        }
    }

    override suspend fun editMessage(chatId: String, messageId: String, request: EditMessageRequest): NetworkResult<MessageResponse> {
        return withContext(ioDispatcher) {
            val userId = signatureService.getUserId() ?: "self"
            val event = EditMessageEvent(
                messageId = messageId,
                text = request.text
            )
            val payloadString = json.encodeToString(event)

            val finalPayload = encryptPayloadIfNeeded(chatId, payloadString)

            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "",
                action = "EDIT_MESSAGE",
                payload = finalPayload
            )
            gossipProtocol.broadcast(envelope)
            
            val response = MessageResponse(
                id = messageId,
                chat = ShortChatDto(id = chatId, name = ""),
                author = ShortUserDto(id = userId),
                text = request.text,
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString(),
                source = MessageSource.OFFLINE,
                status = null,
                attachments = emptyList()
            )
            NetworkResult.Success(response)
        }
    }

    override suspend fun loadChatHistory(chatId: String, page: Int, size: Int): NetworkResult<List<MessageResponse>> {
        return NetworkResult.Success(emptyList()) // Implemented locally or synced differently
    }

    override suspend fun loadChatAttachments(chatId: String, type: String, page: Int, size: Int): NetworkResult<List<MessageResponse>> {
         return NetworkResult.Success(emptyList())
    }

    override suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
             if (forAll) {
                 val payloadString = "{\"messageId\":\"$messageId\",\"forAll\":$forAll}"
                 val finalPayload = encryptPayloadIfNeeded(chatId, payloadString)
                 val envelope = GossipEnvelope(
                     envelopeId = UUID.randomUUID().toString(),
                     originEndpointId = "",
                     action = "DELETE_MESSAGE",
                     payload = finalPayload
                 )
                 gossipProtocol.broadcast(envelope)
             }
             NetworkResult.Success(Unit)
        }
    }

    override suspend fun addReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val reactionEvent = ReactionEvent(
                messageId = messageId,
                user = ShortUserDto(id = "self"), // Endpoint ID will be filled later or by recipient
                reaction = reaction,
                isAdded = true
            )
            val payloadString = json.encodeToString(reactionEvent)
            val finalPayload = encryptPayloadIfNeeded(chatId, payloadString)
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "",
                action = "ADD_REACTION",
                payload = finalPayload
            )
            gossipProtocol.broadcast(envelope)
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun removeReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val reactionEvent = ReactionEvent(
                messageId = messageId,
                user = ShortUserDto(id = "self"),
                reaction = reaction,
                isAdded = false
            )
            val payloadString = json.encodeToString(reactionEvent)
            val finalPayload = encryptPayloadIfNeeded(chatId, payloadString)
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "",
                action = "REMOVE_REACTION",
                payload = finalPayload
            )
            gossipProtocol.broadcast(envelope)
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun pinMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val finalPayload = encryptPayloadIfNeeded(chatId, messageId)
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "",
                action = "PIN_MESSAGE",
                payload = finalPayload
            )
            gossipProtocol.broadcast(envelope)
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun unpinMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val finalPayload = encryptPayloadIfNeeded(chatId, messageId)
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "",
                action = "UNPIN_MESSAGE",
                payload = finalPayload
            )
            gossipProtocol.broadcast(envelope)
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun getPinnedMessages(chatId: String): NetworkResult<List<MessageResponse>> {
        return NetworkResult.Success(emptyList())
    }

    override suspend fun readMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val readEvent = ReadReceiptEvent(
                user = ShortUserDto(id = "self"),
                messageId = messageId
            )
            val payloadString = json.encodeToString(readEvent)
            val finalPayload = encryptPayloadIfNeeded(chatId, payloadString)
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "",
                action = "READ_MESSAGE",
                payload = finalPayload
            )
            gossipProtocol.broadcast(envelope)
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun getMessageReaders(chatId: String, messageId: String): NetworkResult<List<EnrichedUserResponse>> {
        return NetworkResult.Success(emptyList())
    }

    override suspend fun getMessageComments(
        chatId: String,
        messageId: String,
        page: Int,
        size: Int
    ): NetworkResult<List<MessageResponse>> {
        return NetworkResult.Success(emptyList())
    }
}