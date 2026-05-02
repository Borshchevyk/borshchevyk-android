package ru.kubsu.borshchevyk.feature.call.ui.call

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.call.usecase.EndCallUseCase
import ru.kubsu.borshchevyk.core.domain.call.usecase.JoinCallUseCase
import ru.kubsu.borshchevyk.core.domain.call.usecase.LeaveCallUseCase
import ru.kubsu.borshchevyk.core.domain.call.usecase.ObserveCallEventsUseCase
import javax.inject.Inject

/**
 * ViewModel managing the active call screen logic and state.
 * It interacts with the LiveKit [CallHandler] to manage WebRTC connections
 * and observes domain call events to handle external signaling (e.g. call rejected or ended by remote).
 *
 * @property savedStateHandle Provides navigation arguments (e.g., `callId` and `isInitiator`).
 * @property callHandler The singleton managing the LiveKit room session.
 * @property joinCallUseCase Retrieves the LiveKit access token for the room.
 * @property endCallUseCase Ends the call for all participants (if initiator).
 * @property leaveCallUseCase Leaves the call (if participant).
 * @property observeCallEventsUseCase Observes signaling events to react to remote actions.
 */
@HiltViewModel
class CallViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val callHandler: CallHandler,
    private val joinCallUseCase: JoinCallUseCase,
    private val endCallUseCase: EndCallUseCase,
    private val leaveCallUseCase: LeaveCallUseCase,
    private val observeCallEventsUseCase: ObserveCallEventsUseCase
) : ViewModel() {

    private val TAG = "CallViewModel"
    
    /** The unique ID of the current call session. */
    val callId: String = checkNotNull(savedStateHandle["callId"])
    
    /** Indicates if the current user initiated this call. */
    val isInitiator: Boolean = savedStateHandle["isInitiator"] ?: false

    private val _uiState = MutableStateFlow<CallUiState>(CallUiState.Loading)
    /** StateFlow emitting the current [CallUiState]. */
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val _effect = Channel<CallEffect>(Channel.BUFFERED)
    /** Flow of one-time [CallEffect]s to be handled by the UI. */
    val effect = _effect.receiveAsFlow()

    private var connectJob: Job? = null

    init {
        observeCallEvents()
        observeSessionState()
    }

    /**
     * Observes call signaling events from the domain layer (e.g., call rejected or ended).
     */
    private fun observeCallEvents() {
        viewModelScope.launch {
            observeCallEventsUseCase().collect { event ->
                if (event.callId == callId) {
                    when (event.type) {
                        "REJECTED" -> {
                            handleCallEnded("Call rejected")
                        }
                        "ENDED" -> {
                            handleCallEnded()
                        }
                    }
                }
            }
        }
    }

    /**
     * Observes the WebRTC session state from the [CallHandler] to update the UI accordingly.
     */
    private fun observeSessionState() {
        viewModelScope.launch {
            callHandler.state.collect { sessionState ->
                when (sessionState) {
                    is CallSessionState.Idle -> {
                        if (_uiState.value is CallUiState.Active) {
                            handleCallEnded()
                        }
                    }
                    is CallSessionState.Active -> {
                        _uiState.update { currentState ->
                            if (currentState is CallUiState.Active) {
                                currentState.copy(
                                    room = sessionState.room,
                                    localVideoTrack = sessionState.localVideoTrack,
                                    remoteVideoTrack = sessionState.remoteVideoTrack,
                                    remoteParticipantName = sessionState.remoteParticipantName,
                                    isMicEnabled = sessionState.isMicEnabled,
                                    isCameraEnabled = sessionState.isCameraEnabled,
                                    isRemoteMicMuted = sessionState.isRemoteMicMuted,
                                    isRemoteVideoMuted = sessionState.isRemoteVideoMuted
                                )
                            } else {
                                CallUiState.Active(
                                    room = sessionState.room,
                                    localVideoTrack = sessionState.localVideoTrack,
                                    remoteVideoTrack = sessionState.remoteVideoTrack,
                                    remoteParticipantName = sessionState.remoteParticipantName,
                                    isMicEnabled = sessionState.isMicEnabled,
                                    isCameraEnabled = sessionState.isCameraEnabled,
                                    isRemoteMicMuted = sessionState.isRemoteMicMuted,
                                    isRemoteVideoMuted = sessionState.isRemoteVideoMuted,
                                    isMinimized = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles incoming intents from the UI.
     * @param intent The [CallIntent] to process.
     */
    fun handleIntent(intent: CallIntent) {
        when (intent) {
            is CallIntent.Connect -> connectToCall()
            is CallIntent.ToggleMic -> toggleMic()
            is CallIntent.ToggleCamera -> toggleCamera()
            is CallIntent.EndCall -> endCall()
            is CallIntent.ToggleMinimize -> toggleMinimize()
        }
    }

    /**
     * Connects to the active call session using the provided token.
     */
    private fun connectToCall() {
        if (connectJob?.isActive == true || _uiState.value is CallUiState.Active) return

        connectJob = viewModelScope.launch {
            try {
                val token = joinCallUseCase(callId)
                callHandler.connect(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to call", e)
                _uiState.value = CallUiState.Error(e.message ?: "Failed to connect")
                _effect.send(CallEffect.ShowError(e.message ?: "Failed to connect to call"))
            }
        }
    }

    /**
     * Toggles the user's microphone state (mute/unmute) in the active call.
     */
    private fun toggleMic() {
        val state = _uiState.value as? CallUiState.Active ?: return
        viewModelScope.launch {
            callHandler.toggleMic(!state.isMicEnabled)
        }
    }

    /**
     * Toggles the user's camera state (on/off) in the active call.
     */
    private fun toggleCamera() {
        val state = _uiState.value as? CallUiState.Active ?: return
        viewModelScope.launch {
            callHandler.toggleCamera(!state.isCameraEnabled)
        }
    }

    /**
     * Ends or leaves the current call depending on whether the user is the initiator.
     */
    private fun endCall() {
        viewModelScope.launch {
            try {
                callHandler.disconnect()
                if (isInitiator) {
                    endCallUseCase(callId)
                } else {
                    leaveCallUseCase(callId)
                }
                _effect.send(CallEffect.CallEnded)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to end call", e)
                _effect.send(CallEffect.CallEnded) // Still end UI even if signaling fails
            }
        }
    }

    /**
     * Handles the logic for when a call is ended, updating the UI state and disconnecting the handler.
     *
     * @param message An optional error message or reason for the call ending.
     */
    private fun handleCallEnded(message: String? = null) {
        viewModelScope.launch {
            message?.let {
                _uiState.value = CallUiState.Error(it)
                _effect.send(CallEffect.ShowError(it))
            }
            callHandler.disconnect()
            _effect.send(CallEffect.CallEnded)
        }
    }

    /**
     * Toggles the minimized state of the call interface.
     */
    private fun toggleMinimize() {
        _uiState.update { state ->
            if (state is CallUiState.Active) {
                state.copy(isMinimized = !state.isMinimized)
            } else state
        }
    }

    /**
     * Called when the ViewModel is destroyed. Ensures the call is disconnected 
     * to avoid memory leaks or lingering connections.
     */
    override fun onCleared() {
        callHandler.disconnect()
        super.onCleared()
    }
}