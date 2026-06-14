package ru.kubsu.borshchevyk.feature.chat.settings.mvi

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
    data class UpdateChatInfo(val title: String, val description: String?) : ChatSettingsIntent
}

sealed interface ChatSettingsEffect {
    data class ShowError(val message: String) : ChatSettingsEffect
    object NavigateBack : ChatSettingsEffect
}
