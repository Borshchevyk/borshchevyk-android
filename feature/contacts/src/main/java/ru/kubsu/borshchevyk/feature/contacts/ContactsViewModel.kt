package ru.kubsu.borshchevyk.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.chat.CreatePrivateChatUseCase
import ru.kubsu.borshchevyk.core.domain.user.GetContactsUseCase
import ru.kubsu.borshchevyk.core.domain.user.RemoveContactUseCase
import javax.inject.Inject

/**
 * ViewModel managing the contacts list screen.
 * Handles loading contacts, removing contacts, and starting chats with contacts.
 *
 * @property getContactsUseCase Use case for fetching the current user's contacts.
 * @property removeContactUseCase Use case for removing a user from the contact list.
 * @property createPrivateChatUseCase Use case for creating a new private chat with a user.
 */
@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val removeContactUseCase: RemoveContactUseCase,
    private val createPrivateChatUseCase: CreatePrivateChatUseCase
) : ViewModel() {

    /** Internal mutable state flow for the UI state. */
    private val _uiState = MutableStateFlow(ContactsUiState())
    /** StateFlow emitting the current [ContactsUiState]. */
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    /** Internal channel for side-effects (navigation, error messages, etc.). */
    private val _effect = Channel<ContactsEffect>(Channel.BUFFERED)
    /** Flow of one-time [ContactsEffect]s. */
    val effect = _effect.receiveAsFlow()

    init {
        loadContacts()
    }

    /**
     * Handles incoming intents from the UI.
     *
     * @param intent The intent to handle.
     */
    fun handleIntent(intent: ContactsIntent) {
        when (intent) {
            is ContactsIntent.LoadContacts -> loadContacts()
            is ContactsIntent.RemoveContact -> removeContact(intent.contactUserId)
            is ContactsIntent.ContactClicked -> onContactClicked(intent.contactUserId)
        }
    }

    /**
     * Loads the initial list of contacts from the repository.
     */
    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val contacts = getContactsUseCase()
                _uiState.update { it.copy(contacts = contacts, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load contacts") }
            }
        }
    }

    /**
     * Removes a user from the contact list.
     *
     * @param contactUserId The ID of the contact to remove.
     */
    private fun removeContact(contactUserId: String) {
        viewModelScope.launch {
            try {
                removeContactUseCase(contactUserId)
                val currentContacts = _uiState.value.contacts.filterNot { it.contactUserId == contactUserId }
                _uiState.update { it.copy(contacts = currentContacts) }
            } catch (e: Exception) {
                _effect.send(ContactsEffect.ShowError(e.message ?: "Failed to remove contact"))
            }
        }
    }

    /**
     * Handles a click on a contact by attempting to create or open a private chat.
     *
     * @param userId The ID of the user that was clicked.
     */
    private fun onContactClicked(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val chatId = createPrivateChatUseCase(userId)
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(ContactsEffect.NavigateToChat(chatId))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(ContactsEffect.ShowError(e.message ?: "Failed to start chat"))
            }
        }
    }
}
