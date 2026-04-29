package ru.kubsu.borshchevyk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.database.entity.AttachmentEntity
import ru.kubsu.borshchevyk.core.database.entity.MessageEntity
import ru.kubsu.borshchevyk.core.database.entity.MessageWithDetails
import ru.kubsu.borshchevyk.core.database.entity.ReactionEntity
import ru.kubsu.borshchevyk.core.database.entity.UserEntity

@Dao
interface MessageDao {
    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC")
    fun observeChatMessages(chatId: String): Flow<List<MessageWithDetails>>

    @Transaction
    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessage(messageId: String): MessageWithDetails?

    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isPinned = 1 ORDER BY createdAt DESC")
    fun observePinnedMessages(chatId: String): Flow<List<MessageWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessageEntity(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessageEntities(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAttachments(attachments: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReactions(reactions: List<ReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUsers(users: List<UserEntity>)

    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    fun deleteAttachmentsForMessage(messageId: String)

    @Query("DELETE FROM reactions WHERE messageId = :messageId")
    fun deleteReactionsForMessage(messageId: String)

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

    @Query("DELETE FROM messages WHERE id = :messageId")
    fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    fun deleteMessagesByChat(chatId: String)

    @Query("DELETE FROM messages")
    fun deleteAll()
}
