package ru.kubsu.borshchevyk.core.domain.call.usecase

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import ru.kubsu.borshchevyk.core.model.dto.NotificationDto
import javax.inject.Inject

class ObserveCallEventsUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    operator fun invoke(): Flow<NotificationDto.CallEventDto> {
        return callRepository.observeCallEvents()
    }
}