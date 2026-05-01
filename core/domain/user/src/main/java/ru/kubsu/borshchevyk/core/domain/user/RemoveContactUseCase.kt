package ru.kubsu.borshchevyk.core.domain.user

import javax.inject.Inject

/**
 * Use case for removing a contact from the user's contact list.
 *
 * This use case handles the business logic to delete an existing contact entry.
 * It interacts with the [ContactRepository] to perform the deletion.
 *
 * @property contactRepository The repository used to manage contact data.
 */
class RemoveContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
     * Executes the use case to remove a contact.
     *
     * @param contactUserId The unique identifier of the user to be removed from contacts.
     */
    suspend operator fun invoke(contactUserId: String) {
        contactRepository.deleteContact(contactUserId)
    }
}
