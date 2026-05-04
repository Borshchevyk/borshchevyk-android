package ru.kubsu.borshchevyk.core.network.message

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import ru.kubsu.borshchevyk.core.network.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.EnrichedUserResponse
import ru.kubsu.borshchevyk.core.network.dto.MeshEnvelope
import ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope as GossipEnvelope
import ru.kubsu.borshchevyk.core.network.dto.MeshMessagePayload
import ru.kubsu.borshchevyk.core.network.dto.MessageResponse
import ru.kubsu.borshchevyk.core.network.dto.ReactionMeshPayload
import ru.kubsu.borshchevyk.core.network.dto.PinMeshPayload
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptMeshPayload
import ru.kubsu.borshchevyk.core.network.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.ShortChatDto
import ru.kubsu.borshchevyk.core.network.dto.ShortUserDto
import ru.kubsu.borshchevyk.core.network.dto.MessageAttachmentResponse
import ru.kubsu.borshchevyk.core.network.media.MeshMediaNetworkDataSource
import ru.kubsu.borshchevyk.core.network.mesh.MeshGossipProtocol
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class MeshMessageNetworkDataSource @Inject constructor(
    private val json: Json,
    private val gossipProtocol: MeshGossipProtocol,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val mediaDataSource: MeshMediaNetworkDataSource
) : MessageNetworkDataSource {

    private val messageReadersCache = java.util.concurrent.ConcurrentHashMap<String, MutableSet<EnrichedUserResponse>>()

    init {
        kotlinx.coroutines.CoroutineScope(ioDispatcher + kotlinx.coroutines.SupervisorJob()).launch {
            gossipProtocol.incomingEnvelopes.collect { envelope ->
                if (envelope.action == "READ_MESSAGE") {
                    try {
                        val payload = json.decodeFromString<ReadReceiptMeshPayload>(envelope.payload)
                        val readers = messageReadersCache.getOrPut(payload.messageId) { java.util.concurrent.ConcurrentHashMap.newKeySet() }
                        readers.add(
                            EnrichedUserResponse(
                                id = envelope.originEndpointId,
                                firstName = "Mesh User" // Could be enriched if we had a user cache
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override suspend fun sendMessage(chatId: String, request: SendMessageRequest): NetworkResult<MessageResponse> {
        return withContext(ioDispatcher) {
            val attachments = request.attachmentIds?.mapNotNull { mediaDataSource.getCachedAttachment(it) } ?: emptyList()
            val meshPayload = MeshMessagePayload(chatId, request, attachments)
            val payloadString = json.encodeToString(meshPayload)
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "", // Filled by GossipProtocol
                action = "SEND_MESSAGE",
                payload = payloadString
            )
            gossipProtocol.broadcast(envelope)
            
            val response = MessageResponse(
                id = envelope.envelopeId,
                chat = ShortChatDto(id = chatId, name = "Mesh Chat"),
                author = ShortUserDto(id = "self", firstName = "Me"),
                text = request.text,
                createdAt = Instant.now().toString(),
                source = MessageSource.OFFLINE,
                status = null,
                attachments = attachments.map {
                    MessageAttachmentResponse(
                        id = it.id,
                        type = it.type,
                        originalFilename = it.originalFilename,
                        extension = it.extension,
                        sizeBytes = it.sizeBytes
                    )
                },
                parentMessageId = request.parentMessageId,
                reactions = emptyList()
            )
            NetworkResult.Success(response)
        }
    }

    override suspend fun editMessage(chatId: String, messageId: String, request: EditMessageRequest): NetworkResult<MessageResponse> {
        return withContext(ioDispatcher) {
            val payload = json.encodeToString(request)
            val envelope = MeshEnvelope(
                action = "EDIT_MESSAGE",
                payload = payload
            )
            // Broadcast `envelope`
            
            val response = MessageResponse(
                id = messageId,
                chat = ShortChatDto(id = chatId, name = "Mesh Chat"),
                author = ShortUserDto(id = "self", firstName = "Me"),
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
             val envelope = MeshEnvelope(
                 action = "DELETE_MESSAGE",
                 payload = "{\"messageId\":\"$messageId\",\"forAll\":$forAll}"
             )
             // Broadcast envelope
             NetworkResult.Success(Unit)
        }
    }

    override suspend fun addReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val payload = json.encodeToString(ReactionMeshPayload(chatId, messageId, reaction))
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "",
                action = "ADD_REACTION",
                payload = payload
            )
            gossipProtocol.broadcast(envelope)
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun removeReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val payload = json.encodeToString(ReactionMeshPayload(chatId, messageId, reaction))
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "",
                action = "REMOVE_REACTION",
                payload = payload
            )
            gossipProtocol.broadcast(envelope)
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun pinMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val payload = json.encodeToString(PinMeshPayload(chatId, messageId))
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "",
                action = "PIN_MESSAGE",
                payload = payload
            )
            gossipProtocol.broadcast(envelope)
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun unpinMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            val payload = json.encodeToString(PinMeshPayload(chatId, messageId))
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "",
                action = "UNPIN_MESSAGE",
                payload = payload
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
            val payload = json.encodeToString(ReadReceiptMeshPayload(chatId, messageId))
            val envelope = GossipEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                originEndpointId = "",
                action = "READ_MESSAGE",
                payload = payload
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
