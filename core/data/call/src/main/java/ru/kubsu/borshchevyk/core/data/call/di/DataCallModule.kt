package ru.kubsu.borshchevyk.core.data.call.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.data.call.CallRepositoryImpl
import ru.kubsu.borshchevyk.core.domain.call.CallRepository

@Module
@InstallIn(SingletonComponent::class)
interface DataCallModule {

    @Binds
    fun bindCallRepository(
        callRepositoryImpl: CallRepositoryImpl
    ): CallRepository
}