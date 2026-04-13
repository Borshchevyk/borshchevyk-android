package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class AddReactionUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, messageId: String, reaction: String) {
        messageRepository.addReaction(chatId, messageId, reaction)
    }
}
