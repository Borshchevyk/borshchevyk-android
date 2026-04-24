package ru.kubsu.borshchevyk.core.network.websocket

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NetworkWebSocketModule {
    @Binds
    @Singleton
    fun bindWebSocketDataSource(impl: KrossbowWebSocketDataSource): WebSocketDataSource
}
