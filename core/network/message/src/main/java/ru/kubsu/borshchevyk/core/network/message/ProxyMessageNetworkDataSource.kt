package ru.kubsu.borshchevyk.core.network.message

import ru.kubsu.borshchevyk.core.network.client.NetworkMode
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.client.TransportModeManager
import ru.kubsu.borshchevyk.core.network.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.EnrichedUserResponse
import ru.kubsu.borshchevyk.core.network.dto.MessageResponse
import ru.kubsu.borshchevyk.core.network.dto.SendMessageRequest
import javax.inject.Inject

class ProxyMessageNetworkDataSource @Inject constructor(
    private val ktorDataSource: KtorMessageNetworkDataSource,
    private val meshDataSource: MeshMessageNetworkDataSource,
    private val transportModeManager: TransportModeManager
) : MessageNetworkDataSource {

    private val currentDataSource: MessageNetworkDataSource
        get() = if (transportModeManager.networkMode.value == NetworkMode.MESH) {
            meshDataSource
        } else {
            ktorDataSource
        }

    override suspend fun sendMessage(chatId: String, request: SendMessageRequest): NetworkResult<MessageResponse> {
        return currentDataSource.sendMessage(chatId, request)
    }

    override suspend fun editMessage(chatId: String, messageId: String, request: EditMessageRequest): NetworkResult<MessageResponse> {
        return currentDataSource.editMessage(chatId, messageId, request)
    }

    override suspend fun loadChatHistory(chatId: String, page: Int, size: Int): NetworkResult<List<MessageResponse>> {
        return currentDataSource.loadChatHistory(chatId, page, size)
    }

    override suspend fun loadChatAttachments(chatId: String, type: String, page: Int, size: Int): NetworkResult<List<MessageResponse>> {
        return currentDataSource.loadChatAttachments(chatId, type, page, size)
    }

    override suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean): NetworkResult<Unit> {
        return currentDataSource.deleteMessage(chatId, messageId, forAll)
    }

    override suspend fun addReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit> {
        return currentDataSource.addReaction(chatId, messageId, reaction)
    }

    override suspend fun removeReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit> {
        return currentDataSource.removeReaction(chatId, messageId, reaction)
    }

    override suspend fun pinMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return currentDataSource.pinMessage(chatId, messageId)
    }

    override suspend fun unpinMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return currentDataSource.unpinMessage(chatId, messageId)
    }

    override suspend fun getPinnedMessages(chatId: String): NetworkResult<List<MessageResponse>> {
        return currentDataSource.getPinnedMessages(chatId)
    }

    override suspend fun readMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return currentDataSource.readMessage(chatId, messageId)
    }

    override suspend fun getMessageReaders(chatId: String, messageId: String): NetworkResult<List<EnrichedUserResponse>> {
        return currentDataSource.getMessageReaders(chatId, messageId)
    }

    override suspend fun getMessageComments(chatId: String, messageId: String, page: Int, size: Int): NetworkResult<List<MessageResponse>> {
        return currentDataSource.getMessageComments(chatId, messageId, page, size)
    }
}
