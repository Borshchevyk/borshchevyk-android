package ru.kubsu.borshchevyk.feature.call.ui.call

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
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
import ru.kubsu.borshchevyk.core.network.client.NetworkConstants
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val application: Application,
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

    private var room: Room? = null

    init {
        viewModelScope.launch {
            observeCallEventsUseCase().collect { event ->
                if (event.callId == callId) {
                    if (event.eventType == "REJECTED") {
                        val message = "Call rejected" + (event.actor?.firstName?.let { " by $it" } ?: "")
                        _uiState.value = CallUiState.Error(message)
                        _effect.send(CallEffect.ShowError(message))
                        _effect.send(CallEffect.CallEnded)
                    } else if (event.eventType == "ENDED") {
                        _effect.send(CallEffect.CallEnded)
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
        viewModelScope.launch {
            try {
                val token = joinCallUseCase(callId)
                room = LiveKit.create(application)
                
                room?.let { r ->
                    r.connect(NetworkConstants.LIVEKIT_URL, token)
                    r.localParticipant.setMicrophoneEnabled(true)
                    r.localParticipant.setCameraEnabled(true)
                    
                    _uiState.value = CallUiState.Active(room = r)
                }
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
            val newState = !state.isMicEnabled
            room?.localParticipant?.setMicrophoneEnabled(newState)
            _uiState.update { (it as CallUiState.Active).copy(isMicEnabled = newState) }
        }
    }

    private fun toggleCamera() {
        val state = _uiState.value as? CallUiState.Active ?: return
        viewModelScope.launch {
            val newState = !state.isCameraEnabled
            room?.localParticipant?.setCameraEnabled(newState)
            _uiState.update { (it as CallUiState.Active).copy(isCameraEnabled = newState) }
        }
    }

    private fun endCall() {
        viewModelScope.launch {
            try {
                room?.disconnect()
                if (isInitiator) {
                    endCallUseCase(callId)
                } else {
                    leaveCallUseCase(callId)
                }
                _effect.send(CallEffect.CallEnded)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to end call", e)
            }
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
        room?.disconnect()
        room?.release()
        super.onCleared()
    }
}