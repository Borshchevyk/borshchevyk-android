package ru.kubsu.borshchevyk.feature.call.ui.call

import io.livekit.android.room.Room

sealed interface CallUiState {
    data object Loading : CallUiState
    data class Error(val message: String) : CallUiState
    data class Active(
        val room: Room,
        val isMicEnabled: Boolean = true,
        val isCameraEnabled: Boolean = true,
        val isMinimized: Boolean = false
    ) : CallUiState
}

sealed interface CallIntent {
    data object Connect : CallIntent
    data object ToggleMic : CallIntent
    data object ToggleCamera : CallIntent
    data object EndCall : CallIntent
    data object ToggleMinimize : CallIntent
}

sealed interface CallEffect {
    data class ShowError(val message: String) : CallEffect
    data object CallEnded : CallEffect
}