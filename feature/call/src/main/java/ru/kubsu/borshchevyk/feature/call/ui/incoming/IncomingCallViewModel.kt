package ru.kubsu.borshchevyk.feature.call.ui.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.call.usecase.LeaveCallUseCase
import ru.kubsu.borshchevyk.core.domain.call.usecase.ObserveCallEventsUseCase
import ru.kubsu.borshchevyk.core.model.domain.DomainCallEvent
import javax.inject.Inject

/**
 * A singleton-like or globally scoped ViewModel that constantly listens for incoming call events.
 * Used primarily by the [IncomingCallBanner] to prompt the user to accept or reject the call.
 *
 * @property observeCallEventsUseCase Observes signaling events via WebSocket to detect INITIATED calls.
 * @property leaveCallUseCase Use case to notify the server that the call was rejected.
 */
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    private val observeCallEventsUseCase: ObserveCallEventsUseCase,
    private val leaveCallUseCase: LeaveCallUseCase
) : ViewModel() {

    private val _incomingCall = MutableStateFlow<DomainCallEvent?>(null)
    /** Emits the [DomainCallEvent] of the pending incoming call, or null if there is none. */
    val incomingCall: StateFlow<DomainCallEvent?> = _incomingCall.asStateFlow()

    init {
        viewModelScope.launch {
            observeCallEventsUseCase().collect { event ->
                if (event.type == "INITIATED") {
                    _incomingCall.value = event
                } else if (event.type == "ENDED" || event.type == "REJECTED" || event.type == "ACCEPTED") {
                    if (_incomingCall.value?.callId == event.callId) {
                        _incomingCall.value = null
                    }
                }
            }
        }
    }

    /**
     * Accepts the pending call and triggers navigation to the active call screen.
     *
     * @param callId The ID of the call being accepted.
     * @param onNavigateToCall Callback passing the callId to the navigation controller.
     */
    fun acceptCall(callId: String, onNavigateToCall: (String) -> Unit) {
        _incomingCall.value = null
        onNavigateToCall(callId)
    }

    /**
     * Rejects the pending call and signals the server.
     *
     * @param callId The ID of the call being rejected.
     */
    fun rejectCall(callId: String) {
        viewModelScope.launch {
            try {
                leaveCallUseCase(callId)
            } catch (e: Exception) {
                // Ignore failure on leave
            } finally {
                if (_incomingCall.value?.callId == callId) {
                    _incomingCall.value = null
                }
            }
        }
    }
}