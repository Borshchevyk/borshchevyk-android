package ru.kubsu.borshchevyk.feature.chat.conversation.mvi

sealed interface ChatEffect {
    data class ShowError(val message: String) : ChatEffect
    object NavigateBack : ChatEffect
    data class NavigateToSettings(val chatId: String) : ChatEffect
    data class NavigateToForwardSelection(val payloadJson: String) : ChatEffect
    data class NavigateToCall(val callId: String) : ChatEffect
}
