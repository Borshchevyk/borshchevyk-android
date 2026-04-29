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

@Singleton
class CallHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "CallHandler"
    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var room: Room? = null
    
    private val _state = MutableStateFlow<CallSessionState>(CallSessionState.Idle)
    val state: StateFlow<CallSessionState> = _state.asStateFlow()

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

    suspend fun toggleMic(enabled: Boolean) {
        room?.localParticipant?.setMicrophoneEnabled(enabled)
        updateState()
    }

    suspend fun toggleCamera(enabled: Boolean) {
        room?.localParticipant?.setCameraEnabled(enabled)
        updateState()
    }

    fun disconnect() {
        room?.disconnect()
        room?.release()
        room = null
        _state.value = CallSessionState.Idle
    }

    private fun updateState() {
        val r = room ?: return
        
        val localVideoTrack = r.localParticipant.videoTrackPublications.firstOrNull()?.first?.track as? VideoTrack
        
        val firstRemote = r.remoteParticipants.values.firstOrNull()
        val remoteVideoTrack = firstRemote?.videoTrackPublications?.firstOrNull()?.first?.track as? VideoTrack
        val isRemoteMicMuted = firstRemote?.audioTrackPublications?.firstOrNull()?.first?.muted ?: false
        
        _state.update {
            CallSessionState.Active(
                room = r,
                localVideoTrack = localVideoTrack,
                remoteVideoTrack = remoteVideoTrack,
                remoteParticipantName = firstRemote?.identity?.value,
                isMicEnabled = r.localParticipant.isMicrophoneEnabled,
                isCameraEnabled = r.localParticipant.isCameraEnabled,
                isRemoteMicMuted = isRemoteMicMuted
            )
        }
    }
}

sealed interface CallSessionState {
    data object Idle : CallSessionState
    data class Active(
        val room: Room,
        val localVideoTrack: VideoTrack?,
        val remoteVideoTrack: VideoTrack?,
        val remoteParticipantName: String?,
        val isMicEnabled: Boolean,
        val isCameraEnabled: Boolean,
        val isRemoteMicMuted: Boolean
    ) : CallSessionState
}