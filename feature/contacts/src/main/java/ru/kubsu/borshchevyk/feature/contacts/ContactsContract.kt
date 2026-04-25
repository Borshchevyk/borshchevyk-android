package ru.kubsu.borshchevyk.feature.contacts

import ru.kubsu.borshchevyk.core.model.domain.Contact

sealed interface ContactsIntent {
    object LoadContacts : ContactsIntent
    data class RemoveContact(val contactUserId: String) : ContactsIntent
    data class ContactClicked(val contactUserId: String) : ContactsIntent
}

sealed interface ContactsEffect {
    data class ShowError(val message: String) : ContactsEffect
    data class NavigateToChat(val chatId: String) : ContactsEffect
}

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)