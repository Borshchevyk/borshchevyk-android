package ru.kubsu.borshchevyk.feature.chat.handlers

import ru.kubsu.borshchevyk.core.domain.message.ChatMessageUseCases
import ru.kubsu.borshchevyk.core.domain.message.usecase.UploadCircleUseCase
import ru.kubsu.borshchevyk.core.domain.message.usecase.UploadVoiceUseCase
import javax.inject.Inject

/**
 * Handler responsible for sending rich media messages, specifically voice recordings
 * and circular video messages (circles) within a chat.
 *
 * @property uploadVoiceUseCase Use case for uploading voice recordings.
 * @property uploadCircleUseCase Use case for uploading circular video recordings.
 * @property messageUseCases Use cases for sending the final chat message.
 */
class MediaVoiceHandler @Inject constructor(
    private val uploadVoiceUseCase: UploadVoiceUseCase,
    private val uploadCircleUseCase: UploadCircleUseCase,
    private val messageUseCases: ChatMessageUseCases
) {
    /**
     * Uploads a voice recording and sends it as a message to the specified chat.
     *
     * @param chatId The unique identifier of the target chat.
     * @param bytes The raw byte array containing the voice recording data.
     * @param duration The duration of the voice recording in seconds.
     */
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

    /**
     * Uploads a circular video recording and sends it as a message to the specified chat.
     *
     * @param chatId The unique identifier of the target chat.
     * @param bytes The raw byte array containing the circular video data.
     * @param duration The duration of the video in seconds.
     */
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
