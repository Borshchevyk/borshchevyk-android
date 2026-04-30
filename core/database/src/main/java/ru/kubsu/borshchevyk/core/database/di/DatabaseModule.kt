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
import ru.kubsu.borshchevyk.core.database.dao.MessageDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing database and DAO dependencies.
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
        ).build()
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
}
