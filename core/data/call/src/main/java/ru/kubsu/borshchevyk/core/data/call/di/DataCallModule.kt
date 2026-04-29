package ru.kubsu.borshchevyk.core.data.call.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.data.call.CallRepositoryImpl
import ru.kubsu.borshchevyk.core.domain.call.CallRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataCallModule {

    @Binds
    @Singleton
    fun bindCallRepository(
        callRepositoryImpl: CallRepositoryImpl
    ): CallRepository
}