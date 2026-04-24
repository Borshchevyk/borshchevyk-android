package ru.kubsu.borshchevyk.core.network.message

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NetworkMessageModule {
    @Binds
    @Singleton
    fun bindMessageNetworkDataSource(impl: KtorMessageNetworkDataSource): MessageNetworkDataSource
}
