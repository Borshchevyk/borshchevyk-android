package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * A wrapper class that groups all use cases related to active message operations.
 *
 * This class aggregates operations such as sending, editing, deleting, and reacting to
 * messages, providing a single dependency for ViewModels to handle active chat interactions.
 *
 * @property sendMessage Use case for sending a new message.
 * @property editMessage Use case for editing an existing message.
 * @property deleteMessage Use case for deleting a message.
 * @property addReaction Use case for adding a reaction to a message.
 * @property removeReaction Use case for removing an existing reaction from a message.
 * @property pinMessage Use case for pinning a message in the chat.
 * @property unpinMessage Use case for unpinning a previously pinned message.
 * @property markMessageAsRead Use case for marking a message as read by the current user.
 * @property sendTypingEvent Use case for broadcasting that the current user is typing.
 */
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
