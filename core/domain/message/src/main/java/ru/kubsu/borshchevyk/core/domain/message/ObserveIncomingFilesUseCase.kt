package ru.kubsu.borshchevyk.core.domain.message

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveIncomingFilesUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<String> {
        return repository.observeIncomingFiles()
    }
}
