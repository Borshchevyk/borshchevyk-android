package ru.kubsu.borshchevyk.core.domain.call.usecase

import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import javax.inject.Inject

/**
 * Use case responsible for leaving an active audio/video call.
 * 
 * This action disconnects the current user from the call session but does not
 * terminate the call for the remaining participants.
 *
 * @property callRepository The repository handling the underlying call disconnection operations.
 */
class LeaveCallUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    /**
     * Executes the use case to leave an active call.
     *
     * @param callId The unique identifier of the call session to leave.
     */
    suspend operator fun invoke(callId: String) {
        callRepository.leaveCall(callId)
    }
}