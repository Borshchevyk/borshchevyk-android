package ru.kubsu.borshchevyk.core.network.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.network.dto.EditMessageEvent
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.network.dto.ShortChatDto
import ru.kubsu.borshchevyk.core.network.dto.ShortUserDto
import ru.kubsu.borshchevyk.core.network.dto.TypingEvent
import ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope
import ru.kubsu.borshchevyk.core.network.mesh.MeshFloodingProtocol
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshChatWebSocketDataSource @Inject constructor(
    private val gossipProtocol: MeshFloodingProtocol,
    private val json: Json
) : ChatWebSocketDataSource {

    override fun observeNewMessages(): Flow<NotificationDto.MessageDto> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "SEND_MESSAGE" || it.action == "EDIT_MESSAGE" }
            .map { envelope ->
                if (envelope.action == "SEND_MESSAGE") {
                    val messageDto = json.decodeFromString<NotificationDto.MessageDto>(envelope.payload)
                    messageDto.copy(
                        author = ShortUserDto(id = envelope.originEndpointId),
                        status = "RECEIVED_BY_USER"
                    )
                } else {
                    val editEvent = json.decodeFromString<EditMessageEvent>(envelope.payload)
                    NotificationDto.MessageDto(
                        id = editEvent.messageId,
                        chat = ShortChatDto(id = "mesh_chat", name = ""), // Repository handles updating existing by ID
                        author = ShortUserDto(id = envelope.originEndpointId),
                        text = editEvent.text,
                        createdAt = Instant.now().toString(),
                        updatedAt = Instant.now().toString(),
                        status = "RECEIVED_BY_USER"
                    )
                }
            }
    }

    override fun observeChatEvents(): Flow<NotificationDto.ChatEventDto> {
        return gossipProtocol.incomingEnvelopes
            .filter { 
                it.action in setOf("UPDATE_CHAT", "CLEAR_HISTORY", "DELETE_CHAT", "INVITE_USER", "KICK_USER") 
            }
            .map { envelope ->
                val chatEvent = json.decodeFromString<NotificationDto.ChatEventDto>(envelope.payload)
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
                val typingEvent = json.decodeFromString<TypingEvent>(envelope.payload)
                typingEvent.copy(user = ShortUserDto(id = envelope.originEndpointId))
            }
    }

    override fun observeReactions(chatId: String): Flow<ReactionEvent> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "ADD_REACTION" || it.action == "REMOVE_REACTION" }
            .map { envelope ->
                val reactionEvent = json.decodeFromString<ReactionEvent>(envelope.payload)
                reactionEvent.copy(
                    user = ShortUserDto(id = envelope.originEndpointId),
                    isAdded = envelope.action == "ADD_REACTION"
                )
            }
    }

    override fun observePins(chatId: String): Flow<String> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "PIN_MESSAGE" }
            .map { envelope ->
                // The payload is just the messageId string
                envelope.payload
            }
    }

    override fun observeUnpins(chatId: String): Flow<String> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "UNPIN_MESSAGE" }
            .map { envelope ->
                // The payload is just the messageId string
                envelope.payload
            }
    }

    override fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent> {
        return gossipProtocol.incomingEnvelopes
            .filter { it.action == "READ_MESSAGE" }
            .map { envelope ->
                val readEvent = json.decodeFromString<ReadReceiptEvent>(envelope.payload)
                readEvent.copy(user = ShortUserDto(id = envelope.originEndpointId))
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
