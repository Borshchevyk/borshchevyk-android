package ru.kubsu.borshchevyk.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.kubsu.borshchevyk.core.database.converter.DatabaseConverters
import ru.kubsu.borshchevyk.core.database.dao.ChatDao
import ru.kubsu.borshchevyk.core.database.dao.MessageDao
import ru.kubsu.borshchevyk.core.database.dao.PendingEnvelopeDao
import ru.kubsu.borshchevyk.core.database.dao.PublicKeyDao
import ru.kubsu.borshchevyk.core.database.dao.UserDao
import ru.kubsu.borshchevyk.core.database.entity.AttachmentEntity
import ru.kubsu.borshchevyk.core.database.entity.ChatEntity
import ru.kubsu.borshchevyk.core.database.entity.MessageEntity
import ru.kubsu.borshchevyk.core.database.entity.MessageReaderEntity
import ru.kubsu.borshchevyk.core.database.entity.PendingEnvelopeEntity
import ru.kubsu.borshchevyk.core.database.entity.PublicKeyEntity
import ru.kubsu.borshchevyk.core.database.entity.ReactionEntity
import ru.kubsu.borshchevyk.core.database.entity.UserEntity

/**
 * The core relational database definition for the Borshchevyk Android client.
 *
 * Built on top of the Room persistence library, this database manages all local storage
 * requirements for the application. It acts as the single source of truth for UI state,
 * seamlessly blending data synchronized from the regular global server and data exchanged
 * directly with peers via the Mesh network (P2P mode).
 *
 * It manages entities representing chats, messages, users, file attachments, and reactions.
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
        ReactionEntity::class,
        MessageReaderEntity::class,
        PublicKeyEntity::class,
        PendingEnvelopeEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun publicKeyDao(): PublicKeyDao
    abstract fun pendingEnvelopeDao(): PendingEnvelopeDao
}
