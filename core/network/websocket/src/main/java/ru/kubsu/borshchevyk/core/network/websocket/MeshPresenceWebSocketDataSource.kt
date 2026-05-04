package ru.kubsu.borshchevyk.core.network.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.kubsu.borshchevyk.core.network.dto.PresenceStatusResponse
import ru.kubsu.borshchevyk.core.network.mesh.MeshConnectionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshPresenceWebSocketDataSource @Inject constructor(
    private val meshConnectionManager: MeshConnectionManager
) : PresenceWebSocketDataSource {

    override fun observePresence(userId: String): Flow<PresenceStatusResponse> {
        return meshConnectionManager.connectedEndpoints.map { connectedSet ->
            PresenceStatusResponse(
                userId = userId,
                isOnline = connectedSet.contains(userId),
                lastSeenAt = null
            )
        }
    }

    override suspend fun connect() {
        // Handled by MeshConnectionManager
    }

    override suspend fun disconnect() {
        // Handled by MeshConnectionManager
    }
}
