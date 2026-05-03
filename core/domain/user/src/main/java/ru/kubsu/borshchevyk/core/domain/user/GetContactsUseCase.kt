package ru.kubsu.borshchevyk.core.domain.user

import ru.kubsu.borshchevyk.core.model.domain.Contact
import javax.inject.Inject

/**
 * Use case for retrieving the user's list of contacts.
 *
 * This use case encapsulates the business logic for fetching contacts. It provides
 * a clean interface for the presentation layer to access contact data without
 * directly coupling to the [ContactRepository].
 *
 * @property contactRepository The repository used to fetch contact data.
 */
class GetContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
     * Executes the use case to fetch the list of contacts.
     *
     * @return A list of [Contact] objects.
     */
    suspend operator fun invoke(): List<Contact> {
        return contactRepository.getContacts()
    }
}
