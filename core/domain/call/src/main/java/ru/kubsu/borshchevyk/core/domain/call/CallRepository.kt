package ru.kubsu.borshchevyk.core.domain.call

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.dto.NotificationDto

interface CallRepository {
    suspend fun createCall(participantIds: List<String>): String
    suspend fun joinCall(callId: String): String
    suspend fun leaveCall(callId: String)
    suspend fun endCall(callId: String)
    fun observeCallEvents(): Flow<NotificationDto.CallEventDto>
}