package ru.kubsu.borshchevyk.core.domain.chat

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.Chat
import javax.inject.Inject

class ObserveUserChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<Chat>> {
        return chatRepository.observeUserChats()
    }
}
