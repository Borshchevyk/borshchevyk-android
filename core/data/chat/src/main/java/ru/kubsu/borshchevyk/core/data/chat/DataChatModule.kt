package ru.kubsu.borshchevyk.core.data.chat

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.domain.chat.ChatRepository
import javax.inject.Singleton

/**
 * Hilt module for providing data-layer chat dependencies.
 *
 * Binds implementation classes to their respective domain interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
interface DataChatModule {
    /**
     * Binds the [ChatRepositoryImpl] implementation to the [ChatRepository] interface.
     *
     * @param impl The concrete implementation of the chat repository.
     * @return The bound [ChatRepository] instance.
     */
    @Binds
    @Singleton
    fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
