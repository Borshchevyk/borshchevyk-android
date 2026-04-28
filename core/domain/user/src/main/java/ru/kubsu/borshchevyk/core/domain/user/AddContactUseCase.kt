package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.Contact
import ru.kubsu.borshchevyk.core.model.domain.DomainAddContactParam
import javax.inject.Inject

class AddContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(targetUserId: String, firstName: String, lastName: String? = null): Contact {
        return contactRepository.addContact(
            DomainAddContactParam(
                targetUserId = targetUserId,
                firstName = firstName,
                lastName = lastName
            )
        )
    }
}
