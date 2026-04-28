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

@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    private val observeCallEventsUseCase: ObserveCallEventsUseCase,
    private val leaveCallUseCase: LeaveCallUseCase
) : ViewModel() {

    private val _incomingCall = MutableStateFlow<DomainCallEvent?>(null)
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

    fun acceptCall(callId: String, onNavigateToCall: (String) -> Unit) {
        _incomingCall.value = null
        onNavigateToCall(callId)
    }

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