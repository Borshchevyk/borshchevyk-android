package ru.kubsu.borshchevyk.feature.chat.conversation.interactor

import android.net.Uri

interface VoiceRecorder {
    fun startRecording(): Result<Unit>
    fun stopRecording(): Result<Uri>
    fun cancelRecording()
}
