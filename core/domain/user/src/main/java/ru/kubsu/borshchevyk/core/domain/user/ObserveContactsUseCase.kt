package ru.kubsu.borshchevyk.core.domain.user

import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.model.domain.Contact
import javax.inject.Inject

/**
 * Use case for observing the user's contact list reactively.
 */
class ObserveContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    /**
     * Executes the use case to observe contacts.
     *
     * @return A [Flow] of [Contact] lists.
     */
    operator fun invoke(): Flow<List<Contact>> {
        return contactRepository.observeContacts()
    }
}
