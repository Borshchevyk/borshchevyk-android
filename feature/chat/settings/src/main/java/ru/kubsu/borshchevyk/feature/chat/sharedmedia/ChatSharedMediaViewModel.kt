package ru.kubsu.borshchevyk.feature.chat.sharedmedia

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.ui.mvi.mviContainer
import ru.kubsu.borshchevyk.feature.chat.common.model.MediaType
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.interactor.SharedMediaHandler
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi.ChatSharedMediaUiState
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi.SharedMediaEffect
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi.SharedMediaIntent
import javax.inject.Inject

@HiltViewModel
class ChatSharedMediaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val handler: SharedMediaHandler
) : ViewModel() {

    private val container = mviContainer<ChatSharedMediaUiState, SharedMediaEffect>(ChatSharedMediaUiState(), viewModelScope)
    val uiState = container.uiState
    val effect = container.effect

    private fun sendEffect(effect: SharedMediaEffect) {
        container.sendEffect(effect)
    }

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val pageSize = 20

    init {
        handleIntent(SharedMediaIntent.LoadInitialData)
    }

    fun handleIntent(intent: SharedMediaIntent) {
        when (intent) {
            is SharedMediaIntent.LoadInitialData -> {
                viewModelScope.launch {
                    container.updateState { it.copy(isLoading = true, error = null) }
                    try {
                        val data = handler.loadInitialData(chatId, pageSize)
                        container.updateState { it.copy(
                            photoItems = data.photos.toPersistentList(),
                            videoItems = data.videos.toPersistentList(),
                            circleItems = data.circles.toPersistentList(),
                            voiceItems = data.voices.toPersistentList(),
                            fileItems = data.files.toPersistentList(),
                            isPhotoEndReached = data.isPhotoEndReached,
                            isVideoEndReached = data.isVideoEndReached,
                            isCircleEndReached = data.isCircleEndReached,
                            isVoiceEndReached = data.isVoiceEndReached,
                            isFileEndReached = data.isFileEndReached,
                            isLoading = false
                        ) }
                    } catch (e: Exception) {
                        container.updateState { it.copy(isLoading = false, isFetchingNextPage = false, error = e.message ?: "Failed") }
                    }
                }
            }
            is SharedMediaIntent.SelectTab -> container.updateState { it.copy(selectedTab = intent.tab) }
            is SharedMediaIntent.LoadNextPage -> {
                val state = uiState.value
                if (state.isFetchingNextPage || state.isCurrentEndReached) return

                viewModelScope.launch {
                    container.updateState { it.copy(isFetchingNextPage = true) }
                    try {
                        val tab = state.selectedTab
                        val page = state.pages[tab] ?: 0
                        val newPage = page + 1
                        val newItems = handler.loadPage(chatId, tab.backendType, newPage, pageSize)
                        
                        container.updateState { current ->
                            val builder = current.pages.builder()
                            builder[tab] = newPage
                            val isEndReached = newItems.size < pageSize
                            
                            when (tab) {
                                MediaType.PHOTO -> current.copy(photoItems = (current.photoItems + newItems).toPersistentList(), isPhotoEndReached = isEndReached, isFetchingNextPage = false, pages = builder.build())
                                MediaType.VIDEO -> current.copy(videoItems = (current.videoItems + newItems).toPersistentList(), isVideoEndReached = isEndReached, isFetchingNextPage = false, pages = builder.build())
                                MediaType.CIRCLE -> current.copy(circleItems = (current.circleItems + newItems).toPersistentList(), isCircleEndReached = isEndReached, isFetchingNextPage = false, pages = builder.build())
                                MediaType.VOICE -> current.copy(voiceItems = (current.voiceItems + newItems).toPersistentList(), isVoiceEndReached = isEndReached, isFetchingNextPage = false, pages = builder.build())
                                MediaType.FILE -> current.copy(fileItems = (current.fileItems + newItems).toPersistentList(), isFileEndReached = isEndReached, isFetchingNextPage = false, pages = builder.build())
                            }
                        }
                    } catch (e: Exception) {
                        container.updateState { it.copy(isLoading = false, isFetchingNextPage = false, error = e.message ?: "Failed") }
                    }
                }
            }
            is SharedMediaIntent.ResolveUrl -> {
                val key = if (intent.isThumbnail) "${intent.attachmentId}_thumb" else intent.attachmentId
                if (uiState.value.attachmentUrls.containsKey(key)) return
                viewModelScope.launch {
                    try {
                        val url = handler.resolveUrl(intent.attachmentId, intent.isThumbnail)
                        container.updateState { current ->
                            val builder = current.attachmentUrls.builder()
                            builder[key] = url
                            current.copy(attachmentUrls = builder.build())
                        }
                    } catch (e: Exception) { Log.w("SharedMedia", "Resolve failed", e) }
                }
            }
            is SharedMediaIntent.OpenMediaViewer -> container.updateState { it.copy(selectedAttachmentForViewing = intent.attachment) }
            is SharedMediaIntent.CloseMediaViewer -> container.updateState { it.copy(selectedAttachmentForViewing = null) }
        }
    }
}
