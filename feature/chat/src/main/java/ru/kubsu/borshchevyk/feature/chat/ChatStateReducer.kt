package ru.kubsu.borshchevyk.feature.chat

import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType

fun ChatUiState.reduce(event: ChatEvent): ChatUiState {
    if (this !is ChatUiState.Content) return this

    return when (event) {
        is ChatEvent.NewMessage -> {
            val dto = event.message
            if (dto.chatId.equals(this.context.chatId, ignoreCase = true)) {
                if (dto.isDeleted) {
                    this.copy(
                        feed = this.feed.copy(
                            messages = this.feed.messages.filterNot { it.id == dto.id },
                            pinnedMessages = this.feed.pinnedMessages.filterNot { it.id == dto.id }
                        )
                    )
                } else {
                    val existingMsg = this.feed.messages.find { it.id == dto.id }
                    val mappedAttachments = dto.attachments?.map {
                        Attachment(
                            id = it.id,
                            type = it.type ?: AttachmentType.FILE,
                            originalFilename = it.originalFilename ?: "file",
                            extension = it.extension ?: "",
                            sizeBytes = it.sizeBytes ?: 0L
                        )
                    } ?: dto.attachmentIdsOld?.map {
                        Attachment(
                            id = it.id,
                            type = it.type ?: AttachmentType.FILE,
                            originalFilename = it.originalFilename ?: "file",
                            extension = it.extension ?: "",
                            sizeBytes = it.sizeBytes ?: 0L
                        )
                    } ?: emptyList()

                    if (existingMsg != null) {
                        val updatedMsg = existingMsg.copy(
                            text = dto.text,
                            isDeleted = dto.isDeleted,
                            attachments = if (mappedAttachments.isNotEmpty()) mappedAttachments else existingMsg.attachments,
                            forwardedFromChatId = dto.forwardedFromChatId ?: existingMsg.forwardedFromChatId,
                            forwardedFromUserId = dto.forwardedFromUserId ?: existingMsg.forwardedFromUserId
                        )
                        this.copy(
                            feed = this.feed.copy(
                                messages = this.feed.messages.map { if (it.id == dto.id) updatedMsg else it },
                                pinnedMessages = this.feed.pinnedMessages.map { if (it.id == dto.id) updatedMsg else it }
                            )
                        )
                    } else {
                        // Check if message already exists in the list to avoid duplicate keys
                        val alreadyExists = this.feed.messages.any { it.id == dto.id }
                        if (alreadyExists) {
                            val msgInList = this.feed.messages.find { it.id == dto.id }!!
                            val updatedMsg = Message(
                                id = dto.id,
                                chatId = dto.chatId,
                                authorId = dto.authorId,
                                text = dto.text,
                                createdAt = dto.createdAt,
                                status = dto.status?.let {MessageStatus.valueOf(it) },
                                isDeleted = dto.isDeleted,
                                source = MessageSource.ONLINE,
                                isPinned = false,
                                reactions = emptyList(),
                                commentsCount = 0,
                                parentMessageId = null,
                                forwardedFromChatId = dto.forwardedFromChatId ?: msgInList.forwardedFromChatId,
                                forwardedFromUserId = dto.forwardedFromUserId ?: msgInList.forwardedFromUserId,
                                attachments = mappedAttachments
                            )
                            this.copy(
                                feed = this.feed.copy(
                                    messages = this.feed.messages.map { if (it.id == dto.id) updatedMsg else it }
                                )
                            )
                        } else {
                            val newMsg = Message(
                                id = dto.id,
                                chatId = dto.chatId,
                                authorId = dto.authorId,
                                text = dto.text,
                                createdAt = dto.createdAt,
                                status = dto.status?.let { ru.kubsu.borshchevyk.core.model.domain.MessageStatus.valueOf(it) },
                                isDeleted = dto.isDeleted,
                                source = ru.kubsu.borshchevyk.core.model.domain.MessageSource.ONLINE,
                                isPinned = false,
                                reactions = emptyList(),
                                commentsCount = 0,
                                parentMessageId = null,
                                forwardedFromChatId = dto.forwardedFromChatId,
                                forwardedFromUserId = dto.forwardedFromUserId,
                                attachments = mappedAttachments
                            )
                            this.copy(feed = this.feed.copy(messages = listOf(newMsg) + this.feed.messages))
                        }
                    }
                }
            } else {
                this
            }
        }
        is ChatEvent.MessageDeleted -> {
            this.copy(
                feed = this.feed.copy(
                    messages = this.feed.messages.filterNot { it.id == event.messageId },
                    pinnedMessages = this.feed.pinnedMessages.filterNot { it.id == event.messageId }
                )
            )
        }
        is ChatEvent.Typing -> {
            val newTypingUsers = this.input.typingUsers.toMutableSet()
            if (event.event.isTyping) {
                newTypingUsers.add(event.event.userId)
            } else {
                newTypingUsers.remove(event.event.userId)
            }
            this.copy(input = this.input.copy(typingUsers = newTypingUsers))
        }
        is ChatEvent.ReactionUpdated -> {
            val dto = event.event
            this.copy(
                feed = this.feed.copy(
                    messages = this.feed.messages.map { msg ->
                        if (msg.id == dto.messageId) {
                            val newReactions = msg.reactions.toMutableList()
                            if (dto.isAdded) {
                                if (!newReactions.any { it.reaction == dto.reaction && it.userId == dto.userId }) {
                                    newReactions.add(MessageReaction(dto.userId, dto.reaction))
                                }
                            } else {
                                newReactions.removeAll { it.reaction == dto.reaction && it.userId == dto.userId }
                            }
                            msg.copy(reactions = newReactions)
                        } else msg
                    }
                )
            )
        }
        is ChatEvent.MessagePinned -> {
            val updatedMessages = this.feed.messages.map {
                if (it.id == event.messageId) it.copy(isPinned = true) else it
            }
            this.copy(feed = this.feed.copy(messages = updatedMessages))
        }
        is ChatEvent.MessageUnpinned -> {
            val updatedMessages = this.feed.messages.map {
                if (it.id == event.messageId) it.copy(isPinned = false) else it
            }
            this.copy(feed = this.feed.copy(messages = updatedMessages))
        }
        is ChatEvent.ReadReceipt -> {
            val updatedMessages = this.feed.messages.map {
                if (it.id == event.event.messageId) {
                    // Update to READ only if it's not already READ. 
                    // This prevents multiple users reading from causing "re-reading" flashes if any logic depends on state transitions.
                    if (it.status != MessageStatus.READ) {
                        it.copy(status = MessageStatus.READ)
                    } else it
                } else it
            }
            this.copy(feed = this.feed.copy(messages = updatedMessages))
        }
    }
}
