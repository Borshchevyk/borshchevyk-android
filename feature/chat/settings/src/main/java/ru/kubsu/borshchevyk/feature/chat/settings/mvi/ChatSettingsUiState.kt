package ru.kubsu.borshchevyk.feature.chat.settings.mvi

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import ru.kubsu.borshchevyk.core.model.domain.ChatMember

data class ChatSettingsUiState(
    val currentUserId: String = "",
    val isGroupChat: Boolean = false,
    val members: PersistentList<ChatMember> = persistentListOf(),
    val inviteLink: String? = null,
    val isChatDeleted: Boolean = false,
    val isContact: Boolean = false,
    val partnerId: String? = null,
    val partnerTag: String? = null,
    val partnerFirstName: String? = null,
    val partnerLastName: String? = null,
    val isDeletable: Boolean = true,
    val chatName: String = "",
    val chatDescription: String? = null,
    val chatAvatarUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
