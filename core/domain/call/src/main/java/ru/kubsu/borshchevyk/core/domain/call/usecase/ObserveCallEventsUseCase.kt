package ru.kubsu.borshchevyk.core.domain.call.usecase

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import ru.kubsu.borshchevyk.core.model.domain.DomainCallEvent
import javax.inject.Inject

/**
 * Use case responsible for observing real-time events related to audio/video calls.
 * 
 * Provides a continuous stream of events such as incoming calls, participants
 * joining/leaving, and call state changes, allowing the UI and other components
 * to react accordingly.
 *
 * @property callRepository The repository supplying the stream of call events.
 */
class ObserveCallEventsUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    /**
     * Executes the use case to start observing call events.
     *
     * @return A [Flow] emitting [DomainCallEvent] instances representing the ongoing state and actions within a call context.
     */
    operator fun invoke(): Flow<DomainCallEvent> {
        return callRepository.observeCallEvents()
    }
}