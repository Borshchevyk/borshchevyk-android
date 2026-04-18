package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.dto.ReadReceiptEvent
import javax.inject.Inject

class ObserveReadReceiptsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(chatId: String): Flow<ReadReceiptEvent> {
        return messageRepository.observeReadReceipts(chatId)
    }
}
