package ru.kubsu.borshchevyk.core.network.media

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NetworkMediaModule {
    @Binds
    @Singleton
    fun bindMediaNetworkDataSource(impl: KtorMediaNetworkDataSource): MediaNetworkDataSource
}
