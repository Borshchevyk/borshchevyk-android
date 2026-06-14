package ru.kubsu.borshchevyk.feature.chat.conversation.mvi

import kotlinx.collections.immutable.toPersistentList
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.model.MessageUiModel
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.model.toUiModel

fun ChatUiState.updateHistory(history: List<Message>): ChatUiState {
    if (this !is ChatUiState.Content) return this
    val newMessages = history.map { it.toUiModel() }
    val tempMessages = this.feed.messages.filter { tempMsg ->
        tempMsg.id.startsWith("temp_") && !history.any {
            it.authorId == tempMsg.authorId && it.text == tempMsg.text && it.attachments.size == tempMsg.attachments.size
        }
    }
    return this.copy(feed = this.feed.copy(
        messages = (tempMessages + newMessages).toPersistentList(),
        pinnedMessages = history.filter { it.isPinned }.map { it.toUiModel() }.toPersistentList()
    ))
}

fun ChatUiState.updateMessage(message: Message): ChatUiState {
    if (this !is ChatUiState.Content) return this
    val uiModel = message.toUiModel()
    
    val msgIndex = this.feed.messages.indexOfFirst { it.id == message.id }
    val updatedMessages = if (msgIndex != -1) this.feed.messages.set(msgIndex, uiModel) else this.feed.messages
    
    val pinnedIndex = this.feed.pinnedMessages.indexOfFirst { it.id == message.id }
    val updatedPinned = if (pinnedIndex != -1) this.feed.pinnedMessages.set(pinnedIndex, uiModel) else this.feed.pinnedMessages
    
    return this.copy(feed = this.feed.copy(messages = updatedMessages, pinnedMessages = updatedPinned), input = this.input.copy(editingMessage = null))
}

fun ChatUiState.updateAttachmentUrl(attachmentId: String, url: String, isThumbnail: Boolean): ChatUiState {
    if (this !is ChatUiState.Content) return this
    if (isThumbnail) {
        val builder = this.feed.thumbnailUrls.builder()
        builder[attachmentId] = url
        return this.copy(feed = this.feed.copy(thumbnailUrls = builder.build()))
    } else {
        val builder = this.feed.attachmentUrls.builder()
        builder[attachmentId] = url
        return this.copy(feed = this.feed.copy(attachmentUrls = builder.build()))
    }
}

fun ChatUiState.setReaders(messageId: String, readers: List<User>): ChatUiState {
    if (this !is ChatUiState.Content) return this
    val builder = this.feed.readersByMessageId.builder()
    builder[messageId] = readers.toPersistentList()
    return this.copy(feed = this.feed.copy(readersByMessageId = builder.build()))
}

fun ChatUiState.setComments(messageId: String, comments: List<Message>): ChatUiState {
    if (this !is ChatUiState.Content) return this
    val builder = this.feed.commentsByMessageId.builder()
    builder[messageId] = comments.map { it.toUiModel() }.toPersistentList()
    return this.copy(feed = this.feed.copy(commentsByMessageId = builder.build()))
}

fun ChatUiState.removeMessage(messageId: String): ChatUiState {
    if (this !is ChatUiState.Content) return this
    val builder = this.input.pendingMessagesData.builder()
    builder.remove(messageId)
    return this.copy(
        feed = this.feed.copy(
            messages = this.feed.messages.filterNot { it.id == messageId }.toPersistentList(),
            pinnedMessages = this.feed.pinnedMessages.filterNot { it.id == messageId }.toPersistentList()
        ),
        input = this.input.copy(pendingMessagesData = builder.build())
    )
}

fun ChatUiState.setPinnedMessages(pinned: List<Message>): ChatUiState {
    if (this !is ChatUiState.Content) return this
    return this.copy(feed = this.feed.copy(pinnedMessages = pinned.map { it.toUiModel() }.toPersistentList()))
}

fun ChatUiState.toggleReaction(messageId: String, reaction: String, currentUserId: String, isAdded: Boolean): ChatUiState {
    if (this !is ChatUiState.Content) return this
    val updatedMessages = toggleReactionInList(this.feed.messages, messageId, reaction, currentUserId, isAdded)
    return this.copy(feed = this.feed.copy(messages = updatedMessages))
}

internal fun toggleReactionInList(
    messages: kotlinx.collections.immutable.PersistentList<MessageUiModel>,
    messageId: String,
    reaction: String,
    userId: String,
    isAdded: Boolean
): kotlinx.collections.immutable.PersistentList<MessageUiModel> {
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
    
    return messages.set(index, msg.copy(reactions = newReactions.toPersistentList()))
}
