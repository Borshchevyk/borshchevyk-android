package ru.kubsu.borshchevyk.feature.chat.settings.interactor

import kotlinx.coroutines.flow.firstOrNull
import ru.kubsu.borshchevyk.core.domain.auth.GetUserIdUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetChatMembersUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import javax.inject.Inject

data class ChatSettingsData(
    val currentUserId: String,
    val isGroupChat: Boolean,
    val members: List<ChatMember>,
    val isContact: Boolean,
    val partnerId: String?,
    val partnerTag: String?,
    val partnerFirstName: String?,
    val partnerLastName: String?,
    val isDeletable: Boolean,
    val chatName: String,
    val chatDescription: String?,
    val chatAvatarUrl: String?
)

class ChatSettingsDataLoader @Inject constructor(
    private val getChatMembersUseCase: GetChatMembersUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val contactHandler: ContactHandler
) {
    suspend fun loadData(chatId: String): ChatSettingsData {
        val userId = getUserIdUseCase().firstOrNull() ?: ""
        val chat = getUserChatsUseCase().find { it.id == chatId }
        val membersPage = getChatMembersUseCase(chatId, 0, 100)
        
        val isPrivate = chat?.type == ChatType.PRIVATE
        val isContact = if (isPrivate && chat?.partnerId != null) {
            contactHandler.isContact(chat.partnerId!!)
        } else false

        val chatName = if (isPrivate) chat?.partnerName ?: "" else chat?.title ?: ""
        val chatAvatarUrl = if (isPrivate) chat?.partnerAvatarUrl else null
        
        val partnerTag = if (isPrivate) {
            val partnerMember = membersPage.content.find { it.userId == chat?.partnerId }
            partnerMember?.user?.tag
        } else null

        return ChatSettingsData(
            currentUserId = userId,
            isGroupChat = chat?.type == ChatType.GROUP,
            members = membersPage.content,
            isContact = isContact,
            partnerId = chat?.partnerId,
            partnerTag = partnerTag,
            partnerFirstName = chat?.partnerName?.substringBefore(" "),
            partnerLastName = chat?.partnerName?.substringAfter(" ", missingDelimiterValue = ""),
            isDeletable = chat?.isDeletable ?: true,
            chatName = chatName,
            chatDescription = chat?.description,
            chatAvatarUrl = chatAvatarUrl
        )
    }
}
