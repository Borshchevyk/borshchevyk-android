package ru.kubsu.borshchevyk.feature.call.ui.call

import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack

/**
 * Represents the entire UI state of the call screen.
 */
sealed interface CallUiState {
    /** Indicates that the call is currently connecting. */
    data object Loading : CallUiState
    
    /** Indicates that an error occurred during the call connection or active state.
     * @property message The localized error message to display.
     */
    data class Error(val message: String) : CallUiState
    
    /** Indicates an active ongoing call.
     * @property room The current LiveKit [Room] session.
     * @property localVideoTrack The video track for the local camera.
     * @property remoteVideoTrack The video track for the remote participant.
     * @property remoteParticipantName The display name of the remote participant.
     * @property isMicEnabled Whether the local microphone is currently unmuted.
     * @property isCameraEnabled Whether the local camera is currently streaming.
     * @property isRemoteMicMuted Whether the remote participant has muted their microphone.
     * @property isRemoteVideoMuted Whether the remote participant has muted their camera.
     * @property isMinimized Whether the call UI is minimized (e.g. into a banner or PiP).
     */
    data class Active(
        val room: Room,
        val localVideoTrack: VideoTrack? = null,
        val remoteVideoTrack: VideoTrack? = null,
        val remoteParticipantName: String? = null,
        val isMicEnabled: Boolean = true,
        val isCameraEnabled: Boolean = true,
        val isRemoteMicMuted: Boolean = false,
        val isRemoteVideoMuted: Boolean = false,
        val isMinimized: Boolean = false
    ) : CallUiState
}

/**
 * Represents intents (user actions) performed on the call screen.
 */
sealed interface CallIntent {
    /** Connects to the call room. */
    data object Connect : CallIntent
    /** Toggles the local microphone on/off. */
    data object ToggleMic : CallIntent
    /** Toggles the local camera on/off. */
    data object ToggleCamera : CallIntent
    /** Ends the current call and leaves the room. */
    data object EndCall : CallIntent
    /** Toggles the minimized/expanded state of the call UI. */
    data object ToggleMinimize : CallIntent
}

/**
 * Represents side-effects triggered by the call ViewModel.
 */
sealed interface CallEffect {
    /** Shows an error message to the user via a Snackbar or Toast.
     * @property message The error message to display.
     */
    data class ShowError(val message: String) : CallEffect
    /** Emitted when the call ends, prompting the UI to navigate back. */
    data object CallEnded : CallEffect
}