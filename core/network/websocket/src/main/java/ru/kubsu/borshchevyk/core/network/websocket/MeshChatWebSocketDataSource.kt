package ru.kubsu.borshchevyk.core.network.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.network.dto.NotificationDto
import ru.kubsu.borshchevyk.core.network.dto.ReactionEvent
import ru.kubsu.borshchevyk.core.network.dto.ReadReceiptEvent
import ru.kubsu.borshchevyk.core.network.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.ShortChatDto
import ru.kubsu.borshchevyk.core.network.dto.ShortUserDto
import ru.kubsu.borshchevyk.core.network.dto.TypingEvent
import ru.kubsu.borshchevyk.core.network.mesh.MeshEnvelope
import ru.kubsu.borshchevyk.core.network.mesh.MeshGossipProtocol
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import ru.kubsu.borshchevyk.core.network.dto.MeshMessagePayload

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
                    chat = ShortChatDto(id = meshPayload.chatId, name = "Mesh Chat"),
                    author = ShortUserDto(id = envelope.originEndpointId, firstName = "Mesh User"),
                    text = meshPayload.request.text,
                    createdAt = Instant.now().toString(),
                    status = "DELIVERED"
                )
            }
    }

    override fun observeChatEvents(): Flow<NotificationDto.ChatEventDto> {
        return emptyFlow()
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
                    user = ShortUserDto(id = envelope.originEndpointId, firstName = "Mesh User"),
                    isTyping = isTyping
                )
            }
    }

    override fun observeReactions(chatId: String): Flow<ReactionEvent> {
        return emptyFlow()
    }

    override fun observePins(chatId: String): Flow<String> {
        return emptyFlow()
    }

    override fun observeUnpins(chatId: String): Flow<String> {
        return emptyFlow()
    }

    override fun observeReadReceipts(chatId: String): Flow<ReadReceiptEvent> {
        return emptyFlow()
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
