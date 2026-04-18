package ru.kubsu.borshchevyk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import ru.kubsu.borshchevyk.core.model.dto.ChatResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.model.dto.EditMessageRequest
import ru.kubsu.borshchevyk.core.model.dto.MessageResponse
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest
import ru.kubsu.borshchevyk.core.model.dto.TargetUserRequest
import ru.kubsu.borshchevyk.core.model.dto.UpdatePermissionsRequest
import javax.inject.Inject

class KtorChatNetworkDataSource @Inject constructor(
    private val httpClient: HttpClient
) : ChatNetworkDataSource {

    override suspend fun createChat(request: CreateChatRequest): ChatResponse {
        return httpClient.post("api/v1/chats") {
            setBody(request)
        }.body()
    }

    override suspend fun createPrivateChat(request: TargetUserRequest): ChatResponse {
        return httpClient.post("api/v1/chats/private") {
            setBody(request)
        }.body()
    }

    override suspend fun getUserChats(): List<ChatResponse> {
        return httpClient.get("api/v1/chats").body()
    }

    override suspend fun updatePermissions(
        chatId: String,
        targetUserId: String,
        request: UpdatePermissionsRequest
    ) {
        httpClient.patch("api/v1/chats/$chatId/members/$targetUserId/permissions") {
            setBody(request)
        }
    }

    override suspend fun clearChatHistory(chatId: String, forAll: Boolean) {
        httpClient.delete("api/v1/chats/$chatId/history") {
            parameter("forAll", forAll)
        }
    }

    override suspend fun deleteChat(chatId: String) {
        httpClient.delete("api/v1/chats/$chatId")
    }

    override suspend fun sendMessage(chatId: String, request: SendMessageRequest): MessageResponse {
        return httpClient.post("api/v1/chats/$chatId/messages") {
            setBody(request)
        }.body()
    }

    override suspend fun editMessage(chatId: String, messageId: String, request: EditMessageRequest): MessageResponse {
        return httpClient.put("api/v1/chats/$chatId/messages/$messageId") {
            setBody(request)
        }.body()
    }

    override suspend fun loadChatHistory(chatId: String, page: Int, size: Int): List<MessageResponse> {
        return httpClient.get("api/v1/chats/$chatId/messages") {
            parameter("page", page)
            parameter("size", size)
        }.body()
    }

    override suspend fun deleteMessage(chatId: String, messageId: String, forAll: Boolean) {
        httpClient.delete("api/v1/chats/$chatId/messages/$messageId") {
            parameter("forAll", forAll)
        }
    }

    override suspend fun addReaction(chatId: String, messageId: String, reaction: String) {
        httpClient.post("api/v1/chats/$chatId/messages/$messageId/reactions/$reaction")
    }

    override suspend fun removeReaction(chatId: String, messageId: String, reaction: String) {
        httpClient.delete("api/v1/chats/$chatId/messages/$messageId/reactions/$reaction")
    }

    override suspend fun pinMessage(chatId: String, messageId: String) {
        httpClient.post("api/v1/chats/$chatId/messages/$messageId/pin")
    }

    override suspend fun unpinMessage(chatId: String, messageId: String) {
        httpClient.post("api/v1/chats/$chatId/messages/$messageId/unpin")
    }

    override suspend fun getPinnedMessages(chatId: String): List<MessageResponse> {
        return httpClient.get("api/v1/chats/$chatId/messages/pinned").body()
    }

    override suspend fun readMessage(chatId: String, messageId: String) {
        httpClient.post("api/v1/chats/$chatId/messages/$messageId/read")
    }

    override suspend fun getMessageReaders(chatId: String, messageId: String): List<String> {
        return httpClient.get("api/v1/chats/$chatId/messages/$messageId/readers").body()
    }

    override suspend fun getMessageComments(
        chatId: String,
        messageId: String,
        page: Int,
        size: Int
    ): List<MessageResponse> {
        return httpClient.get("api/v1/chats/$chatId/messages/$messageId/comments") {
            parameter("page", page)
            parameter("size", size)
        }.body()
    }
}
