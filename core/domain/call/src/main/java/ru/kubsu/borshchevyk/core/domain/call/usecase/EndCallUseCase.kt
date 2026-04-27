package ru.kubsu.borshchevyk.core.domain.call.usecase

import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import javax.inject.Inject

class EndCallUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    suspend operator fun invoke(callId: String) {
        callRepository.endCall(callId)
    }
}