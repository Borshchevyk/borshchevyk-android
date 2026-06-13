package ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi

import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.feature.chat.common.model.MediaType

sealed interface SharedMediaStateAction {
    data class LoadingStarted(val isFullLoad: Boolean = true) : SharedMediaStateAction
    data class LoadFailed(val error: String) : SharedMediaStateAction
    data class InitialDataLoaded(
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
    ) : SharedMediaStateAction
    data class TabSelected(val tab: MediaType) : SharedMediaStateAction
    object NextPageFetching : SharedMediaStateAction
    data class NextPageLoaded(val tab: MediaType, val newItems: List<Message>, val isEndReached: Boolean, val newPage: Int) : SharedMediaStateAction
    data class AttachmentUrlResolved(val attachmentId: String, val url: String) : SharedMediaStateAction
}

sealed interface SharedMediaIntent {
    object LoadInitialData : SharedMediaIntent
    data class SelectTab(val tab: MediaType) : SharedMediaIntent
    object LoadNextPage : SharedMediaIntent
    data class ResolveUrl(val attachmentId: String, val isThumbnail: Boolean = false) : SharedMediaIntent
}

sealed interface SharedMediaEffect {
    data class ShowError(val message: String) : SharedMediaEffect
}
