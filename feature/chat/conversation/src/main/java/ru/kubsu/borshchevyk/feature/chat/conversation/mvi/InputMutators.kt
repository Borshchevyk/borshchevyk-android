package ru.kubsu.borshchevyk.feature.chat.conversation.mvi

import kotlinx.collections.immutable.toPersistentList
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus

fun ChatUiState.setEditingMessage(message: Message?): ChatUiState {
    if (this !is ChatUiState.Content) return this
    return this.copy(input = this.input.copy(editingMessage = message))
}

fun ChatUiState.setMessageSending(tempId: String, message: Message, data: FailedMessageData): ChatUiState {
    if (this !is ChatUiState.Content) return this
    val existingIndex = this.feed.messages.indexOfFirst { it.id == tempId }
    val newMessages = if (existingIndex != -1) {
        val mutableMessages = this.feed.messages.toMutableList()
        mutableMessages[existingIndex] = message
        mutableMessages.toPersistentList()
    } else {
        (listOf(message) + this.feed.messages).toPersistentList()
    }

    val builder = this.input.pendingMessagesData.builder()
    builder[tempId] = data

    return this.copy(
        feed = this.feed.copy(messages = newMessages),
        input = this.input.copy(isSending = true, forwardPayload = null, editingMessage = null, pendingMessagesData = builder.build())
    )
}

fun ChatUiState.setMessageSent(tempId: String): ChatUiState {
    if (this !is ChatUiState.Content) return this
    val builder = this.input.pendingMessagesData.builder()
    builder.remove(tempId)
    val updatedMessages = this.feed.messages.filterNot { it.id == tempId }.toPersistentList()
    return this.copy(feed = this.feed.copy(messages = updatedMessages), input = this.input.copy(isSending = false, pendingMessagesData = builder.build()))
}

fun ChatUiState.setMessageSendFailed(tempId: String): ChatUiState {
    if (this !is ChatUiState.Content) return this
    val updatedMessages = this.feed.messages.map { if (it.id == tempId) it.copy(status = MessageStatus.ERROR) else it }.toPersistentList()
    return this.copy(feed = this.feed.copy(messages = updatedMessages), input = this.input.copy(isSending = false))
}
