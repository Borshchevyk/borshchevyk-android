package ru.kubsu.borshchevyk.core.database.entity

import androidx.room.Entity

/**
 * Represents a member of a group chat, supporting CRDT-based P2P synchronization
 * via Last-Write-Wins (LWW) timestamps for resolving conflicts.
 */
@Entity(
    tableName = "chat_members",
    primaryKeys = ["chatId", "userId"]
)
data class ChatMemberEntity(
    val chatId: String,
    val userId: String,
    val role: String,
    val joinedAt: String,
    val status: String, // "ACTIVE", "KICKED", "LEFT"
    
    val canSendMessages: Boolean,
    val canDeleteMessages: Boolean,
    val canInviteUsers: Boolean,
    val canChangeInfo: Boolean,
    
    // LWW Timestamps for CRDT merges
    val roleUpdatedAt: Long = 0,
    val statusUpdatedAt: Long = 0,
    val permissionsUpdatedAt: Long = 0
)