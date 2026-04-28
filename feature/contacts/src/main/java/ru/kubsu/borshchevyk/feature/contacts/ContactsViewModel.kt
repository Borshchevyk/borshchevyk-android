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

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val removeContactUseCase: RemoveContactUseCase,
    private val createPrivateChatUseCase: CreatePrivateChatUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ContactsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadContacts()
    }

    fun handleIntent(intent: ContactsIntent) {
        when (intent) {
            is ContactsIntent.LoadContacts -> loadContacts()
            is ContactsIntent.RemoveContact -> removeContact(intent.contactUserId)
            is ContactsIntent.ContactClicked -> onContactClicked(intent.contactUserId)
        }
    }

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