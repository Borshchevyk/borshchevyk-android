package ru.kubsu.borshchevyk.core.data.message

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.domain.message.MediaRepository
import ru.kubsu.borshchevyk.core.domain.message.MessageRepository
import ru.kubsu.borshchevyk.core.network.user.MeshProfileBroadcaster
import javax.inject.Singleton

/**
 * Hilt module for providing data-layer messaging dependencies.
 *
 * Binds implementation classes to their respective domain interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
interface DataMessageModule {
    
    /**
     * Binds the [MessageRepositoryImpl] implementation to the [MessageRepository] interface.
     */
    @Binds
    @Singleton
    fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    /**
     * Binds the [MediaRepositoryImpl] implementation to the [MediaRepository] interface.
     */
    @Binds
    @Singleton
    fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    /**
     * Binds the [MeshProfileListener] implementation to the [MeshProfileBroadcaster] interface.
     */
    @Binds
    @Singleton
    fun bindMeshProfileBroadcaster(impl: MeshProfileListener): MeshProfileBroadcaster
}