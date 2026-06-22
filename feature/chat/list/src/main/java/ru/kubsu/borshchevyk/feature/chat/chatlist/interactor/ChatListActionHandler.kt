package ru.kubsu.borshchevyk.feature.chat.chatlist.interactor

import ru.kubsu.borshchevyk.core.domain.chat.CreateGroupChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.CreatePrivateChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GlobalSearchUseCase
import ru.kubsu.borshchevyk.core.domain.chat.JoinChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.PinChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.SyncUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.chat.UnpinChatUseCase
import javax.inject.Inject

class ChatListActionHandler @Inject constructor(
    private val syncUserChatsUseCase: SyncUserChatsUseCase,
    private val createPrivateChatUseCase: CreatePrivateChatUseCase,
    private val createGroupChatUseCase: CreateGroupChatUseCase,
    private val joinChatUseCase: JoinChatUseCase,
    private val pinChatUseCase: PinChatUseCase,
    private val unpinChatUseCase: UnpinChatUseCase,
    private val globalSearchUseCase: GlobalSearchUseCase
) {
    suspend fun loadChats() {
        syncUserChatsUseCase()
    }

    suspend fun pinChat(chatId: String) {
        pinChatUseCase(chatId)
    }

    suspend fun unpinChat(chatId: String) {
        unpinChatUseCase(chatId)
    }

    suspend fun createPrivateChat(targetUserId: String): String {
        return createPrivateChatUseCase(targetUserId)
    }

    suspend fun createGroupChat(title: String, description: String?): String {
        return createGroupChatUseCase(title, description)
    }

    suspend fun joinChat(inviteCode: String): String {
        return joinChatUseCase(inviteCode).id
    }

    suspend fun createPrivateChatByTag(tag: String): String {
        val results = globalSearchUseCase(tag)
        val targetUser = results.users.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
        if (targetUser != null) {
            return createPrivateChatUseCase(targetUser.userId)
        } else {
            throw IllegalArgumentException("User $tag not found in local database")
        }
    }
}
