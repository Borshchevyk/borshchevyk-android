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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kubsu.borshchevyk.core.domain.chat.CreatePrivateChatUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GetUserChatsUseCase
import ru.kubsu.borshchevyk.core.domain.chat.GlobalSearchUseCase
import ru.kubsu.borshchevyk.core.domain.chat.JoinChatUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

import ru.kubsu.borshchevyk.core.model.domain.GlobalSearchResults

@HiltViewModel
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel @Inject constructor(
    private val globalSearchUseCase: GlobalSearchUseCase,
    private val createPrivateChatUseCase: CreatePrivateChatUseCase,
    private val joinChatUseCase: JoinChatUseCase,
    private val getUserChatsUseCase: GetUserChatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _effect = Channel<SearchEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var defaultUserResults: List<User> = emptyList()

    init {
        loadDefaultResults()
        observeSearchQuery()
    }

    /**
     * Loads default search results.
     */
    private fun loadDefaultResults() {
        viewModelScope.launch {
            try {
                val chats = getUserChatsUseCase()
                defaultUserResults = chats
                    .filter { it.type == ChatType.PRIVATE && it.partnerId != null }
                    .map { it.toSearchUser() }
                
                if (_uiState.value.query.length < MIN_QUERY_LENGTH) {
                    _uiState.update { 
                        it.copy(userResults = defaultUserResults, chatResults = emptyList()) 
                    }
                }
            } catch (e: Exception) {
                // Silently ignore or log - default results are non-critical
            }
        }
    }

    fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateQuery -> onQueryChange(intent.query)
            is SearchIntent.CreateChat -> onCreateChat(intent.userId)
            is SearchIntent.JoinChat -> onJoinChat(intent.chatId)
        }
    }

    /**
     * Updates the current search query.
     *
     * @param newQuery The new search query string.
     */
    private fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, error = null) }
    }

    /**
     * Observes the search query and triggers searches.
     */
    private fun observeSearchQuery() {
        uiState
            .map { it.query }
            .distinctUntilChanged()
            .debounce(DEBOUNCE_MS)
            .onEach { query ->
                if (query.length < MIN_QUERY_LENGTH) {
                    _uiState.update { 
                        it.copy(
                            userResults = defaultUserResults, 
                            chatResults = emptyList(), 
                            isLoading = false,
                            error = null
                        ) 
                    }
                }
            }
            .filter { it.length >= MIN_QUERY_LENGTH }
            .flatMapLatest { query ->
                flow {
                    emit(Result.Loading)
                    try {
                        val results = globalSearchUseCase(query)
                        emit(Result.Success(results))
                    } catch (e: Exception) {
                        emit(Result.Error(e.message ?: "Search failed"))
                    }
                }
            }
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is Result.Success -> _uiState.update { 
                        it.copy(
                            userResults = result.data.users, 
                            chatResults = result.data.chats, 
                            isLoading = false 
                        ) 
                    }
                    is Result.Error -> _uiState.update { 
                        it.copy(isLoading = false, error = result.message) 
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onCreateChat(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val chatId = createPrivateChatUseCase(userId)
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(SearchEffect.NavigateToChat(chatId))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(SearchEffect.ShowError(e.message ?: "Failed to create chat"))
            }
        }
    }

    private fun onJoinChat(chatId: String) {
        viewModelScope.launch {
            _effect.send(SearchEffect.NavigateToChat(chatId))
        }
    }

    private fun ru.kubsu.borshchevyk.core.model.domain.Chat.toSearchUser() = User(
        userId = partnerId!!,
        tag = partnerName ?: "",
        firstName = partnerName,
        lastName = null,
        avatarUrl = partnerAvatarUrl
    )

    private sealed interface Result {
        data object Loading : Result
        data class Success(val data: GlobalSearchResults) : Result
        data class Error(val message: String) : Result
    }

    companion object {
        private const val DEBOUNCE_MS = 500L
        private const val MIN_QUERY_LENGTH = 3
    }
}
