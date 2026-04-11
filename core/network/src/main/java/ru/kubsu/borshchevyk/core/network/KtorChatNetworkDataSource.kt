package ru.kubsu.borshchevyk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import ru.kubsu.borshchevyk.core.model.dto.ChatResponse
import ru.kubsu.borshchevyk.core.model.dto.CreateChatRequest
import ru.kubsu.borshchevyk.core.model.dto.MessageResponse
import ru.kubsu.borshchevyk.core.model.dto.SendMessageRequest
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
}
