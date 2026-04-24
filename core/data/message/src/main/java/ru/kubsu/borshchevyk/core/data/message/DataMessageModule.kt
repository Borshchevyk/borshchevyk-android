package ru.kubsu.borshchevyk.core.data.message

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.domain.message.MediaRepository
import ru.kubsu.borshchevyk.core.domain.message.MessageRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataMessageModule {
    
    @Binds
    @Singleton
    fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
}
