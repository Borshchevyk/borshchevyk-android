package ru.kubsu.borshchevyk.core.data.call.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.data.call.CallRepositoryImpl
import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import javax.inject.Singleton

/**
 * Hilt module for providing data-layer call dependencies.
 *
 * Binds implementation classes to their respective domain interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
interface DataCallModule {

    /**
     * Binds the [CallRepositoryImpl] implementation to the [CallRepository] interface.
     *
     * @param callRepositoryImpl The concrete implementation of the call repository.
     * @return The bound [CallRepository] instance.
     */
    @Binds
    @Singleton
    fun bindCallRepository(
        callRepositoryImpl: CallRepositoryImpl
    ): CallRepository
}