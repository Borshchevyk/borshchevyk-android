package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.DomainPage
import javax.inject.Inject

class GetChatMembersUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, page: Int = 0, size: Int = 50): DomainPage<ChatMember> {
        return chatRepository.getChatMembers(chatId, page, size)
    }
}
