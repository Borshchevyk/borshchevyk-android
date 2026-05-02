package ru.kubsu.borshchevyk.feature.call.ui.call

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.network.client.NetworkConstants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A singleton handler responsible for managing the low-level LiveKit WebRTC connection.
 * It encapsulates the complexities of [Room] management, tracking tracks, participants,
 * and mapping LiveKit events to the [CallSessionState].
 *
 * @property context The application context used for initializing LiveKit.
 */
@Singleton
class CallHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "CallHandler"
    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var room: Room? = null
    
    private val _state = MutableStateFlow<CallSessionState>(CallSessionState.Idle)
    /** StateFlow exposing the current active status of the WebRTC session. */
    val state: StateFlow<CallSessionState> = _state.asStateFlow()

    /**
     * Connects to a LiveKit room using the provided access token.
     *
     * @param token The LiveKit access token.
     * @throws Exception if connection fails.
     */
    suspend fun connect(token: String) {
        if (room != null) return
        
        try {
            val r = LiveKit.create(context)
            room = r
            
            handlerScope.launch {
                r.events.collect { event ->
                    when (event) {
                        is RoomEvent.ParticipantConnected,
                        is RoomEvent.ParticipantDisconnected,
                        is RoomEvent.TrackSubscribed,
                        is RoomEvent.TrackUnsubscribed,
                        is RoomEvent.TrackMuted,
                        is RoomEvent.TrackUnmuted -> {
                            updateState()
                        }
                        is RoomEvent.Disconnected -> {
                            _state.value = CallSessionState.Idle
                            room = null
                        }
                        else -> {}
                    }
                }
            }

            r.connect(NetworkConstants.LIVEKIT_URL, token)
            r.localParticipant.setMicrophoneEnabled(true)
            r.localParticipant.setCameraEnabled(true)
            updateState()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to LiveKit", e)
            room = null
            throw e
        }
    }

    /**
     * Toggles the local microphone state.
     * @param enabled True to enable the microphone, false to mute it.
     */
    suspend fun toggleMic(enabled: Boolean) {
        room?.localParticipant?.setMicrophoneEnabled(enabled)
        updateState()
    }

    /**
     * Toggles the local camera state.
     * @param enabled True to enable the camera, false to turn it off.
     */
    suspend fun toggleCamera(enabled: Boolean) {
        room?.localParticipant?.setCameraEnabled(enabled)
        updateState()
    }

    /**
     * Disconnects from the current room and resets the session state to Idle.
     */
    fun disconnect() {
        room?.disconnect()
        room?.release()
        room = null
        _state.value = CallSessionState.Idle
    }

    /**
     * Updates the internal session state based on the current LiveKit room configuration.
     * Extracts local and remote media tracks, participant information, and hardware states,
     * emitting the updated [CallSessionState.Active] to observers.
     */
    private fun updateState() {
        val r = room ?: return
        
        val localVideoTrack = r.localParticipant.videoTrackPublications.firstOrNull()?.first?.track as? VideoTrack
        
        val firstRemote = r.remoteParticipants.values.firstOrNull()
        val remoteVideoTrack = firstRemote?.videoTrackPublications?.firstOrNull()?.first?.track as? VideoTrack
        val isRemoteMicMuted = firstRemote?.audioTrackPublications?.firstOrNull()?.first?.muted ?: false
        val isRemoteVideoMuted = firstRemote?.videoTrackPublications?.firstOrNull()?.first?.muted ?: false
        
        _state.update {
            CallSessionState.Active(
                room = r,
                localVideoTrack = localVideoTrack,
                remoteVideoTrack = remoteVideoTrack,
                remoteParticipantName = firstRemote?.identity?.value,
                isMicEnabled = r.localParticipant.isMicrophoneEnabled,
                isCameraEnabled = r.localParticipant.isCameraEnabled,
                isRemoteMicMuted = isRemoteMicMuted,
                isRemoteVideoMuted = isRemoteVideoMuted
            )
        }
    }
}

/**
 * Represents the low-level WebRTC session state emitted by the [CallHandler].
 */
sealed interface CallSessionState {
    /** Indicates there is no active session. */
    data object Idle : CallSessionState
    /** Indicates an active session with specific tracks and participant details. */
    data class Active(
        val room: Room,
        val localVideoTrack: VideoTrack?,
        val remoteVideoTrack: VideoTrack?,
        val remoteParticipantName: String?,
        val isMicEnabled: Boolean,
        val isCameraEnabled: Boolean,
        val isRemoteMicMuted: Boolean,
        val isRemoteVideoMuted: Boolean
    ) : CallSessionState
}