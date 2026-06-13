package ru.kubsu.borshchevyk.feature.chat.sharedmedia

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.feature.chat.common.mvi.BaseMviViewModel
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.interactor.SharedMediaHandler
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi.ChatSharedMediaUiState
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi.SharedMediaEffect
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi.SharedMediaIntent
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi.SharedMediaStateAction
import ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi.reduce
import javax.inject.Inject

@HiltViewModel
class ChatSharedMediaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val handler: SharedMediaHandler
) : BaseMviViewModel<ChatSharedMediaUiState, SharedMediaStateAction, SharedMediaIntent, SharedMediaEffect>(ChatSharedMediaUiState()) {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val pageSize = 20

    override fun reduce(state: ChatSharedMediaUiState, action: SharedMediaStateAction): ChatSharedMediaUiState = state.reduce(action)

    init {
        handleIntent(SharedMediaIntent.LoadInitialData)
    }

    override fun handleIntent(intent: SharedMediaIntent) {
        when (intent) {
            is SharedMediaIntent.LoadInitialData -> {
                viewModelScope.launch {
                    dispatch(SharedMediaStateAction.LoadingStarted())
                    try {
                        val data = handler.loadInitialData(chatId, pageSize)
                        dispatch(SharedMediaStateAction.InitialDataLoaded(
                            photos = data.photos,
                            videos = data.videos,
                            circles = data.circles,
                            voices = data.voices,
                            files = data.files,
                            isPhotoEndReached = data.isPhotoEndReached,
                            isVideoEndReached = data.isVideoEndReached,
                            isCircleEndReached = data.isCircleEndReached,
                            isVoiceEndReached = data.isVoiceEndReached,
                            isFileEndReached = data.isFileEndReached
                        ))
                    } catch (e: Exception) {
                        dispatch(SharedMediaStateAction.LoadFailed(e.message ?: "Failed"))
                    }
                }
            }
            is SharedMediaIntent.SelectTab -> dispatch(SharedMediaStateAction.TabSelected(intent.tab))
            is SharedMediaIntent.LoadNextPage -> {
                val state = uiState.value
                if (state.isFetchingNextPage || state.isCurrentEndReached) return

                viewModelScope.launch {
                    dispatch(SharedMediaStateAction.NextPageFetching)
                    try {
                        val tab = state.selectedTab
                        val page = state.pages[tab] ?: 0
                        val newPage = page + 1
                        val newItems = handler.loadPage(chatId, tab.backendType, newPage, pageSize)
                        dispatch(SharedMediaStateAction.NextPageLoaded(tab, newItems, newItems.size < pageSize, newPage))
                    } catch (e: Exception) {
                        dispatch(SharedMediaStateAction.LoadFailed(e.message ?: "Failed"))
                    }
                }
            }
            is SharedMediaIntent.ResolveUrl -> {
                if (uiState.value.attachmentUrls.containsKey(intent.attachmentId)) return
                viewModelScope.launch {
                    try {
                        val url = handler.resolveUrl(intent.attachmentId, intent.isThumbnail)
                        dispatch(SharedMediaStateAction.AttachmentUrlResolved(intent.attachmentId, url))
                    } catch (e: Exception) { Log.w("SharedMedia", "Resolve failed", e) }
                }
            }
        }
    }
}
