package ru.kubsu.borshchevyk.core.network.message

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.kubsu.borshchevyk.core.network.client.NetworkResult
import ru.kubsu.borshchevyk.core.network.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.network.dto.EnrichedUserResponse
import ru.kubsu.borshchevyk.core.network.dto.MessageResponse
import ru.kubsu.borshchevyk.core.network.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.network.client.safeRequest
import ru.kubsu.borshchevyk.core.network.di.IoDispatcher
import javax.inject.Inject

class KtorMessageNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MessageNetworkDataSource {

    override suspend fun sendMessage(chatId: String, request: SendMessageRequest): NetworkResult<MessageResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/chats/$chatId/messages") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun editMessage(chatId: String, messageId: String, request: EditMessageRequest): NetworkResult<MessageResponse> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.put("api/v1/chats/$chatId/messages/$messageId") {
                    setBody(request)
                }
            }
        }
    }

    override suspend fun loadChatHistory(chatId: String, page: Int, size: Int): NetworkResult<List<MessageResponse>> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/chats/$chatId/messages") {
                    parameter("page", page)
                    parameter("size", size)
                }
            }
        }
    }

    override suspend fun loadChatAttachments(chatId: String, type: String, page: Int, size: Int): NetworkResult<List<MessageResponse>> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/chats/$chatId/messages/attachments") {
                    parameter("type", type)
                    parameter("page", page)
                    parameter("size", size)
                }
            }
        }
    }

    override suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.delete("api/v1/chats/$chatId/messages/$messageId") {
                    parameter("forAll", forAll)
                }
            }
        }
    }

    override suspend fun addReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/chats/$chatId/messages/$messageId/reactions/$reaction")
            }
        }
    }

    override suspend fun removeReaction(chatId: String, messageId: String, reaction: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.delete("api/v1/chats/$chatId/messages/$messageId/reactions/$reaction")
            }
        }
    }

    override suspend fun pinMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/chats/$chatId/messages/$messageId/pin")
            }
        }
    }

    override suspend fun unpinMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/chats/$chatId/messages/$messageId/unpin")
            }
        }
    }

    override suspend fun getPinnedMessages(chatId: String): NetworkResult<List<MessageResponse>> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/chats/$chatId/messages/pinned")
            }
        }
    }

    override suspend fun readMessage(chatId: String, messageId: String): NetworkResult<Unit> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.post("api/v1/chats/$chatId/messages/$messageId/read")
            }
        }
    }

    override suspend fun getMessageReaders(chatId: String, messageId: String): NetworkResult<List<EnrichedUserResponse>> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/chats/$chatId/messages/$messageId/readers")
            }
        }
    }

    override suspend fun getMessageComments(
        chatId: String,
        messageId: String,
        page: Int,
        size: Int
    ): NetworkResult<List<MessageResponse>> {
        return withContext(ioDispatcher) {
            safeRequest {
                httpClient.get("api/v1/chats/$chatId/messages/$messageId/comments") {
                    parameter("page", page)
                    parameter("size", size)
                }
            }
        }
    }
}
