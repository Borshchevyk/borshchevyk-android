package ru.kubsu.borshchevyk.feature.chat.settings.mvi

import kotlinx.collections.immutable.toPersistentList

fun ChatSettingsUiState.reduce(action: ChatSettingsStateAction): ChatSettingsUiState {
    return when (action) {
        is ChatSettingsStateAction.LoadingStarted -> this.copy(isLoading = action.isFullLoad, error = null)
        is ChatSettingsStateAction.LoadFailed -> this.copy(isLoading = false, error = action.error)
        is ChatSettingsStateAction.InitialDataLoaded -> this.copy(
            isLoading = false,
            currentUserId = action.currentUserId,
            isGroupChat = action.isGroupChat,
            members = action.members.toPersistentList(),
            isContact = action.isContact,
            partnerId = action.partnerId,
            partnerTag = action.partnerTag,
            partnerFirstName = action.partnerFirstName,
            partnerLastName = action.partnerLastName,
            isDeletable = action.isDeletable,
            chatName = action.chatName,
            chatDescription = action.chatDescription,
            chatAvatarUrl = action.chatAvatarUrl
        )
        is ChatSettingsStateAction.ContactStatusUpdated -> this.copy(isContact = action.isContact)
        is ChatSettingsStateAction.MembersUpdated -> this.copy(members = action.members.toPersistentList())
        is ChatSettingsStateAction.InviteLinkGenerated -> this.copy(inviteLink = action.link)
        is ChatSettingsStateAction.ChatDeleted -> this.copy(isChatDeleted = true)
    }
}
