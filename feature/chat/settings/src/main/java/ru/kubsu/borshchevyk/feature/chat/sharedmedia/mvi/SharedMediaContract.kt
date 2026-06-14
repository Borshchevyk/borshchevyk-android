package ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi

import ru.kubsu.borshchevyk.feature.chat.common.model.MediaType

sealed interface SharedMediaIntent {
    object LoadInitialData : SharedMediaIntent
    data class SelectTab(val tab: MediaType) : SharedMediaIntent
    object LoadNextPage : SharedMediaIntent
    data class ResolveUrl(val attachmentId: String, val isThumbnail: Boolean) : SharedMediaIntent
}

sealed interface SharedMediaEffect {
    data class ShowError(val message: String) : SharedMediaEffect
}
