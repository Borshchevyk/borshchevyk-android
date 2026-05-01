package ru.kubsu.borshchevyk.core.domain.call.usecase

import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import javax.inject.Inject

/**
 * Use case responsible for terminating an ongoing call for all participants.
 * 
 * This action typically drops the entire call session and cleans up resources.
 * Note that ending a call usually requires appropriate permissions (e.g., being the call initiator).
 *
 * @property callRepository The repository handling the underlying call termination operations.
 */
class EndCallUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    /**
     * Executes the use case to end an active call.
     *
     * @param callId The unique identifier of the call session to be terminated.
     */
    suspend operator fun invoke(callId: String) {
        callRepository.endCall(callId)
    }
}