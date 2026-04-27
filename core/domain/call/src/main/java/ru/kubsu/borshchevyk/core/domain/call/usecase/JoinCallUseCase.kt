package ru.kubsu.borshchevyk.core.domain.call.usecase

import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import javax.inject.Inject

class JoinCallUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    suspend operator fun invoke(callId: String): String {
        return callRepository.joinCall(callId)
    }
}