package ru.kubsu.borshchevyk.feature.chat.conversation.interactor

import ru.kubsu.borshchevyk.core.domain.message.ChatAttachmentUseCases
import ru.kubsu.borshchevyk.core.domain.message.ChatMessageUseCases
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.feature.chat.common.model.AttachmentFile
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class ChatMessageHandler @Inject constructor(
    private val messageUseCases: ChatMessageUseCases,
    private val attachmentUseCases: ChatAttachmentUseCases,
    private val localMediaInteractor: LocalMediaInteractor
) {
    suspend fun sendTypingEvent(chatId: String, isTyping: Boolean) {
        messageUseCases.sendTypingEvent(chatId, isTyping)
    }

    suspend fun sendMessage(chatId: String, text: String, attachments: List<AttachmentFile>, forwardPayload: ForwardPayload?) {
        val attachmentIds = mutableListOf<String>()
        if (forwardPayload != null) attachmentIds.addAll(forwardPayload.attachmentIds)
        if (attachments.isNotEmpty()) {
            val uploadedIds = attachments.mapNotNull { file ->
                val provider = localMediaInteractor.getInputStreamProvider(file.uri)
                attachmentUseCases.uploadAttachment(
                    inputStreamProvider = provider,
                    sizeBytes = file.sizeBytes,
                    originalFilename = file.originalFilename,
                    contentType = file.contentType,
                    extension = file.extension,
                    type = file.toDomainAttachmentType(),
                    width = file.width,
                    height = file.height,
                    duration = file.duration?.toDouble()
                )
            }
            attachmentIds.addAll(uploadedIds)
        }

        val finalAttachmentIds = if (attachmentIds.isNotEmpty()) attachmentIds else null
        val finalText = if (text.isNotBlank()) text else forwardPayload?.text ?: ""

        messageUseCases.sendMessage(
            chatId = chatId,
            text = finalText,
            attachmentIds = finalAttachmentIds,
            forwardedFromChatId = forwardPayload?.fromChatId,
            forwardedFromUserId = forwardPayload?.fromUserId
        )
    }

    suspend fun editMessage(chatId: String, messageId: String, newText: String) {
        messageUseCases.editMessage(chatId, messageId, newText)
    }

    suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean) {
        messageUseCases.deleteMessage(chatId, messageId, forAll)
    }

    suspend fun markAsRead(chatId: String, messageId: String) {
        messageUseCases.markMessageAsRead(chatId, messageId)
    }

    suspend fun pinMessage(chatId: String, messageId: String) {
        messageUseCases.pinMessage(chatId, messageId)
    }

    suspend fun unpinMessage(chatId: String, messageId: String) {
        messageUseCases.unpinMessage(chatId, messageId)
    }

    suspend fun toggleReaction(chatId: String, messageId: String, reaction: String, hasReaction: Boolean) {
        if (hasReaction) {
            messageUseCases.removeReaction(chatId, messageId, reaction)
        } else {
            messageUseCases.addReaction(chatId, messageId, reaction)
        }
    }

    fun createOptimisticMessages(
        chatId: String, text: String, attachments: List<AttachmentFile>, 
        currentUserId: String, forwardPayload: ForwardPayload?
    ): List<OptimisticMessageData> {
        val messages = mutableListOf<OptimisticMessageData>()
        
        if (forwardPayload != null && (text.isNotBlank() || attachments.isNotEmpty())) {
            val tempId1 = "temp_${java.util.UUID.randomUUID()}"
            val msg1 = Message(
                id = tempId1,
                chatId = chatId,
                authorId = currentUserId,
                text = forwardPayload.text ?: "",
                createdAt = LocalDateTime.now(ZoneOffset.UTC).toString() + "Z",
                status = MessageStatus.SENDING,
                source = MessageSource.ONLINE,
                forwardedFromChatId = forwardPayload.fromChatId,
                forwardedFromUserId = forwardPayload.fromUserId
            )
            messages.add(OptimisticMessageData(tempId1, msg1, "", emptyList(), forwardPayload))

            val tempId2 = "temp_${java.util.UUID.randomUUID()}"
            val msg2 = Message(
                id = tempId2,
                chatId = chatId,
                authorId = currentUserId,
                text = text,
                createdAt = LocalDateTime.now(ZoneOffset.UTC).toString() + "Z",
                status = MessageStatus.SENDING,
                source = MessageSource.ONLINE,
                attachments = attachments.map { 
                    Attachment(
                        id = "temp_att_${java.util.UUID.randomUUID()}",
                        type = it.toDomainAttachmentType(),
                        originalFilename = it.originalFilename,
                        extension = it.extension,
                        sizeBytes = it.sizeBytes
                    )
                }
            )
            messages.add(OptimisticMessageData(tempId2, msg2, text, attachments, null))
        } else {
            val tempId = "temp_${java.util.UUID.randomUUID()}"
            val msg = Message(
                id = tempId,
                chatId = chatId,
                authorId = currentUserId,
                text = text.ifBlank { forwardPayload?.text ?: "" },
                createdAt = LocalDateTime.now(ZoneOffset.UTC).toString() + "Z",
                status = MessageStatus.SENDING,
                source = MessageSource.ONLINE,
                forwardedFromChatId = forwardPayload?.fromChatId,
                forwardedFromUserId = forwardPayload?.fromUserId,
                attachments = attachments.map { 
                    Attachment(
                        id = "temp_att_${java.util.UUID.randomUUID()}",
                        type = it.toDomainAttachmentType(),
                        originalFilename = it.originalFilename,
                        extension = it.extension,
                        sizeBytes = it.sizeBytes
                    )
                }
            )
            messages.add(OptimisticMessageData(tempId, msg, text, attachments, forwardPayload))
        }
        return messages
    }
}

data class OptimisticMessageData(
    val tempId: String,
    val message: Message,
    val text: String,
    val attachments: List<AttachmentFile>,
    val forwardPayload: ForwardPayload?
)
