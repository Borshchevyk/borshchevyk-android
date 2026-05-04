package ru.kubsu.borshchevyk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.database.entity.AttachmentEntity
import ru.kubsu.borshchevyk.core.database.entity.MessageEntity
import ru.kubsu.borshchevyk.core.database.entity.MessageReaderEntity
import ru.kubsu.borshchevyk.core.database.entity.MessageWithDetails
import ru.kubsu.borshchevyk.core.database.entity.ReactionEntity
import ru.kubsu.borshchevyk.core.database.entity.UserEntity

/**
 * Data Access Object (DAO) for [MessageEntity] and its associated data in the Borshchevyk messenger.
 *
 * This DAO provides transactional support for complex upsert operations involving relations like
 * attachments, reactions, and authors. It handles both direct device-to-device (P2P/Mesh)
 * and global server synchronization scenarios, ensuring local database consistency when merging
 * incoming messages from different sources.
 */
@Dao
interface MessageDao {
    /**
     * Observes all messages in a specific chat with their full details.
     *
     * @param chatId The ID of the chat.
     * @return A [Flow] emitting the list of [MessageWithDetails].
     */
    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC")
    fun observeChatMessages(chatId: String): Flow<List<MessageWithDetails>>

    /**
     * Retrieves a single message by ID with its details.
     *
     * @param messageId The unique ID of the message.
     * @return The [MessageWithDetails] or null if not found.
     */
    @Transaction
    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessage(messageId: String): MessageWithDetails?

    /**
     * Observes all pinned messages in a specific chat.
     *
     * @param chatId The ID of the chat.
     * @return A [Flow] emitting the list of [MessageWithDetails].
     */
    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isPinned = 1 ORDER BY createdAt DESC")
    fun observePinnedMessages(chatId: String): Flow<List<MessageWithDetails>>

    /**
     * Inserts a single message entity.
     *
     * @param message The [MessageEntity] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessageEntity(message: MessageEntity)

    /**
     * Inserts a list of message entities.
     *
     * @param messages The list of [MessageEntity] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessageEntities(messages: List<MessageEntity>)

    /**
     * Inserts attachments for messages.
     *
     * @param attachments The list of [AttachmentEntity] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAttachments(attachments: List<AttachmentEntity>)

    /**
     * Inserts reactions for messages.
     *
     * @param reactions The list of [ReactionEntity] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReactions(reactions: List<ReactionEntity>)

    /**
     * Inserts or updates user entities.
     *
     * @param users The list of [UserEntity] to upsert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUsers(users: List<UserEntity>)

    /**
     * Inserts a user but ignores if it already exists.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertUserIgnore(user: UserEntity)

    /**
     * Inserts a message reader mapping.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMessageReader(reader: MessageReaderEntity)

    /**
     * Retrieves the list of users who have read a specific message.
     */
    @Query("SELECT users.* FROM users INNER JOIN message_readers ON users.userId = message_readers.userId WHERE message_readers.messageId = :messageId")
    fun getMessageReaders(messageId: String): List<UserEntity>

    /**
     * Deletes all attachments for a specific message.
     *
     * @param messageId The ID of the message.
     */
    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    fun deleteAttachmentsForMessage(messageId: String)

    /**
     * Deletes a specific reaction by a user for a message.
     */
    @Query("DELETE FROM reactions WHERE messageId = :messageId AND userId = :userId AND reaction = :reaction")
    fun deleteReaction(messageId: String, userId: String, reaction: String)

    /**
     * Deletes all reactions for a specific message.
     *
     * @param messageId The ID of the message.
     */
    @Query("DELETE FROM reactions WHERE messageId = :messageId")
    fun deleteReactionsForMessage(messageId: String)

    /**
     * Atomically updates a single message and all its related data.
     *
     * @param message The [MessageEntity] to update.
     * @param author The optional [UserEntity] author.
     * @param forwardedFromUser The optional [UserEntity] who forwarded the message.
     * @param attachments The list of [AttachmentEntity] associated.
     * @param reactions The list of [ReactionEntity] associated.
     */
    @Transaction
    fun upsertMessageWithDetails(
        message: MessageEntity,
        author: UserEntity?,
        forwardedFromUser: UserEntity?,
        attachments: List<AttachmentEntity>,
        reactions: List<ReactionEntity>
    ) {
        val usersToInsert = listOfNotNull(author, forwardedFromUser)
        if (usersToInsert.isNotEmpty()) {
            insertUsers(usersToInsert)
        }
        
        insertMessageEntity(message)
        
        // Clear old relations to handle removed items cleanly during an update
        deleteAttachmentsForMessage(message.id)
        deleteReactionsForMessage(message.id)
        
        if (attachments.isNotEmpty()) {
            insertAttachments(attachments)
        }
        if (reactions.isNotEmpty()) {
            insertReactions(reactions)
        }
    }

    /**
     * Deletes a message by its ID.
     *
     * @param messageId The ID of the message to delete.
     */
    @Query("DELETE FROM messages WHERE id = :messageId")
    fun deleteMessage(messageId: String)

    /**
     * Deletes all messages in a specific chat.
     *
     * @param chatId The ID of the chat.
     */
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    fun deleteMessagesByChat(chatId: String)

    /**
     * Clears all messages from the database.
     */
    @Query("DELETE FROM messages")
    fun deleteAll()

    /**
     * Deletes attachments for a batch of messages.
     *
     * @param messageIds The list of message IDs.
     */
    @Query("DELETE FROM attachments WHERE messageId IN (:messageIds)")
    fun deleteAttachmentsForMessages(messageIds: List<String>)

    /**
     * Deletes reactions for a batch of messages.
     *
     * @param messageIds The list of message IDs.
     */
    @Query("DELETE FROM reactions WHERE messageId IN (:messageIds)")
    fun deleteReactionsForMessages(messageIds: List<String>)

    /**
     * Atomically updates a batch of messages and their associated data.
     *
     * @param messages The list of [MessageEntity] to update.
     * @param users The list of [UserEntity] associated.
     * @param attachments The list of [AttachmentEntity] associated.
     * @param reactions The list of [ReactionEntity] associated.
     */
    @Transaction
    fun upsertMessagesWithDetails(
        messages: List<MessageEntity>,
        users: List<UserEntity>,
        attachments: List<AttachmentEntity>,
        reactions: List<ReactionEntity>
    ) {
        if (users.isNotEmpty()) {
            insertUsers(users)
        }
        
        insertMessageEntities(messages)
        
        val messageIds = messages.map { it.id }
        messageIds.chunked(900).forEach { chunk ->
            deleteAttachmentsForMessages(chunk)
            deleteReactionsForMessages(chunk)
        }
        
        if (attachments.isNotEmpty()) {
            insertAttachments(attachments)
        }
        if (reactions.isNotEmpty()) {
            insertReactions(reactions)
        }
    }
}
