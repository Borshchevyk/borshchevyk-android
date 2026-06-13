package ru.kubsu.borshchevyk.feature.chat.sharedmedia.interactor

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.kubsu.borshchevyk.core.domain.message.GetAttachmentUrlUseCase
import ru.kubsu.borshchevyk.core.domain.message.GetThumbnailUrlUseCase
import ru.kubsu.borshchevyk.core.domain.message.LoadChatAttachmentsUseCase
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.feature.chat.common.model.MediaType
import javax.inject.Inject

data class SharedMediaData(
    val photos: List<Message>,
    val videos: List<Message>,
    val circles: List<Message>,
    val voices: List<Message>,
    val files: List<Message>,
    val isPhotoEndReached: Boolean,
    val isVideoEndReached: Boolean,
    val isCircleEndReached: Boolean,
    val isVoiceEndReached: Boolean,
    val isFileEndReached: Boolean
)

class SharedMediaHandler @Inject constructor(
    private val loadChatAttachmentsUseCase: LoadChatAttachmentsUseCase,
    private val getAttachmentUrlUseCase: GetAttachmentUrlUseCase,
    private val getThumbnailUrlUseCase: GetThumbnailUrlUseCase
) {
    suspend fun loadInitialData(chatId: String, pageSize: Int): SharedMediaData = coroutineScope {
        val photosDeferred = async { loadChatAttachmentsUseCase(chatId, MediaType.PHOTO.backendType, 0, pageSize) }
        val videosDeferred = async { loadChatAttachmentsUseCase(chatId, MediaType.VIDEO.backendType, 0, pageSize) }
        val circlesDeferred = async { loadChatAttachmentsUseCase(chatId, MediaType.CIRCLE.backendType, 0, pageSize) }
        val voicesDeferred = async { loadChatAttachmentsUseCase(chatId, MediaType.VOICE.backendType, 0, pageSize) }
        val filesDeferred = async { loadChatAttachmentsUseCase(chatId, MediaType.FILE.backendType, 0, pageSize) }

        val photos = photosDeferred.await()
        val videos = videosDeferred.await()
        val circles = circlesDeferred.await()
        val voices = voicesDeferred.await()
        val files = filesDeferred.await()

        SharedMediaData(
            photos = photos,
            videos = videos,
            circles = circles,
            voices = voices,
            files = files,
            isPhotoEndReached = photos.size < pageSize,
            isVideoEndReached = videos.size < pageSize,
            isCircleEndReached = circles.size < pageSize,
            isVoiceEndReached = voices.size < pageSize,
            isFileEndReached = files.size < pageSize
        )
    }

    suspend fun resolveUrl(attachmentId: String, isThumbnail: Boolean): String {
        return if (isThumbnail) {
            getThumbnailUrlUseCase(attachmentId)
        } else {
            getAttachmentUrlUseCase(attachmentId)
        }
    }

    suspend fun loadPage(chatId: String, backendType: String, page: Int, pageSize: Int): List<Message> {
        return loadChatAttachmentsUseCase(chatId, backendType, page, pageSize)
    }
}
