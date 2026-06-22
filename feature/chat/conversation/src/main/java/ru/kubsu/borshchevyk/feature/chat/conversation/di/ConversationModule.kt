package ru.kubsu.borshchevyk.feature.chat.conversation.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.AndroidVoiceRecorder
import ru.kubsu.borshchevyk.feature.chat.conversation.interactor.VoiceRecorder

@Module
@InstallIn(SingletonComponent::class)
abstract class ConversationModule {

    @Binds
    abstract fun bindVoiceRecorder(
        androidVoiceRecorder: AndroidVoiceRecorder
    ): VoiceRecorder
}
