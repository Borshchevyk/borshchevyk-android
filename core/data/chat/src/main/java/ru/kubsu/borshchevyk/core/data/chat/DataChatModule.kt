package ru.kubsu.borshchevyk.core.data.chat

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.domain.chat.ChatRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataChatModule {
    @Binds
    @Singleton
    fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
