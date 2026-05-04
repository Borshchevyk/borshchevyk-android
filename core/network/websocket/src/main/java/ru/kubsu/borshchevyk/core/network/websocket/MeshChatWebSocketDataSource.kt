package ru.kubsu.borshchevyk.core.network.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.network.dto.MeshMessagePayload
import ru.kubsu.borshchevyk.core.network.dto.MessageAttachmentResponse
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.PinMeshPayload
import ru.kubsu.borshchevyk.core.network.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.network.dto.ReactionMeshPayload
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptMeshPayload
import ru.kubsu.borshchevyk.core.network.dto.ShortChatDto
import ru.kubsu.borshchevyk.core.network.dto.ShortUserDto
import ru.kubsu.borshchevyk.core.network.dto.TypingEvent
import ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope
import ru.kubsu.borshchevyk.core.network.mesh.MeshGossipProtocol
import ru.kubsu.borshchevyk.core.network.dto.UpdateChatMeshPayload
import ru.kubsu.borshchevyk.core.network.dto.DeleteChatMeshPayload
import ru.kubsu.borshchevyk.core.network.dto.ClearHistoryMeshPayload
import ru.kubsu.borshchevyk.core.network.dto.InviteUserMeshPayload
import ru.kubsu.borshchevyk.core.network.dto.KickUserMeshPayload
import ru.kubsu.borshchevyk.core.network.dto.ChatResponse
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshChatWebSocketDataSource @Inject constructor(
    private val gossipProtocol: MeshGossipProtocol,
    private val json: Json
) : ChatWebSocketDataSource {

    override fun observeNewMessages(): Flow<NotificationDto.MessageDto> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "SEND_MESSAGE" }
            .map { envelope ->
                val meshPayload = json.decodeFromString<MeshMessagePayload>(envelope.payload)
                NotificationDto.MessageDto(
                    id = envelope.envelopeId,
                    chat = ShortChatDto(id = meshPayload.chatId, name = ""),
                    author = ShortUserDto(id = envelope.originEndpointId),
                    text = meshPayload.request.text,
                    createdAt = Instant.now().toString(),
                    status = "RECEIVED_BY_USER",
                    attachments = meshPayload.attachments?.map {
                        MessageAttachmentResponse(
                            id = it.id,
                            type = it.type,
                            originalFilename = it.originalFilename,
                            extension = it.extension,
                            sizeBytes = it.sizeBytes
                        )
                    }
                )
            }
    }

    override fun observeChatEvents(): Flow<NotificationDto.ChatEventDto> {
        return gossipProtocol.incomingEnvelopes
            .filter { 
                it.action in setOf("UPDATE_CHAT", "CLEAR_HISTORY", "DELETE_CHAT", "INVITE_USER", "KICK_USER") 
            }
            .map { envelope ->
                val chatId = when (envelope.action) {
                    "UPDATE_CHAT" -> json.decodeFromString<UpdateChatMeshPayload>(envelope.payload).chatId
                    "CLEAR_HISTORY" -> json.decodeFromString<ClearHistoryMeshPayload>(envelope.payload).chatId
                    "DELETE_CHAT" -> json.decodeFromString<DeleteChatMeshPayload>(envelope.payload).chatId
                    "INVITE_USER" -> json.decodeFromString<InviteUserMeshPayload>(envelope.payload).chatId
                    "KICK_USER" -> json.decodeFromString<KickUserMeshPayload>(envelope.payload).chatId
                    else -> ""
                }
                
                // Constructing a minimal ChatResponse
                val chat = ChatResponse(
                    id = chatId,
                    type = ChatType.GROUP, // Assuming actions are mostly for groups, but the UI/DB layer usually matches by ID
                    createdAt = Instant.now().toString()
                )
                
                // If it's an UPDATE_CHAT, we could optionally extract the title/description
                // but the UI typically re-fetches or applies the partial update.
                val updatedChat = if (envelope.action == "UPDATE_CHAT") {
                    val payload = json.decodeFromString<UpdateChatMeshPayload>(envelope.payload)
                    chat.copy(title = payload.title, description = payload.description)
                } else {
                    chat
                }

                NotificationDto.ChatEventDto(
                    chat = updatedChat,
                    action = envelope.action
                )
            }
    }

    @Serializable
    private data class DeleteMessagePayload(val messageId: String, val forAll: Boolean)

    override fun observeDeletedMessages(): Flow<String> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "DELETE_MESSAGE" }
            .map { envelope ->
                try {
                    val payload = json.decodeFromString<DeleteMessagePayload>(envelope.payload)
                    payload.messageId
                } catch (e: Exception) {
                    // Fallback to basic string parsing if json decode fails
                    if (envelope.payload.contains("messageId")) {
                        envelope.payload.substringAfter("\"messageId\":\"").substringBefore("\"")
                    } else {
                        ""
                    }
                }
            }
            .filter { it.isNotEmpty() }
    }

    override fun observeTyping(chatId: String): Flow<TypingEvent> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "TYPING" }
            .map { envelope ->
                val isTyping = envelope.payload.contains("\"isTyping\":true")
                TypingEvent(
                    user = ShortUserDto(id = envelope.originEndpointId),
                    isTyping = isTyping
                )
            }
    }

    override fun observeReactions(chatId: String): Flow<ReactionEvent> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "ADD_REACTION" || it.action == "REMOVE_REACTION" }
            .map { envelope ->
                val payload = json.decodeFromString<ReactionMeshPayload>(envelope.payload)
                val isAdded = envelope.action == "ADD_REACTION"
                ReactionEvent(
                    messageId = payload.messageId,
                    reaction = payload.reaction,
                    user = ShortUserDto(id = envelope.originEndpointId),
                    isAdded = isAdded
                )
            }
    }

    override fun observePins(chatId: String): Flow<String> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "PIN_MESSAGE" }
            .map { envelope ->
                val payload = json.decodeFromString<PinMeshPayload>(envelope.payload)
                payload.messageId
            }
    }

    override fun observeUnpins(chatId: String): Flow<String> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "UNPIN_MESSAGE" }
            .map { envelope ->
                val payload = json.decodeFromString<PinMeshPayload>(envelope.payload)
                payload.messageId
            }
    }

    override fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "READ_MESSAGE" }
            .map { envelope ->
                val payload = json.decodeFromString<ReadReceiptMeshPayload>(envelope.payload)
                ReadReceiptEvent(
                    messageId = payload.messageId,
                    user = ShortUserDto(id = envelope.originEndpointId)
                )
            }
    }

    override suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        val payload = """{"chatId":"$chatId","isTyping":$isTyping}"""
        val envelope = MeshEnvelope(
            envelopeId = UUID.randomUUID().toString(),
            originEndpointId = "self",
            action = "TYPING",
            payload = payload
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
