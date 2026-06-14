package ru.kubsu.borshchevyk.feature.chat.conversation.mvi

import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageStatus
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.model.MessageUiModel
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.model.toUiModel

fun ChatUiState.setEditingMessage(message: Message?): ChatUiState {
    if (this !is ChatUiState.Content) return this
    return this.copy(input = this.input.copy(editingMessage = message?.toUiModel()))
}

fun ChatUiState.setMessageSending(tempId: String, message: MessageUiModel, data: FailedMessageData): ChatUiState {
    if (this !is ChatUiState.Content) return this
    val existingIndex = this.feed.messages.indexOfFirst { it.id == tempId }
    val newMessages = if (existingIndex != -1) {
        this.feed.messages.set(existingIndex, message)
    } else {
        this.feed.messages.add(0, message)
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
    val existingIndex = this.feed.messages.indexOfFirst { it.id == tempId }
    val updatedMessages = if (existingIndex != -1) {
        this.feed.messages.set(existingIndex, this.feed.messages[existingIndex].copy(status = MessageStatus.RECEIVED_BY_SERVER))
    } else {
        this.feed.messages
    }
    return this.copy(feed = this.feed.copy(messages = updatedMessages), input = this.input.copy(isSending = false, pendingMessagesData = builder.build()))
}

fun ChatUiState.setMessageSendFailed(tempId: String): ChatUiState {
    if (this !is ChatUiState.Content) return this
    val existingIndex = this.feed.messages.indexOfFirst { it.id == tempId }
    val updatedMessages = if (existingIndex != -1) {
        this.feed.messages.set(existingIndex, this.feed.messages[existingIndex].copy(status = MessageStatus.ERROR))
    } else {
        this.feed.messages
    }
    return this.copy(feed = this.feed.copy(messages = updatedMessages), input = this.input.copy(isSending = false))
}

fun ChatUiState.setRecordingVoice(isRecording: Boolean): ChatUiState {
    if (this !is ChatUiState.Content) return this
    return this.copy(input = this.input.copy(isRecordingVoice = isRecording))
}
