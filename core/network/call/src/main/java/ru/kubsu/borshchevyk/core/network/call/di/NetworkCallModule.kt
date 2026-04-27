package ru.kubsu.borshchevyk.core.network.call.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.network.call.CallNetworkDataSource
import ru.kubsu.borshchevyk.core.network.call.KtorCallNetworkDataSource

@Module
@InstallIn(SingletonComponent::class)
interface NetworkCallModule {

    @Binds
    fun bindCallNetworkDataSource(
        ktorCallNetworkDataSource: KtorCallNetworkDataSource
    ): CallNetworkDataSource
}