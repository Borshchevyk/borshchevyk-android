package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class GenerateInviteLinkUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String): String {
        return chatRepository.generateInviteLink(chatId)
    }
}
