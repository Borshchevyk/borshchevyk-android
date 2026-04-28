package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.DomainUpdatePermissionsParam
import javax.inject.Inject

class UpdateMemberPermissionsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        chatId: String,
        targetUserId: String,
        canSendMessages: Boolean? = null,
        canDeleteMessages: Boolean? = null,
        canInviteUsers: Boolean? = null,
        canChangeInfo: Boolean? = null
    ) {
        chatRepository.updatePermissions(
            chatId,
            targetUserId,
            DomainUpdatePermissionsParam(
                userId = targetUserId,
                canSendMessages = canSendMessages,
                canDeleteMessages = canDeleteMessages,
                canInviteUsers = canInviteUsers,
                canChangeInfo = canChangeInfo
            )
        )
    }}
