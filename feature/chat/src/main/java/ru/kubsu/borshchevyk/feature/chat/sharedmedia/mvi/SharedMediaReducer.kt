package ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi

import kotlinx.collections.immutable.toPersistentList
import ru.kubsu.borshchevyk.feature.chat.common.model.MediaType

fun ChatSharedMediaUiState.reduce(action: SharedMediaStateAction): ChatSharedMediaUiState {
    return when (action) {
        is SharedMediaStateAction.LoadingStarted -> this.copy(isLoading = action.isFullLoad, error = null)
        is SharedMediaStateAction.LoadFailed -> this.copy(isLoading = false, isFetchingNextPage = false, error = action.error)
        is SharedMediaStateAction.InitialDataLoaded -> this.copy(
            photoItems = action.photos.toPersistentList(),
            videoItems = action.videos.toPersistentList(),
            circleItems = action.circles.toPersistentList(),
            voiceItems = action.voices.toPersistentList(),
            fileItems = action.files.toPersistentList(),
            isPhotoEndReached = action.isPhotoEndReached,
            isVideoEndReached = action.isVideoEndReached,
            isCircleEndReached = action.isCircleEndReached,
            isVoiceEndReached = action.isVoiceEndReached,
            isFileEndReached = action.isFileEndReached,
            isLoading = false
        )
        is SharedMediaStateAction.TabSelected -> this.copy(selectedTab = action.tab)
        is SharedMediaStateAction.NextPageFetching -> this.copy(isFetchingNextPage = true)
        is SharedMediaStateAction.NextPageLoaded -> {
            val builder = this.pages.builder()
            builder[action.tab] = action.newPage
            when (action.tab) {
                MediaType.PHOTO -> this.copy(photoItems = (this.photoItems + action.newItems).toPersistentList(), isPhotoEndReached = action.isEndReached, isFetchingNextPage = false, pages = builder.build())
                MediaType.VIDEO -> this.copy(videoItems = (this.videoItems + action.newItems).toPersistentList(), isVideoEndReached = action.isEndReached, isFetchingNextPage = false, pages = builder.build())
                MediaType.CIRCLE -> this.copy(circleItems = (this.circleItems + action.newItems).toPersistentList(), isCircleEndReached = action.isEndReached, isFetchingNextPage = false, pages = builder.build())
                MediaType.VOICE -> this.copy(voiceItems = (this.voiceItems + action.newItems).toPersistentList(), isVoiceEndReached = action.isEndReached, isFetchingNextPage = false, pages = builder.build())
                MediaType.FILE -> this.copy(fileItems = (this.fileItems + action.newItems).toPersistentList(), isFileEndReached = action.isEndReached, isFetchingNextPage = false, pages = builder.build())
            }
        }
        is SharedMediaStateAction.AttachmentUrlResolved -> {
            val builder = this.attachmentUrls.builder()
            builder[action.attachmentId] = action.url
            this.copy(attachmentUrls = builder.build())
        }
    }
}
