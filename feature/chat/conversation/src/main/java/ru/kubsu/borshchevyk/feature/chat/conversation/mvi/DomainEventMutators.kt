package ru.kubsu.borshchevyk.feature.chat.conversation.mvi

import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.core.model.domain.GlobalChatAction
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus

fun ChatUiState.processDomainEvent(event: ChatEvent): ChatUiState {
    if (this !is ChatUiState.Content) return this
    return when (event) {
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
            val updatedMessages = toggleReactionInList(this.feed.messages, dto.messageId, dto.reaction, dto.userId, dto.isAdded)
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
