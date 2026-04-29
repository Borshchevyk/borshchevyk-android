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
    val callId: String = checkNotNull(savedStateHandle["callId"])
    val isInitiator: Boolean = savedStateHandle["isInitiator"] ?: false

    private val _uiState = MutableStateFlow<CallUiState>(CallUiState.Loading)
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val _effect = Channel<CallEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var connectJob: Job? = null

    init {
        observeCallEvents()
        observeSessionState()
    }

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
                                    isRemoteMicMuted = sessionState.isRemoteMicMuted
                                )
                            } else {
                                CallUiState.Active(
                                    room = sessionState.room,
                                    localVideoTrack = sessionState.localVideoTrack,
                                    remoteVideoTrack = sessionState.remoteVideoTrack,
                                    remoteParticipantName = sessionState.remoteParticipantName,
                                    isMicEnabled = sessionState.isMicEnabled,
                                    isCameraEnabled = sessionState.isCameraEnabled,
                                    isRemoteMicMuted = sessionState.isRemoteMicMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun handleIntent(intent: CallIntent) {
        when (intent) {
            is CallIntent.Connect -> connectToCall()
            is CallIntent.ToggleMic -> toggleMic()
            is CallIntent.ToggleCamera -> toggleCamera()
            is CallIntent.EndCall -> endCall()
            is CallIntent.ToggleMinimize -> toggleMinimize()
        }
    }

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

    private fun toggleMic() {
        val state = _uiState.value as? CallUiState.Active ?: return
        viewModelScope.launch {
            callHandler.toggleMic(!state.isMicEnabled)
        }
    }

    private fun toggleCamera() {
        val state = _uiState.value as? CallUiState.Active ?: return
        viewModelScope.launch {
            callHandler.toggleCamera(!state.isCameraEnabled)
        }
    }

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

    private fun toggleMinimize() {
        _uiState.update { state ->
            if (state is CallUiState.Active) {
                state.copy(isMinimized = !state.isMinimized)
            } else state
        }
    }

    override fun onCleared() {
        callHandler.disconnect()
        super.onCleared()
    }
}