package ru.kubsu.borshchevyk.core.network.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NetworkAuthModule {
    @Binds
    @Singleton
    fun bindAuthNetworkDataSource(impl: KtorAuthNetworkDataSource): AuthNetworkDataSource
}
