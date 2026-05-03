package ru.kubsu.borshchevyk.core.security

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing security-related dependencies.
 *
 * This module is responsible for binding the concrete implementations of security
 * interfaces to their respective abstractions, making them available for dependency
 * injection throughout the application.
 */
@Module
@InstallIn(SingletonComponent::class)
interface SecurityModule {

    /**
     * Binds the [KeyManagerImpl] implementation to the [KeyManager] interface.
     *
     * @param impl The standard implementation using Android Keystore.
     * @return The bound [KeyManager] instance.
     */
    @Binds
    @Singleton
    fun bindKeyManager(impl: KeyManagerImpl): KeyManager
}
