package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class RemoveReactionUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, messageId: String, reaction: String) {
        messageRepository.removeReaction(chatId, messageId, reaction)
    }
}
