package ru.kubsu.borshchevyk.core.network.client

/**
 * Centralized definition of global network constants, including base API and WebSocket URLs.
 */
object NetworkConstants {
    /** The base URL for all standard REST API requests. */
    const val BASE_URL = "https://borshchevik.su"
    
    /** The base WebSocket URL for real-time messaging and notifications. */
    const val WS_URL = "wss://borshchevik.su/api/v1/messages/ws-message"
    
    /** The WebSocket URL for LiveKit audio/video communication. */
    const val LIVEKIT_URL = "wss://borshchevik.su/livekit"
}
