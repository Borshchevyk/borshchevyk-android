package ru.kubsu.borshchevyk.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.core.database.AppDatabase
import ru.kubsu.borshchevyk.core.database.dao.ChatDao
import ru.kubsu.borshchevyk.core.database.dao.ChatMemberDao
import ru.kubsu.borshchevyk.core.database.dao.ContactDao
import ru.kubsu.borshchevyk.core.database.dao.MessageDao
import ru.kubsu.borshchevyk.core.database.dao.PendingEnvelopeDao
import ru.kubsu.borshchevyk.core.database.dao.PrivacySettingsDao
import ru.kubsu.borshchevyk.core.database.dao.PublicKeyDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import javax.inject.Singleton

/**
 * Dagger Hilt dependency injection module for the Borshchevyk database layer.
 *
 * This module is responsible for instantiating the Room [AppDatabase] as a singleton and
 * exposing its Data Access Objects (DAOs) to the rest of the application (e.g., repository layers).
 * This ensures that only a single instance of the database connection is open at any time,
 * providing a unified local cache for both global server and P2P mesh network operations.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the [AppDatabase] instance as a singleton.
     *
     * @param context The application context.
     * @return A configured [AppDatabase] instance.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "borshchevyk_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    /**
     * Provides the [ChatDao] implementation.
     *
     * @param database The [AppDatabase] instance.
     * @return The [ChatDao] implementation.
     */
    @Provides
    @Singleton
    fun provideChatDao(database: AppDatabase): ChatDao = database.chatDao()

    /**
     * Provides the [ChatMemberDao] implementation.
     *
     * @param database The [AppDatabase] instance.
     * @return The [ChatMemberDao] implementation.
     */
    @Provides
    @Singleton
    fun provideChatMemberDao(database: AppDatabase): ChatMemberDao = database.chatMemberDao()

    /**
     * Provides the [MessageDao] implementation.
     *
     * @param database The [AppDatabase] instance.
     * @return The [MessageDao] implementation.
     */
    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    /**
     * Provides the [UserDao] implementation.
     *
     * @param database The [AppDatabase] instance.
     * @return The [UserDao] implementation.
     */
    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    /**
     * Provides the [PublicKeyDao] implementation.
     *
     * @param database The [AppDatabase] instance.
     * @return The [PublicKeyDao] implementation.
     */
    @Provides
    @Singleton
    fun providePublicKeyDao(database: AppDatabase): PublicKeyDao = database.publicKeyDao()

    /**
     * Provides the [PendingEnvelopeDao] implementation.
     *
     * @param database The [AppDatabase] instance.
     * @return The [PendingEnvelopeDao] implementation.
     */
    @Provides
    @Singleton
    fun providePendingEnvelopeDao(database: AppDatabase): PendingEnvelopeDao = database.pendingEnvelopeDao()

    /**
     * Provides the [ContactDao] implementation.
     *
     * @param database The [AppDatabase] instance.
     * @return The [ContactDao] implementation.
     */
    @Provides
    @Singleton
    fun provideContactDao(database: AppDatabase): ContactDao = database.contactDao()

    /**
     * Provides the [PrivacySettingsDao] implementation.
     *
     * @param database The [AppDatabase] instance.
     * @return The [PrivacySettingsDao] implementation.
     */
    @Provides
    @Singleton
    fun providePrivacySettingsDao(database: AppDatabase): PrivacySettingsDao = database.privacySettingsDao()
}
