package ru.kubsu.borshchevyk.core.domain.call

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.DomainCallEvent

interface CallRepository {
    suspend fun createCall(participantsIds: List<String>): String
    suspend fun joinCall(callId: String): String
    suspend fun leaveCall(callId: String)
    suspend fun endCall(callId: String)
    suspend fun getIceServers(): List<Any>
    fun observeCallEvents(): Flow<DomainCallEvent>
}