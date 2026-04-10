package ru.kubsu.borshchevyk.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.message.CreatePrivateChatUseCase
import ru.kubsu.borshchevyk.core.domain.user.SearchUsersUseCase
import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUsersUseCase: SearchUsersUseCase,
    private val createPrivateChatUseCase: CreatePrivateChatUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        if (newQuery.length >= 3) {
            search()
        }
    }

    fun onCreateChat(userId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val chat = createPrivateChatUseCase(userId)
                onSuccess(chat.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val users = searchUsersUseCase(_uiState.value.query)
                _uiState.update { it.copy(results = users, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
