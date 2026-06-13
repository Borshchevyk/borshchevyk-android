package ru.kubsu.borshchevyk.feature.chat.conversation.mvi

import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.core.model.domain.GlobalChatAction
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
                    chatName = action.chatTitle,
                    chatAvatarUrl = action.chatAvatarUrl
                ),
                feed = MessageFeed(
                    messages = action.history.toPersistentList(),
                    pinnedMessages = action.pinned.toPersistentList()
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
                this.copy(
                    feed = this.feed.copy(
                        messages = action.history.toPersistentList(),
                        pinnedMessages = action.history.filter { it.isPinned }.toPersistentList()
                    )
                )
            } else this
        }
        is ChatStateAction.SetEditingMessage -> {
            if (this is ChatUiState.Content) this.copy(input = this.input.copy(editingMessage = action.message)) else this
        }
        is ChatStateAction.MessageUpdated -> {
            if (this is ChatUiState.Content) {
                val updatedMessages = this.feed.messages.map { if (it.id == action.message.id) action.message else it }.toPersistentList()
                val updatedPinned = this.feed.pinnedMessages.map { if (it.id == action.message.id) action.message else it }.toPersistentList()
                this.copy(
                    feed = this.feed.copy(messages = updatedMessages, pinnedMessages = updatedPinned),
                    input = this.input.copy(editingMessage = null)
                )
            } else this
        }
        is ChatStateAction.MessageSending -> {
            if (this is ChatUiState.Content) {
                val existingIndex = this.feed.messages.indexOfFirst { it.id == action.tempId }
                val newMessages = if (existingIndex != -1) {
                    val mutableMessages = this.feed.messages.toMutableList()
                    mutableMessages[existingIndex] = action.message
                    mutableMessages.toPersistentList()
                } else {
                    (listOf(action.message) + this.feed.messages).toPersistentList()
                }

                val builder = this.input.pendingMessagesData.builder()
                builder[action.tempId] = action.data

                this.copy(
                    feed = this.feed.copy(messages = newMessages),
                    input = this.input.copy(isSending = true, forwardPayload = null, editingMessage = null, pendingMessagesData = builder.build())
                )
            } else this
        }
        is ChatStateAction.MessageSent -> {
            if (this is ChatUiState.Content) {
                val builder = this.input.pendingMessagesData.builder()
                builder.remove(action.tempId)

                val updatedMessages = this.feed.messages.filterNot { it.id == action.tempId }.toPersistentList()
                this.copy(
                    feed = this.feed.copy(messages = updatedMessages),
                    input = this.input.copy(isSending = false, pendingMessagesData = builder.build())
                )
            } else this
        }
        is ChatStateAction.MessageSendFailed -> {
            if (this is ChatUiState.Content) {
                val updatedMessages = this.feed.messages.map { 
                    if (it.id == action.tempId) it.copy(status = MessageStatus.ERROR) else it 
                }.toPersistentList()
                this.copy(
                    feed = this.feed.copy(messages = updatedMessages),
                    input = this.input.copy(isSending = false)
                )
            } else this
        }
        is ChatStateAction.UpdateAttachmentUrl -> {
            if (this is ChatUiState.Content) {
                if (action.isThumbnail) {
                    val builder = this.feed.thumbnailUrls.builder()
                    builder[action.attachmentId] = action.url
                    this.copy(feed = this.feed.copy(thumbnailUrls = builder.build()))
                } else {
                    val builder = this.feed.attachmentUrls.builder()
                    builder[action.attachmentId] = action.url
                    this.copy(feed = this.feed.copy(attachmentUrls = builder.build()))
                }
            } else this
        }
        is ChatStateAction.SetReaders -> {
            if (this is ChatUiState.Content) {
                val builder = this.feed.readersByMessageId.builder()
                builder[action.messageId] = action.readers.toPersistentList()
                this.copy(feed = this.feed.copy(readersByMessageId = builder.build()))
            } else this
        }
        is ChatStateAction.SetComments -> {
            if (this is ChatUiState.Content) {
                val builder = this.feed.commentsByMessageId.builder()
                builder[action.messageId] = action.comments.toPersistentList()
                this.copy(feed = this.feed.copy(commentsByMessageId = builder.build()))
            } else this
        }

        is ChatStateAction.MessageRemoved -> {
            if (this is ChatUiState.Content) {
                val builder = this.input.pendingMessagesData.builder()
                builder.remove(action.messageId)

                this.copy(
                    feed = this.feed.copy(
                        messages = this.feed.messages.filterNot { it.id == action.messageId }.toPersistentList(),
                        pinnedMessages = this.feed.pinnedMessages.filterNot { it.id == action.messageId }.toPersistentList()
                    ),
                    input = this.input.copy(pendingMessagesData = builder.build())
                )
            } else this
        }
        is ChatStateAction.SetPinnedMessages -> {
            if (this is ChatUiState.Content) {
                this.copy(feed = this.feed.copy(pinnedMessages = action.pinned.toPersistentList()))
            } else this
        }
        is ChatStateAction.ReactionToggled -> {
            if (this is ChatUiState.Content) {
                val updatedMessages = toggleReactionInList(
                    messages = this.feed.messages,
                    messageId = action.messageId,
                    reaction = action.reaction,
                    userId = action.currentUserId,
                    isAdded = action.isAdded
                )
                this.copy(feed = this.feed.copy(messages = updatedMessages))
            } else this
        }
        is ChatStateAction.TitleUpdated -> {
            if (this is ChatUiState.Content) {
                this.copy(context = this.context.copy(chatName = action.title, chatAvatarUrl = action.avatarUrl))
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
                                    messages = this.feed.messages.filterNot { it.id == msg.id }.toPersistentList(),
                                    pinnedMessages = this.feed.pinnedMessages.filterNot { it.id == msg.id }.toPersistentList()
                                )
                            )
                        } else {
                            val existingMsg = this.feed.messages.find { it.id == msg.id }
                            if (existingMsg != null) {
                                this.copy(
                                    feed = this.feed.copy(
                                        messages = this.feed.messages.map { if (it.id == msg.id) msg else it }.toPersistentList(),
                                        pinnedMessages = this.feed.pinnedMessages.map { if (it.id == msg.id) msg else it }.toPersistentList()
                                    )
                                )
                            } else {
                                this.copy(feed = this.feed.copy(messages = (listOf(msg) + this.feed.messages).toPersistentList()))
                            }
                        }
                    } else this
                }
                is ChatEvent.MessageDeleted -> {
                    this.copy(
                        feed = this.feed.copy(
                            messages = this.feed.messages.filterNot { it.id == event.messageId }.toPersistentList(),
                            pinnedMessages = this.feed.pinnedMessages.filterNot { it.id == event.messageId }.toPersistentList()
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
                    this.copy(input = this.input.copy(typingUsers = newTypingUsers.toPersistentSet()))
                }
                is ChatEvent.ReactionUpdated -> {
                    val dto = event.event
                    val updatedMessages = toggleReactionInList(
                        messages = this.feed.messages,
                        messageId = dto.messageId,
                        reaction = dto.reaction,
                        userId = dto.userId,
                        isAdded = dto.isAdded
                    )
                    this.copy(feed = this.feed.copy(messages = updatedMessages))
                }
                is ChatEvent.MessagePinned -> {
                    val updatedMessages = this.feed.messages.map {
                        if (it.id == event.messageId) it.copy(isPinned = true) else it
                    }.toPersistentList()
                    val pinnedMsg = this.feed.messages.find { it.id == event.messageId }?.copy(isPinned = true)
                    val newPinned = if (pinnedMsg != null && !this.feed.pinnedMessages.any { it.id == event.messageId }) {
                        (listOf(pinnedMsg) + this.feed.pinnedMessages).toPersistentList()
                    } else {
                        this.feed.pinnedMessages
                    }
                    this.copy(feed = this.feed.copy(messages = updatedMessages, pinnedMessages = newPinned))
                }
                is ChatEvent.MessageUnpinned -> {
                    val updatedMessages = this.feed.messages.map {
                        if (it.id == event.messageId) it.copy(isPinned = false) else it
                    }.toPersistentList()
                    val newPinned = this.feed.pinnedMessages.filterNot { it.id == event.messageId }.toPersistentList()
                    this.copy(feed = this.feed.copy(messages = updatedMessages, pinnedMessages = newPinned))
                }
                is ChatEvent.ReadReceipt -> {
                    val updatedMessages = this.feed.messages.map {
                        if (it.id == event.event.messageId) {
                            if (it.status != MessageStatus.READ) {
                                it.copy(status = MessageStatus.READ)
                            } else it
                        } else it
                    }.toPersistentList()
                    this.copy(feed = this.feed.copy(messages = updatedMessages))
                }
                is ChatEvent.GlobalChatEvent -> {
                    if (event.event.action == GlobalChatAction.DELETED && 
                        event.event.chat.id.equals(this.context.chatId, ignoreCase = true)) {
                        this.copy(isChatDeleted = true)
                    } else this
                }
            }
        }
    }
}

private fun toggleReactionInList(
    messages: kotlinx.collections.immutable.PersistentList<ru.kubsu.borshchevyk.core.model.domain.Message>,
    messageId: String,
    reaction: String,
    userId: String,
    isAdded: Boolean
): kotlinx.collections.immutable.PersistentList<ru.kubsu.borshchevyk.core.model.domain.Message> {
    val index = messages.indexOfFirst { it.id == messageId }
    if (index == -1) return messages

    val msg = messages[index]
    val newReactions = msg.reactions.toMutableList()
    if (isAdded) {
        if (!newReactions.any { it.reaction == reaction && it.userId == userId }) {
            newReactions.add(MessageReaction(userId, reaction))
        }
    } else {
        newReactions.removeAll { it.reaction == reaction && it.userId == userId }
    }
    
    val mutableMessages = messages.toMutableList()
    mutableMessages[index] = msg.copy(reactions = newReactions)
    return mutableMessages.toPersistentList()
}
