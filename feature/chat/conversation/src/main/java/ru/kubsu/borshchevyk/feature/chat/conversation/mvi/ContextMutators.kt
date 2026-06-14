package ru.kubsu.borshchevyk.feature.chat.conversation.mvi

import kotlinx.collections.immutable.toPersistentList
import ru.kubsu.borshchevyk.core.model.domain.ForwardPayload
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.feature.chat.conversation.ui.model.toUiModel

fun ChatUiState.setLoading(isFullLoad: Boolean = true): ChatUiState = 
    if (isFullLoad) ChatUiState.Loading else this

fun ChatUiState.setLoadFailed(error: String): ChatUiState = 
    ChatUiState.Error(error)

fun ChatUiState.setInitialDataLoaded(
    chatId: String, currentUserId: String, isGroup: Boolean, chatTitle: String, 
    chatAvatarUrl: String?, history: List<Message>, pinned: List<Message>, forwardPayload: ForwardPayload?
): ChatUiState = ChatUiState.Content(
    context = ChatContext(chatId, currentUserId, isGroup, chatTitle, chatAvatarUrl),
    feed = MessageFeed(
        history.map { it.toUiModel() }.toPersistentList(), 
        pinned.map { it.toUiModel() }.toPersistentList()
    ),
    input = InputState(forwardPayload = forwardPayload)
)

fun ChatUiState.updatePresence(isOnline: Boolean, lastSeenAt: Long?): ChatUiState {
    if (this !is ChatUiState.Content) return this
    return this.copy(context = this.context.copy(isOnline = isOnline, lastSeenAt = lastSeenAt))
}

fun ChatUiState.setChatDeleted(): ChatUiState {
    if (this !is ChatUiState.Content) return this
    return this.copy(isChatDeleted = true)
}

fun ChatUiState.updateTitle(title: String, avatarUrl: String?): ChatUiState {
    if (this !is ChatUiState.Content) return this
    return this.copy(context = this.context.copy(chatName = title, chatAvatarUrl = avatarUrl))
}
