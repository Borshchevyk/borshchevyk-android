package ru.kubsu.borshchevyk.core.domain.call.usecase

import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import javax.inject.Inject

/**
 * Use case responsible for initiating a new audio/video call.
 * 
 * Encapsulates the business logic required to create a call session
 * and notify the specified participants.
 *
 * @property callRepository The repository handling the underlying call creation operations.
 */
class CreateCallUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    /**
     * Executes the use case to create a new call.
     *
     * @param participantIds A list of unique user identifiers defining who should be invited to the call.
     * @return The unique identifier (`callId`) of the successfully created call session.
     */
    suspend operator fun invoke(participantIds: List<String>): String {
        return callRepository.createCall(participantIds)
    }
}