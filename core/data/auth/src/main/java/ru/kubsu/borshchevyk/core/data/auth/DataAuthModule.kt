package ru.kubsu.borshchevyk.core.data.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.domain.auth.AuthRepository
import javax.inject.Singleton

/**
 * Hilt module for providing data-layer authentication dependencies.
 *
 * Binds implementation classes to their respective domain and network interfaces,
 * ensuring they are provided as Singletons within the application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
interface DataAuthModule {
    
    /**
     * Binds the [AuthRepositoryImpl] implementation to the [AuthRepository] interface.
     *
     * @param impl The concrete implementation of the authentication repository.
     * @return The bound [AuthRepository] instance.
     */
    @Binds
    @Singleton
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    /**
     * Binds the [AuthPreferences] implementation to the [ru.kubsu.borshchevyk.core.network.client.TokenProvider] interface.
     *
     * @param impl The concrete implementation managing local token storage.
     * @return The bound [TokenProvider] instance used for network requests.
     */
    @Binds
    @Singleton
    fun bindTokenProvider(impl: AuthPreferences): ru.kubsu.borshchevyk.core.network.client.TokenProvider
}
