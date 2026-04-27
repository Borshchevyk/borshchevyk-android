package ru.kubsu.borshchevyk.feature.chat

import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.domain.MessageSource
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.dto.AttachmentType

fun ChatUiState.reduce(event: ChatEvent): ChatUiState {
    if (this !is ChatUiState.Content) return this

    var newState = this

    return when (event) {
        is ChatEvent.NewMessage -> {
            val dto = event.message
            
            val author = User(
                userId = dto.author.id,
                firstName = dto.author.firstName,
                lastName = dto.author.lastName,
                tag = dto.author.tag ?: "",
                avatarUrl = dto.author.avatarUrl
            )
            val forwardedAuthor = dto.forwardedFromUser?.let { 
                User(
                    userId = it.id,
                    firstName = it.firstName,
                    lastName = it.lastName,
                    tag = it.tag ?: "",
                    avatarUrl = it.avatarUrl
                )
            }

            if (dto.chat.id.equals(newState.context.chatId, ignoreCase = true)) {
                if (dto.isDeleted) {
                    newState.copy(
                        feed = newState.feed.copy(
                            messages = newState.feed.messages.filterNot { it.id == dto.id },
                            pinnedMessages = newState.feed.pinnedMessages.filterNot { it.id == dto.id }
                        )
                    )
                } else {
                    val existingMsg = newState.feed.messages.find { it.id == dto.id }
                    val mappedAttachments = dto.attachments?.map {
                        Attachment(
                            id = it.id,
                            type = it.type ?: AttachmentType.FILE,
                            originalFilename = it.originalFilename ?: "file",
                            extension = it.extension ?: "",
                            sizeBytes = it.sizeBytes ?: 0L,
                            thumbnailKey = it.thumbnailKey,
                            updatedAt = it.updatedAt,
                            width = it.width,
                            height = it.height,
                            duration = it.duration
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
                            forwardedFromChatId = dto.forwardedFromChat?.id ?: existingMsg.forwardedFromChatId,
                            forwardedFromUserId = dto.forwardedFromUser?.id ?: existingMsg.forwardedFromUserId,
                            author = author,
                            forwardedFromUser = forwardedAuthor ?: existingMsg.forwardedFromUser
                        )
                        newState.copy(
                            feed = newState.feed.copy(
                                messages = newState.feed.messages.map { if (it.id == dto.id) updatedMsg else it },
                                pinnedMessages = newState.feed.pinnedMessages.map { if (it.id == dto.id) updatedMsg else it }
                            )
                        )
                    } else {
                        val alreadyExists = newState.feed.messages.any { it.id == dto.id }
                        if (alreadyExists) {
                            val msgInList = newState.feed.messages.find { it.id == dto.id }!!
                            val updatedMsg = Message(
                                id = dto.id,
                                chatId = dto.chat.id,
                                authorId = dto.author.id,
                                author = author,
                                text = dto.text,
                                createdAt = dto.createdAt,
                                status = dto.status?.let { MessageStatus.valueOf(it) },
                                isDeleted = dto.isDeleted,
                                source = MessageSource.ONLINE,
                                isPinned = false,
                                reactions = emptyList(),
                                commentsCount = 0,
                                parentMessageId = null,
                                forwardedFromChatId = dto.forwardedFromChat?.id ?: msgInList.forwardedFromChatId,
                                forwardedFromUserId = dto.forwardedFromUser?.id ?: msgInList.forwardedFromUserId,
                                forwardedFromUser = forwardedAuthor ?: msgInList.forwardedFromUser,
                                attachments = mappedAttachments
                            )
                            newState.copy(
                                feed = newState.feed.copy(
                                    messages = newState.feed.messages.map { if (it.id == dto.id) updatedMsg else it }
                                )
                            )
                        } else {
                            val newMsg = Message(
                                id = dto.id,
                                chatId = dto.chat.id,
                                authorId = dto.author.id,
                                author = author,
                                text = dto.text,
                                createdAt = dto.createdAt,
                                status = dto.status?.let { MessageStatus.valueOf(it) },
                                isDeleted = dto.isDeleted,
                                source = MessageSource.ONLINE,
                                isPinned = false,
                                reactions = emptyList(),
                                commentsCount = 0,
                                parentMessageId = null,
                                forwardedFromChatId = dto.forwardedFromChat?.id,
                                forwardedFromUserId = dto.forwardedFromUser?.id,
                                forwardedFromUser = forwardedAuthor,
                                attachments = mappedAttachments
                            )
                            newState.copy(feed = newState.feed.copy(messages = listOf(newMsg) + newState.feed.messages))
                        }
                    }
                }
            } else {
                newState
            }
        }
        is ChatEvent.MessageDeleted -> {
            newState.copy(
                feed = newState.feed.copy(
                    messages = newState.feed.messages.filterNot { it.id == event.messageId },
                    pinnedMessages = newState.feed.pinnedMessages.filterNot { it.id == event.messageId }
                )
            )
        }
        is ChatEvent.Typing -> {
            val newTypingUsers = newState.input.typingUsers.toMutableSet()
            val userId = event.event.user.id
            if (event.event.isTyping) {
                newTypingUsers.add(userId)
            } else {
                newTypingUsers.remove(userId)
            }
            newState.copy(input = newState.input.copy(typingUsers = newTypingUsers))
        }
        is ChatEvent.ReactionUpdated -> {
            val dto = event.event
            val userId = dto.user.id
            
            newState.copy(
                feed = newState.feed.copy(
                    messages = newState.feed.messages.map { msg ->
                        if (msg.id == dto.messageId) {
                            val newReactions = msg.reactions.toMutableList()
                            if (dto.isAdded) {
                                if (!newReactions.any { it.reaction == dto.reaction && it.userId == userId }) {
                                    newReactions.add(MessageReaction(userId, dto.reaction))
                                }
                            } else {
                                newReactions.removeAll { it.reaction == dto.reaction && it.userId == userId }
                            }
                            msg.copy(reactions = newReactions)
                        } else msg
                    }
                )
            )
        }
        is ChatEvent.MessagePinned -> {
            val updatedMessages = newState.feed.messages.map {
                if (it.id == event.messageId) it.copy(isPinned = true) else it
            }
            newState.copy(feed = newState.feed.copy(messages = updatedMessages))
        }
        is ChatEvent.MessageUnpinned -> {
            val updatedMessages = newState.feed.messages.map {
                if (it.id == event.messageId) it.copy(isPinned = false) else it
            }
            newState.copy(feed = newState.feed.copy(messages = updatedMessages))
        }
        is ChatEvent.ReadReceipt -> {
            val updatedMessages = newState.feed.messages.map {
                if (it.id == event.event.messageId) {
                    if (it.status != MessageStatus.READ) {
                        it.copy(status = MessageStatus.READ)
                    } else it
                } else it
            }
            newState.copy(feed = newState.feed.copy(messages = updatedMessages))
        }
    }
}
