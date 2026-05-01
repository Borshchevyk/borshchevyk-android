package ru.kubsu.borshchevyk.core.domain.call.usecase

import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import javax.inject.Inject

/**
 * Use case responsible for joining an existing audio/video call.
 * 
 * It coordinates with the repository to establish a connection to an ongoing
 * call session based on its identifier.
 *
 * @property callRepository The repository handling the underlying call connection operations.
 */
class JoinCallUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    /**
     * Executes the use case to join a call.
     *
     * @param callId The unique identifier of the call session to join.
     * @return A connection identifier or token confirming successful entry into the call.
     */
    suspend operator fun invoke(callId: String): String {
        return callRepository.joinCall(callId)
    }
}