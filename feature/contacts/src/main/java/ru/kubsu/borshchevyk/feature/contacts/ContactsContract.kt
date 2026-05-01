package ru.kubsu.borshchevyk.feature.contacts

import ru.kubsu.borshchevyk.core.model.domain.Contact

/**
 * Represents the user intents (actions) that can be performed on the contacts screen.
 */
sealed interface ContactsIntent {
    /**
     * Intent to load or reload the list of contacts.
     */
    object LoadContacts : ContactsIntent
    
    /**
     * Intent to remove a specific contact from the user's contact list.
     *
     * @property contactUserId The unique identifier of the user to be removed.
     */
    data class RemoveContact(val contactUserId: String) : ContactsIntent
    
    /**
     * Intent triggered when a contact item is clicked in the UI.
     *
     * @property contactUserId The unique identifier of the clicked user.
     */
    data class ContactClicked(val contactUserId: String) : ContactsIntent
}

/**
 * Represents one-off side effects (events) emitted by the view model,
 * such as navigation or showing temporary error messages.
 */
sealed interface ContactsEffect {
    /**
     * Effect to display an error message to the user.
     *
     * @property message The text of the error to display.
     */
    data class ShowError(val message: String) : ContactsEffect
    
    /**
     * Effect to navigate to a specific private chat.
     *
     * @property chatId The unique identifier of the chat to navigate to.
     */
    data class NavigateToChat(val chatId: String) : ContactsEffect
}

/**
 * Represents the persistent UI state of the contacts screen.
 *
 * @property contacts The current list of contacts to display.
 * @property isLoading Indicates whether a background operation (like loading contacts) is in progress.
 * @property error An optional error message if a state-level error occurred.
 */
data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)