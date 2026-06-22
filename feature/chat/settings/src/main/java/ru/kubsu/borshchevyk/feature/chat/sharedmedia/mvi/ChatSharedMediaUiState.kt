package ru.kubsu.borshchevyk.feature.chat.sharedmedia.mvi

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import ru.kubsu.borshchevyk.core.model.domain.Message
import ru.kubsu.borshchevyk.feature.chat.common.model.MediaType

data class ChatSharedMediaUiState(
    val isLoading: Boolean = false,
    val isFetchingNextPage: Boolean = false,
    val error: String? = null,
    val selectedTab: MediaType = MediaType.PHOTO,
    val photoItems: PersistentList<Message> = persistentListOf(),
    val videoItems: PersistentList<Message> = persistentListOf(),
    val circleItems: PersistentList<Message> = persistentListOf(),
    val voiceItems: PersistentList<Message> = persistentListOf(),
    val fileItems: PersistentList<Message> = persistentListOf(),
    val isPhotoEndReached: Boolean = false,
    val isVideoEndReached: Boolean = false,
    val isCircleEndReached: Boolean = false,
    val isVoiceEndReached: Boolean = false,
    val isFileEndReached: Boolean = false,
    val attachmentUrls: PersistentMap<String, String> = persistentMapOf(),
    val pages: PersistentMap<MediaType, Int> = MediaType.entries.associateWith { 0 }.toPersistentMap(),
    val selectedAttachmentForViewing: ru.kubsu.borshchevyk.core.model.domain.Attachment? = null
) {
    val currentItems: PersistentList<Message>
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
