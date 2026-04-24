package ru.kubsu.borshchevyk.core.network.chat

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NetworkChatModule {
    @Binds
    @Singleton
    fun bindChatNetworkDataSource(impl: KtorChatNetworkDataSource): ChatNetworkDataSource
}
