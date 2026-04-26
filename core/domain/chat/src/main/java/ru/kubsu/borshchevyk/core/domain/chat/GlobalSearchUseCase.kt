package ru.kubsu.borshchevyk.core.domain.chat

import ru.kubsu.borshchevyk.core.model.domain.GlobalSearchResults
import javax.inject.Inject

class GlobalSearchUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(query: String): GlobalSearchResults {
        return chatRepository.globalSearch(query)
    }
}
