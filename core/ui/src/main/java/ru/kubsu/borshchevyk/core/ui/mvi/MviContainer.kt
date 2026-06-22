package ru.kubsu.borshchevyk.core.ui.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A container that holds the MVI state and effects.
 * This replaces the BaseMviViewModel to favor composition over inheritance.
 */
interface MviContainer<S, E> {
    val uiState: StateFlow<S>
    val effect: Flow<E>
    
    fun updateState(transform: (S) -> S)
    fun sendEffect(effect: E)
}

/**
 * Factory function to create an MviContainer.
 */
fun <S, E> mviContainer(
    initialState: S,
    coroutineScope: CoroutineScope
): MviContainer<S, E> = MviContainerImpl(initialState, coroutineScope)

private class MviContainerImpl<S, E>(
    initialState: S,
    private val scope: CoroutineScope
) : MviContainer<S, E> {
    private val _uiState = MutableStateFlow(initialState)
    override val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    override val effect: Flow<E> = _effect.receiveAsFlow()

    override fun updateState(transform: (S) -> S) {
        _uiState.update(transform)
    }

    override fun sendEffect(effect: E) {
        scope.launch { _effect.send(effect) }
    }
}
