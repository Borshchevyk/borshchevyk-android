package ru.kubsu.borshchevyk.core.network.message

import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.EnrichedUserResponse
import ru.kubsu.borshchevyk.core.network.dto.MessageResponse
import ru.kubsu.borshchevyk.core.network.dto.SendMessageRequest

/**
 * Data source interface defining operations for sending and managing messages within chats.
 */
interface MessageNetworkDataSource {
    /**
     * Sends a new message to a specific chat.
     *
     * @param chatId The ID of the chat.
     * @param request The [SendMessageRequest] containing the message text and metadata.
     * @return A [NetworkResult] containing the created [MessageResponse].
     */
    suspend fun sendMessage(chatId: String, request: SendMessageRequest): NetworkResult<MessageResponse>

    /**
     * Edits an existing message.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message to edit.
     * @param request The [EditMessageRequest] containing the new text.
     * @return A [NetworkResult] containing the updated [MessageResponse].
     */
    suspend fun editMessage(chatId: String, messageId: String, request: EditMessageRequest): NetworkResult<MessageResponse>

    /**
     * Retrieves a paginated history of messages for a specific chat.
     *
     * @param chatId The ID of the chat.
     * @param page The zero-based page index.
     * @param size The requested number of messages per page.
     * @return A [NetworkResult] containing a list of [MessageResponse].
     */
    suspend fun loadChatHistory(chatId: String, page: Int, size: Int): NetworkResult<List<MessageResponse>>

    /**
     * Retrieves a paginated list of messages containing attachments of a specific category.
     *
     * @param chatId The ID of the chat.
     * @param type The attachment type category (e.g. PHOTO, VIDEO, DOCUMENT, VOICE, CIRCLE).
     * @param page The zero-based page index.
     * @param size The requested number of messages per page.
     * @return A [NetworkResult] containing a list of [MessageResponse].
     */
    suspend fun loadChatAttachments(chatId: String, type: String, page: Int, size: Int): NetworkResult<List<MessageResponse>>

    /**
     * Deletes a specific message.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message to delete.
     * @param forAll If true, attempts to delete the message for all participants.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean): NetworkResult<Unit>
    
    /**
     * Adds a reaction to a specific message.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message.
     * @param reaction The string or emoji representing the reaction.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun addReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit>

    /**
     * Removes a previously added reaction from a message.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message.
     * @param reaction The string or emoji representing the reaction to remove.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun removeReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit>

    /**
     * Pins a specific message in a chat.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message to pin.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun pinMessage(chatId: String, messageId: String): NetworkResult<Unit>

    /**
     * Unpins a previously pinned message.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message to unpin.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun unpinMessage(chatId: String, messageId: String): NetworkResult<Unit>
    
    /**
     * Retrieves all pinned messages in a specific chat.
     *
     * @param chatId The ID of the chat.
     * @return A [NetworkResult] containing a list of pinned [MessageResponse].
     */
    suspend fun getPinnedMessages(chatId: String): NetworkResult<List<MessageResponse>>

    /**
     * Marks a specific message as read.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message read by the current user.
     * @return A [NetworkResult] indicating success or failure.
     */
    suspend fun readMessage(chatId: String, messageId: String): NetworkResult<Unit>

    /**
     * Retrieves the list of users who have read a specific message.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the message.
     * @return A [NetworkResult] containing a list of [EnrichedUserResponse].
     */
    suspend fun getMessageReaders(chatId: String, messageId: String): NetworkResult<List<EnrichedUserResponse>>

    /**
     * Retrieves a paginated list of comments (replies) for a specific message thread.
     *
     * @param chatId The ID of the chat.
     * @param messageId The ID of the parent message.
     * @param page The zero-based page index.
     * @param size The requested number of comments per page.
     * @return A [NetworkResult] containing a list of [MessageResponse] representing the comments.
     */
    suspend fun getMessageComments(chatId: String, messageId: String, page: Int, size: Int): NetworkResult<List<MessageResponse>>
}
