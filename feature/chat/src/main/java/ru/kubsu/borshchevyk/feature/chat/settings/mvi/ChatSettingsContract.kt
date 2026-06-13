package ru.kubsu.borshchevyk.feature.chat.settings.mvi

import ru.kubsu.borshchevyk.core.model.domain.ChatMember

sealed interface ChatSettingsStateAction {
    data class LoadingStarted(val isFullLoad: Boolean = true) : ChatSettingsStateAction
    data class LoadFailed(val error: String) : ChatSettingsStateAction
    data class InitialDataLoaded(
        val currentUserId: String,
        val isGroupChat: Boolean,
        val members: List<ChatMember>,
        val isContact: Boolean,
        val partnerId: String?,
        val partnerTag: String?,
        val partnerFirstName: String?,
        val partnerLastName: String?,
        val isDeletable: Boolean,
        val chatName: String,
        val chatDescription: String?,
        val chatAvatarUrl: String?
    ) : ChatSettingsStateAction
    data class ContactStatusUpdated(val isContact: Boolean) : ChatSettingsStateAction
    data class MembersUpdated(val members: List<ChatMember>) : ChatSettingsStateAction
    data class InviteLinkGenerated(val link: String) : ChatSettingsStateAction
    object ChatDeleted : ChatSettingsStateAction
}

sealed interface ChatSettingsIntent {
    object LoadData : ChatSettingsIntent
    data class AddContact(val firstName: String, val lastName: String?) : ChatSettingsIntent
    object RemoveContact : ChatSettingsIntent
    data class InviteUser(val userId: String) : ChatSettingsIntent
    object GenerateInviteLink : ChatSettingsIntent
    data class UpdatePermissions(
        val targetUserId: String,
        val canSendMessages: Boolean,
        val canDeleteMessages: Boolean,
        val canInviteUsers: Boolean,
        val canChangeInfo: Boolean
    ) : ChatSettingsIntent
    data class ClearHistory(val forAll: Boolean) : ChatSettingsIntent
    object DeleteChat : ChatSettingsIntent
    data class KickUser(val targetUserId: String) : ChatSettingsIntent
    object LeaveChat : ChatSettingsIntent
    data class UpdateChatInfo(val title: String?, val description: String?) : ChatSettingsIntent
}

sealed interface ChatSettingsEffect {
    data class ShowError(val message: String) : ChatSettingsEffect
    object NavigateBack : ChatSettingsEffect
}
