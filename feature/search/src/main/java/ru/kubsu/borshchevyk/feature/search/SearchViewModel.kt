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
import ru.kubsu.borshchevyk.core.domain.chat.GlobalSearchUseCase
import ru.kubsu.borshchevyk.core.domain.chat.JoinChatUseCase
import ru.kubsu.borshchevyk.core.model.domain.ChatType
import ru.kubsu.borshchevyk.core.model.domain.User
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class)
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

    private val searchQueryFlow = MutableStateFlow("")
    private var defaultUserResults: List<User> = emptyList()

    init {
        loadDefaultResults()
        observeSearchQuery()
    }

    private fun loadDefaultResults() {
        viewModelScope.launch {
            try {
                val chats = getUserChatsUseCase()
                defaultUserResults = chats
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
                    _uiState.update { it.copy(userResults = defaultUserResults, chatResults = emptyList()) }
                }
            } catch (e: Exception) { }
        }
    }

    fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateQuery -> onQueryChange(intent.query)
            is SearchIntent.CreateChat -> onCreateChat(intent.userId)
            is SearchIntent.JoinChat -> onJoinChat(intent.chatId)
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
                    _uiState.update { it.copy(userResults = defaultUserResults, chatResults = emptyList(), isLoading = false) }
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

    private fun onJoinChat(chatId: String) {
        // Technically, for global search, clicking a public chat might just open it if we're already a member, 
        // or join it if we're not. For simplicity, we can call JoinChatUseCase which usually takes an invite link,
        // but maybe the backend allows joining by chatId for public chats?
        // If not, we just navigate to it. Let's just navigate to it. The server will add us if public.
        viewModelScope.launch {
             _effect.send(SearchEffect.NavigateToChat(chatId))
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val results = globalSearchUseCase(query)
                _uiState.update { it.copy(userResults = results.users, chatResults = results.chats, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(SearchEffect.ShowError(e.message ?: "Search failed"))
            }
        }
    }
}
