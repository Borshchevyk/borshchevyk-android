package ru.kubsu.borshchevyk.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.kubsu.borshchevyk.core.database.converter.DatabaseConverters
import ru.kubsu.borshchevyk.core.database.dao.ChatDao
import ru.kubsu.borshchevyk.core.database.dao.MessageDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.AttachmentEntity
import ru.kubsu.borshchevyk.core.database.entity.ChatEntity
import ru.kubsu.borshchevyk.core.database.entity.MessageEntity
import ru.kubsu.borshchevyk.core.database.entity.ReactionEntity
import ru.kubsu.borshchevyk.core.database.entity.UserEntity

/**
 * Main database definition for the Borshchevyk Android client.
 *
 * This database manages all persistent data using the Room persistence library.
 * It currently includes entities for chats, messages, users, attachments, and reactions.
 *
 * @property chatDao Data Access Object for chat-related operations.
 * @property messageDao Data Access Object for message, attachment, and reaction operations.
 * @property userDao Data Access Object for user-related operations.
 */
@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        UserEntity::class,
        AttachmentEntity::class,
        ReactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
}
