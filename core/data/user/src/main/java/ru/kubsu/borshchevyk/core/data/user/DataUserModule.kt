package ru.kubsu.borshchevyk.core.data.user

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.domain.user.ContactRepository
import ru.kubsu.borshchevyk.core.domain.user.UserRepository
import javax.inject.Singleton

/**
 * Hilt module for providing data-layer user and contact dependencies.
 *
 * Binds implementation classes to their respective domain interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
interface DataUserModule {
    /**
     * Binds the [UserRepositoryImpl] implementation to the [UserRepository] interface.
     */
    @Binds
    @Singleton
    fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    /**
     * Binds the [ContactRepositoryImpl] implementation to the [ContactRepository] interface.
     */
    @Binds
    @Singleton
    fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository
}
