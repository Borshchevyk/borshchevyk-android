package ru.kubsu.borshchevyk.feature.chat.settings.interactor

import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import ru.kubsu.borshchevyk.core.domain.auth.GetUserIdUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetChatMembersUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetUserProfileUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatMember
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.displayName
import ru.kubsu.borshchevyk.core.model.domain.displayTag
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
    val chatAvatarUrl: String?,
    val isSavedMessages: Boolean
)

class ChatSettingsDataLoader @Inject constructor(
    private val getChatMembersUseCase: GetChatMembersUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val contactHandler: ContactHandler
) {
    suspend fun loadData(chatId: String): ChatSettingsData {
        val userId = getUserIdUseCase().firstOrNull() ?: ""
        val chat = getUserChatsUseCase().find { it.id == chatId }
        val membersPage = getChatMembersUseCase(chatId, 0, 100)
        
        val isPrivate = chat?.type == ChatType.PRIVATE
        val partnerId = chat?.partnerId
        
        Log.d("ChatSettingsDataLoader", "Loading settings for chat: $chatId, isPrivate: $isPrivate, partnerId: $partnerId")

        var partnerTag: String? = null
        var partnerFirstName = chat?.partnerName?.substringBefore(" ")
        var partnerLastName = chat?.partnerName?.substringAfter(" ", missingDelimiterValue = "")
        var chatAvatarUrl = if (isPrivate) chat?.partnerAvatarUrl else null
        var chatName = if (isPrivate) chat?.partnerName ?: "Unknown User" else chat?.title ?: ""

        if (isPrivate && partnerId != null) {
            try {
                // Use the UseCase (Domain Layer) with clean mapping
                val partnerProfile = getUserProfileUseCase(partnerId)
                partnerTag = partnerProfile.displayTag
                partnerFirstName = partnerProfile.firstName ?: partnerFirstName
                partnerLastName = partnerProfile.lastName ?: partnerLastName
                chatAvatarUrl = partnerProfile.avatarUrl ?: chatAvatarUrl
                chatName = partnerProfile.displayName
                Log.d("ChatSettingsDataLoader", "Resolved partner profile via Domain Layer: $partnerTag")
            } catch (e: Exception) {
                Log.w("ChatSettingsDataLoader", "Failed to fetch partner profile for $partnerId", e)
                // Fallback to searching in members if profile fetch fails
                val partnerMember = membersPage.content.find { it.userId == partnerId }
                partnerTag = partnerMember?.user?.tag?.let { if (it.startsWith("@")) it else "@$it" }
            }
        }

        val isContact = if (isPrivate && partnerId != null) {
            contactHandler.isContact(partnerId)
        } else false

        return ChatSettingsData(
            currentUserId = userId,
            isGroupChat = chat?.type == ChatType.GROUP,
            members = membersPage.content,
            isContact = isContact,
            partnerId = partnerId,
            partnerTag = partnerTag,
            partnerFirstName = partnerFirstName,
            partnerLastName = partnerLastName,
            isDeletable = chat?.isDeletable ?: true,
            chatName = chatName,
            chatDescription = chat?.description,
            chatAvatarUrl = chatAvatarUrl,
            isSavedMessages = chat?.type == ChatType.SAVED_MESSAGES
        )
    }
}
