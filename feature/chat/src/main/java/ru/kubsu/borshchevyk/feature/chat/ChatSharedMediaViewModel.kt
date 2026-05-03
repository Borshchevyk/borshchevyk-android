package ru.kubsu.borshchevyk.feature.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.message.GetAttachmentUrlUseCase
import ru.kubsu.borshchevyk.core.domain.message.GetThumbnailUrlUseCase
import ru.kubsu.borshchevyk.core.domain.message.LoadChatAttachmentsUseCase
import ru.kubsu.borshchevyk.core.model.domain.Message
import javax.inject.Inject

data class ChatSharedMediaUiState(
    val isLoading: Boolean = false,
    val isFetchingNextPage: Boolean = false,
    val error: String? = null,
    val selectedTab: MediaType = MediaType.PHOTO,
    val photoItems: List<Message> = emptyList(),
    val videoItems: List<Message> = emptyList(),
    val circleItems: List<Message> = emptyList(),
    val voiceItems: List<Message> = emptyList(),
    val fileItems: List<Message> = emptyList(),
    val isPhotoEndReached: Boolean = false,
    val isVideoEndReached: Boolean = false,
    val isCircleEndReached: Boolean = false,
    val isVoiceEndReached: Boolean = false,
    val isFileEndReached: Boolean = false,
    val attachmentUrls: Map<String, String> = emptyMap()
) {
    val currentItems: List<Message>
        get() = when (selectedTab) {
            MediaType.PHOTO -> photoItems
            MediaType.VIDEO -> videoItems
            MediaType.CIRCLE -> circleItems
            MediaType.VOICE -> voiceItems
            MediaType.FILE -> fileItems
        }

    val isCurrentEndReached: Boolean
        get() = when (selectedTab) {
            MediaType.PHOTO -> isPhotoEndReached
            MediaType.VIDEO -> isVideoEndReached
            MediaType.CIRCLE -> isCircleEndReached
            MediaType.VOICE -> isVoiceEndReached
            MediaType.FILE -> isFileEndReached
        }
}

enum class MediaType(val backendType: String, val displayName: String) {
    PHOTO("PHOTO", "Photo"),
    VIDEO("VIDEO", "Video"),
    CIRCLE("CIRCLE", "Circle"),
    VOICE("VOICE", "Voice"),
    FILE("FILE", "File")
}

@HiltViewModel
class ChatSharedMediaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadChatAttachmentsUseCase: LoadChatAttachmentsUseCase,
    private val getAttachmentUrlUseCase: GetAttachmentUrlUseCase,
    private val getThumbnailUrlUseCase: GetThumbnailUrlUseCase
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val pageSize = 20

    private val _uiState = MutableStateFlow(ChatSharedMediaUiState())
    val uiState: StateFlow<ChatSharedMediaUiState> = _uiState.asStateFlow()

    private var currentPhotoPage = 0
    private var currentVideoPage = 0
    private var currentCirclePage = 0
    private var currentVoicePage = 0
    private var currentFilePage = 0

    init {
        loadInitialData()
    }

    fun resolveUrl(attachmentId: String, isThumbnail: Boolean = false) {
        if (_uiState.value.attachmentUrls.containsKey(attachmentId)) return
        viewModelScope.launch {
            try {
                val url = if (isThumbnail) {
                    getThumbnailUrlUseCase(attachmentId)
                } else {
                    getAttachmentUrlUseCase(attachmentId)
                }
                _uiState.update { it.copy(attachmentUrls = it.attachmentUrls + (attachmentId to url)) }
            } catch (e: Exception) {
                Log.w("ChatSharedMediaVM", "Failed to resolve URL for attachment $attachmentId", e)
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
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

                _uiState.update {
                    it.copy(
                        photoItems = photos,
                        videoItems = videos,
                        circleItems = circles,
                        voiceItems = voices,
                        fileItems = files,
                        isPhotoEndReached = photos.size < pageSize,
                        isVideoEndReached = videos.size < pageSize,
                        isCircleEndReached = circles.size < pageSize,
                        isVoiceEndReached = voices.size < pageSize,
                        isFileEndReached = files.size < pageSize,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectTab(tab: MediaType) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadNextPage() {
        if (_uiState.value.isFetchingNextPage || _uiState.value.isCurrentEndReached) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingNextPage = true) }
            try {
                val tab = _uiState.value.selectedTab
                val page = when (tab) {
                    MediaType.PHOTO -> ++currentPhotoPage
                    MediaType.VIDEO -> ++currentVideoPage
                    MediaType.CIRCLE -> ++currentCirclePage
                    MediaType.VOICE -> ++currentVoicePage
                    MediaType.FILE -> ++currentFilePage
                }

                val newItems = loadChatAttachmentsUseCase(chatId, tab.backendType, page, pageSize)

                _uiState.update { state ->
                    val isEndReached = newItems.size < pageSize
                    when (tab) {
                        MediaType.PHOTO -> state.copy(photoItems = state.photoItems + newItems, isPhotoEndReached = isEndReached, isFetchingNextPage = false)
                        MediaType.VIDEO -> state.copy(videoItems = state.videoItems + newItems, isVideoEndReached = isEndReached, isFetchingNextPage = false)
                        MediaType.CIRCLE -> state.copy(circleItems = state.circleItems + newItems, isCircleEndReached = isEndReached, isFetchingNextPage = false)
                        MediaType.VOICE -> state.copy(voiceItems = state.voiceItems + newItems, isVoiceEndReached = isEndReached, isFetchingNextPage = false)
                        MediaType.FILE -> state.copy(fileItems = state.fileItems + newItems, isFileEndReached = isEndReached, isFetchingNextPage = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFetchingNextPage = false, error = e.message) }
            }
        }
    }
}
