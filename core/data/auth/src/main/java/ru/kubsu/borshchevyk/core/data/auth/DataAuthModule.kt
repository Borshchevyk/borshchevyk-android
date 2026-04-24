package ru.kubsu.borshchevyk.core.data.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.domain.auth.AuthRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataAuthModule {
    @Binds
    @Singleton
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    fun bindTokenProvider(impl: AuthPreferences): ru.kubsu.borshchevyk.core.network.client.TokenProvider
}
