package ru.kubsu.borshchevyk.feature.chat.conversation.interactor

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class AndroidVoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceRecorder {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    override fun startRecording(): Result<Unit> {
        return try {
            recorder?.release()
            recorder = null
            
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            Result.failure(e)
        }
    }

    override fun stopRecording(): Result<Uri> {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            
            val file = outputFile
            if (file != null && file.exists()) {
                Result.success(Uri.fromFile(file))
            } else {
                Result.failure(Exception("Output file does not exist"))
            }
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            Result.failure(e)
        }
    }

    override fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore errors on cancel
        } finally {
            recorder = null
            outputFile?.delete()
            outputFile = null
        }
    }
}
