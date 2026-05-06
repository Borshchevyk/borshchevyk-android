package ru.kubsu.borshchevyk.core.network.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class MeshPeer(
    val endpointId: String,
    val name: String
)

@Singleton
class MeshConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val payloadRouter: MeshPayloadRouter
) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    
    private val _connectedEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpoints: StateFlow<Set<String>> = _connectedEndpoints.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<MeshPeer>>(emptyList())
    val connectedPeers: StateFlow<List<MeshPeer>> = _connectedPeers.asStateFlow()
    
    private val endpointNames = ConcurrentHashMap<String, String>()
    
    private var currentLocalEndpointName: String = "Unknown User"

    private val strategy = Strategy.P2P_CLUSTER
    private val serviceId = "ru.kubsu.borshchevyk.mesh"
    private val TAG = "MeshConnectionManager"

    private fun updatePeers() {
        val peers = _connectedEndpoints.value.map { id ->
            MeshPeer(id, endpointNames[id] ?: "Unknown")
        }
        _connectedPeers.value = peers
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with endpoint: $endpointId (${connectionInfo.endpointName})")
            endpointNames[endpointId] = connectionInfo.endpointName
            // Automatically accept connections for the cluster
            connectionsClient.acceptConnection(endpointId, payloadRouter.payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Successfully connected to endpoint: $endpointId")
                    _connectedEndpoints.value = _connectedEndpoints.value + endpointId
                    updatePeers()
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(TAG, "Connection rejected by endpoint: $endpointId")
                    endpointNames.remove(endpointId)
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Error connecting to endpoint: $endpointId")
                    endpointNames.remove(endpointId)
                }
                else -> {
                    Log.w(TAG, "Unknown connection status ${result.status.statusCode} for endpoint: $endpointId")
                    endpointNames.remove(endpointId)
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from endpoint: $endpointId")
            _connectedEndpoints.value = _connectedEndpoints.value - endpointId
            endpointNames.remove(endpointId)
            updatePeers()
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: $endpointId (${info.endpointName})")
            // Request connection when a new endpoint is found
            connectionsClient.requestConnection(
                currentLocalEndpointName, // local endpoint name
                endpointId,
                connectionLifecycleCallback
            ).addOnFailureListener { e ->
                Log.e(TAG, "Failed to request connection to $endpointId", e)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
        }
    }

    fun startAdvertising(localEndpointName: String) {
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(
            localEndpointName,
            serviceId,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "Started advertising as $localEndpointName")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start advertising", e)
        }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        Log.d(TAG, "Stopped advertising")
    }

    fun startDiscovery(localEndpointName: String) {
        currentLocalEndpointName = localEndpointName
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "Started discovery")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start discovery", e)
        }
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        Log.d(TAG, "Stopped discovery")
    }
    
    fun disconnect(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        _connectedEndpoints.value = _connectedEndpoints.value - endpointId
        endpointNames.remove(endpointId)
        updatePeers()
        Log.d(TAG, "Disconnected manually from endpoint: $endpointId")
    }
    
    fun stopAllEndpoints() {
        connectionsClient.stopAllEndpoints()
        _connectedEndpoints.value = emptySet()
        endpointNames.clear()
        updatePeers()
        Log.d(TAG, "Stopped all endpoints")
    }
}
