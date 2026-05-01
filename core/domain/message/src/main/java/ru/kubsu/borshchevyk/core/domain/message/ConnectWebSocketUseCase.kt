package ru.kubsu.borshchevyk.core.domain.message

import javax.inject.Inject

/**
 * Use case for establishing a WebSocket connection.
 *
 * This use case handles the initialization of the real-time communication channel
 * required for receiving live message updates, typing events, and other real-time data.
 *
 * @property messageRepository The repository responsible for managing the connection.
 */
class ConnectWebSocketUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Initiates the WebSocket connection.
     *
     * Should be called when the application enters the foreground or when the user
     * navigates to a screen requiring real-time updates.
     */
    suspend operator fun invoke() {
        messageRepository.connectWebSocket()
    }
}
