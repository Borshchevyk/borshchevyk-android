package ru.kubsu.borshchevyk.feature.chat

import ru.kubsu.borshchevyk.core.model.domain.ChatEvent
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus

fun ChatUiState.reduce(event: ChatEvent): ChatUiState {
    if (this !is ChatUiState.Content) return this

    var newState = this

    return when (event) {
        is ChatEvent.NewMessage -> {
            val msg = event.message

            if (msg.chatId.equals(newState.context.chatId, ignoreCase = true)) {
                if (msg.isDeleted) {
                    newState.copy(
                        feed = newState.feed.copy(
                            messages = newState.feed.messages.filterNot { it.id == msg.id },
                            pinnedMessages = newState.feed.pinnedMessages.filterNot { it.id == msg.id }
                        )
                    )
                } else {
                    val existingMsg = newState.feed.messages.find { it.id == msg.id }
                    if (existingMsg != null) {
                        newState.copy(
                            feed = newState.feed.copy(
                                messages = newState.feed.messages.map { if (it.id == msg.id) msg else it },
                                pinnedMessages = newState.feed.pinnedMessages.map { if (it.id == msg.id) msg else it }
                            )
                        )
                    } else {
                        newState.copy(feed = newState.feed.copy(messages = listOf(msg) + newState.feed.messages))
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
            val userId = event.event.userId
            if (event.event.isTyping) {
                newTypingUsers.add(userId)
            } else {
                newTypingUsers.remove(userId)
            }
            newState.copy(input = newState.input.copy(typingUsers = newTypingUsers))
        }
        is ChatEvent.ReactionUpdated -> {
            val dto = event.event
            val userId = dto.userId
            
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
