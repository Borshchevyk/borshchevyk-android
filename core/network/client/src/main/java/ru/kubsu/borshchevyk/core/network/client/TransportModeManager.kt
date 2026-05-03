package ru.kubsu.borshchevyk.core.network.client

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class NetworkMode {
    GLOBAL,
    MESH
}

@Singleton
class TransportModeManager @Inject constructor() {
    private val _networkMode = MutableStateFlow(NetworkMode.GLOBAL)
    val networkMode: StateFlow<NetworkMode> = _networkMode.asStateFlow()

    fun setMode(mode: NetworkMode) {
        _networkMode.value = mode
    }
}
