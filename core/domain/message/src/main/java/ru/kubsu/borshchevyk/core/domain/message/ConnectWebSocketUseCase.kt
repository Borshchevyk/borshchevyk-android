package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

class ConnectWebSocketUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke() {
        messageRepository.connectWebSocket()
    }
}
