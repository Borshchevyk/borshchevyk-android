package ru.kubsu.borshchevyk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.database.entity.ChatEntity

/**
 * Data Access Object (DAO) for handling [ChatEntity] operations in the Borshchevyk messenger.
 *
 * Manages the persistence of chat sessions (both direct and group chats). This includes
 * chats originating from the global server as well as those established over P2P mesh networks.
 */
@Dao
interface ChatDao {
    /**
     * Observes a list of all chats, ordered by creation time descending.
     *
     * @return A [Flow] emitting the list of [ChatEntity]s.
     */
    @Query("SELECT * FROM chats ORDER BY createdAt DESC")
    fun observeAllChats(): Flow<List<ChatEntity>>

    /**
     * Observes a specific chat by its ID.
     *
     * @param chatId The unique ID of the chat.
     * @return A [Flow] emitting the [ChatEntity] or null if not found.
     */
    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun observeChat(chatId: String): Flow<ChatEntity?>

    /**
     * Retrieves a specific chat by its ID synchronously.
     *
     * @param chatId The unique ID of the chat.
     * @return The [ChatEntity] if found, otherwise null.
     */
    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChat(chatId: String): ChatEntity?

    /**
     * Retrieves a specific chat by partner ID synchronously.
     * Useful for P2P mesh mode to find existing direct chats.
     *
     * @param partnerId The unique ID of the partner.
     * @return The [ChatEntity] if found, otherwise null.
     */
    @Query("SELECT * FROM chats WHERE partnerId = :partnerId LIMIT 1")
    fun getChatByPartnerId(partnerId: String): ChatEntity?

    /**
     * Inserts or updates a list of chats.
     *
     * @param chats The list of [ChatEntity] to upsert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertChats(chats: List<ChatEntity>)

    /**
     * Inserts or updates a single chat.
     *
     * @param chat The [ChatEntity] to upsert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertChat(chat: ChatEntity)

    /**
     * Deletes a chat by its ID.
     *
     * @param chatId The unique ID of the chat to delete.
     */
    @Query("DELETE FROM chats WHERE id = :chatId")
    fun deleteChat(chatId: String)

    /**
     * Deletes all chats that are not present in the provided list of IDs.
     *
     * @param chatIds The list of chat IDs to keep.
     */
    @Query("DELETE FROM chats WHERE id NOT IN (:chatIds)")
    fun deleteChatsNotIn(chatIds: List<String>)

    /**
     * Clears all chats from the database.
     */
    @Query("DELETE FROM chats")
    fun deleteAll()
    
    @androidx.room.Transaction
    fun upsertChatWithLWW(incoming: ChatEntity) {
        val existing = getChat(incoming.id)
        if (existing == null) {
            upsertChat(incoming)
        } else {
            val updatedTitle = if (incoming.titleUpdatedAt > existing.titleUpdatedAt) incoming.title else existing.title
            val updatedTitleTs = kotlin.math.max(incoming.titleUpdatedAt, existing.titleUpdatedAt)
            
            val updatedDesc = if (incoming.descriptionUpdatedAt > existing.descriptionUpdatedAt) incoming.description else existing.description
            val updatedDescTs = kotlin.math.max(incoming.descriptionUpdatedAt, existing.descriptionUpdatedAt)
            
            val merged = existing.copy(
                title = updatedTitle,
                titleUpdatedAt = updatedTitleTs,
                description = updatedDesc,
                descriptionUpdatedAt = updatedDescTs
            )
            upsertChat(merged)
        }
    }
}
