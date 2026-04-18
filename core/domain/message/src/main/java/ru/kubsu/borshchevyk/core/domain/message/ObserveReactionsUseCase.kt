package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.dto.ReactionEvent
import javax.inject.Inject

class ObserveReactionsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(chatId: String): Flow<ReactionEvent> {
        return messageRepository.observeReactions(chatId)
    }
}
