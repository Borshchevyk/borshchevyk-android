package ru.kubsu.borshchevyk.core.network.call.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.network.call.CallNetworkDataSource
import ru.kubsu.borshchevyk.core.network.call.ProxyCallNetworkDataSource
import ru.kubsu.borshchevyk.core.network.websocket.CallWebSocketDataSource

@Module
@InstallIn(SingletonComponent::class)
interface NetworkCallModule {

    @Binds
    fun bindCallNetworkDataSource(
        impl: ProxyCallNetworkDataSource
    ): CallNetworkDataSource

    @Binds
    fun bindCallWebSocketDataSource(
        impl: ProxyCallNetworkDataSource
    ): CallWebSocketDataSource
}