package ru.kubsu.borshchevyk.feature.chat.conversation.interactor

import ru.kubsu.borshchevyk.core.domain.message.ChatMessageUseCases
import ru.kubsu.borshchevyk.core.domain.message.usecase.UploadCircleUseCase
import ru.kubsu.borshchevyk.core.domain.message.usecase.UploadVoiceUseCase
import javax.inject.Inject

class MediaVoiceHandler @Inject constructor(
    private val uploadVoiceUseCase: UploadVoiceUseCase,
    private val uploadCircleUseCase: UploadCircleUseCase,
    private val messageUseCases: ChatMessageUseCases
) {
    suspend fun sendVoice(chatId: String, bytes: ByteArray, duration: Double) {
        val attachmentResponse = uploadVoiceUseCase(bytes, duration)
        messageUseCases.sendMessage(
            chatId = chatId,
            text = "",
            attachmentIds = listOf(attachmentResponse.id),
            forwardedFromChatId = null,
            forwardedFromUserId = null
        )
    }

    suspend fun sendCircle(chatId: String, bytes: ByteArray, duration: Double) {
        val attachmentResponse = uploadCircleUseCase(bytes, duration)
        messageUseCases.sendMessage(
            chatId = chatId,
            text = "",
            attachmentIds = listOf(attachmentResponse.id),
            forwardedFromChatId = null,
            forwardedFromUserId = null
        )
    }
}
