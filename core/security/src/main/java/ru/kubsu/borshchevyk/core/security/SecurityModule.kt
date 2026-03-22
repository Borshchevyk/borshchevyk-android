package ru.kubsu.borshchevyk.core.security

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface SecurityModule {
    @Binds
    @Singleton
    fun bindKeyManager(impl: KeyManagerImpl): KeyManager
}
