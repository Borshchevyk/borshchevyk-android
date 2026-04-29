package ru.kubsu.borshchevyk.feature.chat

import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus

fun ChatUiState.reduce(action: ChatStateAction): ChatUiState {
    return when (action) {
        is ChatStateAction.LoadingStarted -> if (action.isFullLoad) ChatUiState.Loading else this
        is ChatStateAction.LoadFailed -> ChatUiState.Error(action.error)
        is ChatStateAction.InitialDataLoaded -> {
            ChatUiState.Content(
                context = ChatContext(
                    chatId = action.chatId,
                    currentUserId = action.currentUserId,
                    isGroupChat = action.isGroup,
                    chatName = action.chatTitle
                ),
                feed = MessageFeed(
                    messages = action.history,
                    pinnedMessages = action.pinned
                ),
                input = InputState(
                    forwardPayload = action.forwardPayload
                )
            )
        }
        is ChatStateAction.PresenceUpdated -> {
            if (this is ChatUiState.Content) {
                this.copy(context = this.context.copy(isOnline = action.isOnline, lastSeenAt = action.lastSeenAt))
            } else this
        }
        is ChatStateAction.ChatDeleted -> {
            if (this is ChatUiState.Content) this.copy(isChatDeleted = true) else this
        }
        is ChatStateAction.HistoryUpdated -> {
            if (this is ChatUiState.Content) {
                this.copy(feed = this.feed.copy(messages = action.history))
            } else this
        }
        is ChatStateAction.SetEditingMessage -> {
            if (this is ChatUiState.Content) this.copy(input = this.input.copy(editingMessage = action.message)) else this
        }
        is ChatStateAction.MessageUpdated -> {
            if (this is ChatUiState.Content) {
                val updatedMessages = this.feed.messages.map { if (it.id == action.message.id) action.message else it }
                val updatedPinned = this.feed.pinnedMessages.map { if (it.id == action.message.id) action.message else it }
                this.copy(
                    feed = this.feed.copy(messages = updatedMessages, pinnedMessages = updatedPinned),
                    input = this.input.copy(editingMessage = null)
                )
            } else this
        }
        is ChatStateAction.MessageSending -> {
            if (this is ChatUiState.Content) {
                this.copy(
                    feed = this.feed.copy(messages = listOf(action.message) + this.feed.messages),
                    input = this.input.copy(isSending = true, forwardPayload = null, editingMessage = null)
                )
            } else this
        }
        is ChatStateAction.MessageSent -> {
            if (this is ChatUiState.Content) {
                // Just remove temp message, the real one will come via Flow
                val updatedMessages = this.feed.messages.filterNot { it.id == action.tempId }
                this.copy(
                    feed = this.feed.copy(messages = updatedMessages),
                    input = this.input.copy(isSending = false)
                )
            } else this
        }
        is ChatStateAction.MessageSendFailed -> {
            if (this is ChatUiState.Content) {
                val updatedMessages = this.feed.messages.map { 
                    if (it.id == action.tempId) it.copy(status = MessageStatus.ERROR) else it 
                }
                this.copy(
                    feed = this.feed.copy(messages = updatedMessages),
                    input = this.input.copy(isSending = false)
                )
            } else this
        }
        is ChatStateAction.UpdateAttachmentUrl -> {
            if (this is ChatUiState.Content) {
                val newMap = this.feed.attachmentUrls.toMutableMap()
                newMap[action.attachmentId] = action.url
                this.copy(feed = this.feed.copy(attachmentUrls = newMap))
            } else this
        }
        is ChatStateAction.SetReaders -> {
            if (this is ChatUiState.Content) {
                val newMap = this.feed.readersByMessageId.toMutableMap()
                newMap[action.messageId] = action.readers
                this.copy(feed = this.feed.copy(readersByMessageId = newMap))
            } else this
        }
        is ChatStateAction.SetComments -> {
            if (this is ChatUiState.Content) {
                val newMap = this.feed.commentsByMessageId.toMutableMap()
                newMap[action.messageId] = action.comments
                this.copy(feed = this.feed.copy(commentsByMessageId = newMap))
            } else this
        }

        is ChatStateAction.MessageRemoved -> {
            if (this is ChatUiState.Content) {
                this.copy(
                    feed = this.feed.copy(
                        messages = this.feed.messages.filterNot { it.id == action.messageId },
                        pinnedMessages = this.feed.pinnedMessages.filterNot { it.id == action.messageId }
                    )
                )
            } else this
        }
        is ChatStateAction.SetPinnedMessages -> {
            if (this is ChatUiState.Content) {
                this.copy(feed = this.feed.copy(pinnedMessages = action.pinned))
            } else this
        }
        is ChatStateAction.ReactionToggled -> {
            if (this is ChatUiState.Content) {
                val updatedMessages = this.feed.messages.map { msg ->
                    if (msg.id == action.messageId) {
                        val newReactions = msg.reactions.toMutableList()
                        if (action.isAdded) {
                            if (!newReactions.any { it.reaction == action.reaction && it.userId == action.currentUserId }) {
                                newReactions.add(MessageReaction(action.currentUserId, action.reaction))
                            }
                        } else {
                            newReactions.removeAll { it.reaction == action.reaction && it.userId == action.currentUserId }
                        }
                        msg.copy(reactions = newReactions)
                    } else msg
                }
                this.copy(feed = this.feed.copy(messages = updatedMessages))
            } else this
        }
        is ChatStateAction.ProcessDomainEvent -> {
            if (this !is ChatUiState.Content) return this
            val event = action.event
            when (event) {
                is ChatEvent.NewMessage -> {
                    val msg = event.message
                    if (msg.chatId.equals(this.context.chatId, ignoreCase = true)) {
                        if (msg.isDeleted) {
                            this.copy(
                                feed = this.feed.copy(
                                    messages = this.feed.messages.filterNot { it.id == msg.id },
                                    pinnedMessages = this.feed.pinnedMessages.filterNot { it.id == msg.id }
                                )
                            )
                        } else {
                            val existingMsg = this.feed.messages.find { it.id == msg.id }
                            if (existingMsg != null) {
                                this.copy(
                                    feed = this.feed.copy(
                                        messages = this.feed.messages.map { if (it.id == msg.id) msg else it },
                                        pinnedMessages = this.feed.pinnedMessages.map { if (it.id == msg.id) msg else it }
                                    )
                                )
                            } else {
                                this.copy(feed = this.feed.copy(messages = listOf(msg) + this.feed.messages))
                            }
                        }
                    } else this
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
                    val userId = event.event.userId
                    if (event.event.isTyping) {
                        newTypingUsers.add(userId)
                    } else {
                        newTypingUsers.remove(userId)
                    }
                    this.copy(input = this.input.copy(typingUsers = newTypingUsers))
                }
                is ChatEvent.ReactionUpdated -> {
                    val dto = event.event
                    val userId = dto.userId
                    this.copy(
                        feed = this.feed.copy(
                            messages = this.feed.messages.map { msg ->
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
                    val updatedMessages = this.feed.messages.map {
                        if (it.id == event.messageId) it.copy(isPinned = true) else it
                    }
                    val pinnedMsg = this.feed.messages.find { it.id == event.messageId }?.copy(isPinned = true)
                    val newPinned = if (pinnedMsg != null && !this.feed.pinnedMessages.any { it.id == event.messageId }) {
                        listOf(pinnedMsg) + this.feed.pinnedMessages
                    } else {
                        this.feed.pinnedMessages
                    }
                    this.copy(feed = this.feed.copy(messages = updatedMessages, pinnedMessages = newPinned))
                }
                is ChatEvent.MessageUnpinned -> {
                    val updatedMessages = this.feed.messages.map {
                        if (it.id == event.messageId) it.copy(isPinned = false) else it
                    }
                    val newPinned = this.feed.pinnedMessages.filterNot { it.id == event.messageId }
                    this.copy(feed = this.feed.copy(messages = updatedMessages, pinnedMessages = newPinned))
                }
                is ChatEvent.ReadReceipt -> {
                    val updatedMessages = this.feed.messages.map {
                        if (it.id == event.event.messageId) {
                            if (it.status != MessageStatus.READ) {
                                it.copy(status = MessageStatus.READ)
                            } else it
                        } else it
                    }
                    this.copy(feed = this.feed.copy(messages = updatedMessages))
                }
            }
        }
    }
}
