package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.Contact
import ru.kubsu.borshchevyk.core.model.domain.DomainAddContactParam
import javax.inject.Inject

/**
 * Use case for adding a new contact to the user's contact list.
 *
 * This use case encapsulates the business logic for creating a new contact entry.
 * It interacts with the [ContactRepository] to persist the new contact.
 *
 * @property contactRepository The repository used to manage contact data.
 */
class AddContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
     * Executes the use case to add a contact.
     *
     * @param targetUserId The unique identifier of the user to be added as a contact.
     * @param firstName The first name to assign to the contact.
     * @param lastName The optional last name to assign to the contact.
     * @return The newly created [Contact] object.
     */
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
