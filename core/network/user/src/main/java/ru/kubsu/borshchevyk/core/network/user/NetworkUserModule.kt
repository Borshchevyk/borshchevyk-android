package ru.kubsu.borshchevyk.core.network.user

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NetworkUserModule {
    @Binds
    @Singleton
    fun bindUserNetworkDataSource(impl: KtorUserNetworkDataSource): UserNetworkDataSource

    @Binds
    @Singleton
    fun bindContactNetworkDataSource(impl: KtorContactNetworkDataSource): ContactNetworkDataSource
}
