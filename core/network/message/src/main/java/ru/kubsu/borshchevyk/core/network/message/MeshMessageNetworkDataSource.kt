package ru.kubsu.borshchevyk.core.network.message

import kotlinx.coroutines.CoroutineDispatcher
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
import ru.kubsu.borshchevyk.core.network.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.ShortChatDto
import ru.kubsu.borshchevyk.core.network.dto.ShortUserDto
import ru.kubsu.borshchevyk.core.network.mesh.MeshGossipProtocol
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class MeshMessageNetworkDataSource @Inject constructor(
    private val json: Json,
    private val gossipProtocol: MeshGossipProtocol,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MessageNetworkDataSource {

    override suspend fun sendMessage(chatId: String, request: SendMessageRequest): NetworkResult<MessageResponse> {
        return withContext(ioDispatcher) {
            val meshPayload = MeshMessagePayload(chatId, request)
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
                attachments = emptyList(),
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
        return NetworkResult.Success(Unit)
    }

    override suspend fun removeReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }

    override suspend fun pinMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }

    override suspend fun unpinMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }

    override suspend fun getPinnedMessages(chatId: String): NetworkResult<List<MessageResponse>> {
        return NetworkResult.Success(emptyList())
    }

    override suspend fun readMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
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
