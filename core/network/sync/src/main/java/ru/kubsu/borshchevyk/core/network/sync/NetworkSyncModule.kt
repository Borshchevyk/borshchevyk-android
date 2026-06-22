package ru.kubsu.borshchevyk.core.network.sync

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkSyncModule {

    @Binds
    abstract fun bindSyncNetworkDataSource(
        ktorSyncNetworkDataSource: KtorSyncNetworkDataSource
    ): SyncNetworkDataSource
}
