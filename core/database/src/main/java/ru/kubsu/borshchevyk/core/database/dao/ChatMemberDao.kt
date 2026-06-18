package ru.kubsu.borshchevyk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.database.entity.ChatMemberEntity
import ru.kubsu.borshchevyk.core.database.entity.ChatMemberWithUser
import kotlin.math.max

@Dao
interface ChatMemberDao {

    @androidx.room.Transaction
    @Query("SELECT * FROM chat_members WHERE chatId = :chatId AND status = 'ACTIVE'")
    fun getActiveMembers(chatId: String): Flow<List<ChatMemberWithUser>>

    @Query("SELECT * FROM chat_members WHERE chatId = :chatId AND status = 'ACTIVE'")
    fun getActiveMembersSync(chatId: String): List<ChatMemberEntity>

    @Query("SELECT * FROM chat_members WHERE chatId = :chatId AND userId = :userId")
    fun getMember(chatId: String, userId: String): ChatMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(member: ChatMemberEntity)

    @Query("DELETE FROM chat_members WHERE chatId = :chatId AND userId = :userId")
    fun deleteMember(chatId: String, userId: String)

    @Transaction
    fun upsertMemberWithLWW(incoming: ChatMemberEntity) {
        val existing = getMember(incoming.chatId, incoming.userId)
        if (existing == null) {
            insertOrReplace(incoming)
        } else {
            val updatedRole = if (incoming.roleUpdatedAt > existing.roleUpdatedAt) incoming.role else existing.role
            val updatedRoleTs = max(incoming.roleUpdatedAt, existing.roleUpdatedAt)
            
            val updatedStatus = if (incoming.statusUpdatedAt > existing.statusUpdatedAt) incoming.status else existing.status
            val updatedStatusTs = max(incoming.statusUpdatedAt, existing.statusUpdatedAt)
            
            val updatedPermsTs = max(incoming.permissionsUpdatedAt, existing.permissionsUpdatedAt)
            val useIncomingPerms = incoming.permissionsUpdatedAt > existing.permissionsUpdatedAt
            
            val merged = existing.copy(
                role = updatedRole,
                roleUpdatedAt = updatedRoleTs,
                status = updatedStatus,
                statusUpdatedAt = updatedStatusTs,
                canSendMessages = if (useIncomingPerms) incoming.canSendMessages else existing.canSendMessages,
                canDeleteMessages = if (useIncomingPerms) incoming.canDeleteMessages else existing.canDeleteMessages,
                canInviteUsers = if (useIncomingPerms) incoming.canInviteUsers else existing.canInviteUsers,
                canChangeInfo = if (useIncomingPerms) incoming.canChangeInfo else existing.canChangeInfo,
                permissionsUpdatedAt = updatedPermsTs
            )
            insertOrReplace(merged)
        }
    }
}