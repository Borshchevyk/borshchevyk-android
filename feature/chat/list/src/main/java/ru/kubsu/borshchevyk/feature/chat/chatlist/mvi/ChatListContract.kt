package ru.kubsu.borshchevyk.feature.chat.chatlist.mvi

sealed interface ChatListIntent {
    object LoadChats : ChatListIntent
    object ToggleNetworkMode : ChatListIntent
    data class PinChat(val chatId: String) : ChatListIntent
    data class UnpinChat(val chatId: String) : ChatListIntent
    data class CreatePrivateChat(val targetUserId: String) : ChatListIntent
    data class CreateGroupChat(val title: String, val description: String?) : ChatListIntent
    data class JoinChat(val inviteCode: String) : ChatListIntent
    data class CreatePrivateChatByTag(val tag: String) : ChatListIntent
}

sealed interface ChatListEffect {
    data class ShowError(val message: String) : ChatListEffect
    data class NavigateToChat(val chatId: String) : ChatListEffect
}
