package ru.kubsu.borshchevyk.core.data.user

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.domain.user.ContactRepository
import ru.kubsu.borshchevyk.core.domain.user.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataUserModule {
    @Binds
    @Singleton
    fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository
}
