package ru.kubsu.borshchevyk.core.network.user

/**
 * Interface for broadcasting profile updates to the Mesh network.
 *
 * This interface breaks the circular dependency between the network layer
 * and the profile listener in the data layer.
 */
interface MeshProfileBroadcaster {
    /**
     * Triggers a broadcast of the local user's current profile to the network.
     */
    suspend fun broadcastLocalProfile()
}
