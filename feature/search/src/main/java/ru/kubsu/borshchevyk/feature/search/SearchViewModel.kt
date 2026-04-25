package ru.kubsu.borshchevyk.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.chat.CreatePrivateChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.user.SearchUsersUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class)
class SearchViewModel @Inject constructor(
    private val searchUsersUseCase: SearchUsersUseCase,
    private val createPrivateChatUseCase: CreatePrivateChatUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _effect = Channel<SearchEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private var defaultResults: List<User> = emptyList()

    init {
        loadDefaultResults()
        observeSearchQuery()
    }

    private fun loadDefaultResults() {
        viewModelScope.launch {
            try {
                val chats = getUserChatsUseCase()
                defaultResults = chats
                    .filter { it.type == ChatType.PRIVATE && it.partnerId != null }
                    .map { chat ->
                        User(
                            userId = chat.partnerId!!,
                            tag = chat.partnerName ?: "",
                            firstName = chat.partnerName,
                            lastName = null,
                            avatarUrl = chat.partnerAvatarUrl
                        )
                    }
                if (_uiState.value.query.length < 3) {
                    _uiState.update { it.copy(results = defaultResults) }
                }
            } catch (e: Exception) { }
        }
    }

    fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateQuery -> onQueryChange(intent.query)
            is SearchIntent.CreateChat -> onCreateChat(intent.userId)
        }
    }

    private fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchQueryFlow.value = newQuery
    }

    private fun observeSearchQuery() {
        searchQueryFlow
            .debounce(500L)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 3) {
                    search(query)
                } else {
                    _uiState.update { it.copy(results = defaultResults, isLoading = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onCreateChat(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val chat = createPrivateChatUseCase(userId)
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(SearchEffect.NavigateToChat(chat.id))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(SearchEffect.ShowError(e.message ?: "Failed to create chat"))
            }
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val users = searchUsersUseCase(query)
                _uiState.update { it.copy(results = users, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(SearchEffect.ShowError(e.message ?: "Search failed"))
            }
        }
    }
}
