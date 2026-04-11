package ru.kubsu.borshchevyk.core.domain.user

import javax.inject.Inject

class RemoveContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(contactUserId: String) {
        contactRepository.deleteContact(contactUserId)
    }
}
