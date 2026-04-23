package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class ChatMessageUseCases @Inject constructor(
    val sendMessage: SendMessageUseCase,
    val editMessage: EditMessageUseCase,
    val deleteMessage: DeleteMessageUseCase,
    val addReaction: AddReactionUseCase,
    val removeReaction: RemoveReactionUseCase,
    val pinMessage: PinMessageUseCase,
    val unpinMessage: UnpinMessageUseCase,
    val markMessageAsRead: MarkMessageAsReadUseCase,
    val sendTypingEvent: SendTypingEventUseCase
)
