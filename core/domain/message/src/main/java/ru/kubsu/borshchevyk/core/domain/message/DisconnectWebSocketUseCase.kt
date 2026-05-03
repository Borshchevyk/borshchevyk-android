package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for terminating the WebSocket connection.
 *
 * This use case safely disconnects the real-time communication channel. It should be
 * invoked when the user logs out, the app goes into the background, or when real-time
 * updates are no longer needed, to conserve resources and battery life.
 *
 * @property messageRepository The repository managing the WebSocket connection.
 */
class DisconnectWebSocketUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Disconnects the active WebSocket session.
     */
    suspend operator fun invoke() {
        messageRepository.disconnectWebSocket()
    }
}
