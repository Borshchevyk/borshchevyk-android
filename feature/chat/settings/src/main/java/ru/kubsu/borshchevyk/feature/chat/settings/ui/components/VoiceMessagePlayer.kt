package ru.kubsu.borshchevyk.feature.chat.settings.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme

@Composable
internal fun VoiceMessagePlayer(
    url: String?,
    duration: Double,
    isFromMe: Boolean
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isPreparing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isSeeking by remember { mutableStateOf(false) }

    DisposableEffect(url) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            mediaPlayer?.let {
                try {
                    if (it.duration > 0) {
                        progress = it.currentPosition.toFloat() / it.duration.toFloat()
                    }
                } catch (e: Exception) {}
            }
            delay(50)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFromMe) BorshchevykTheme.colors.primaryContainer else BorshchevykTheme.colors.background
        ),
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isFromMe) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.primaryContainer, CircleShape)
                    .clickable {
                        if (isPlaying) {
                            mediaPlayer?.pause()
                            isPlaying = false
                        } else {
                            if (url != null && !isPreparing) {
                                if (mediaPlayer == null) {
                                    isPreparing = true
                                    val player = MediaPlayer().apply {
                                        setDataSource(url)
                                        setOnCompletionListener {
                                            isPlaying = false
                                            progress = 0f
                                            it.seekTo(0)
                                        }
                                        setOnPreparedListener {
                                            isPreparing = false
                                            isPlaying = true
                                            it.start()
                                        }
                                        setOnErrorListener { _, _, _ ->
                                            isPreparing = false
                                            isPlaying = false
                                            true
                                        }
                                    }
                                    mediaPlayer = player
                                    player.prepareAsync()
                                } else {
                                    if (progress >= 1f) {
                                        progress = 0f
                                        mediaPlayer?.seekTo(0)
                                    }
                                    mediaPlayer?.start()
                                    isPlaying = true
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isPreparing) {
                    CircularProgressIndicator(
                        color = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.primary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play Voice",
                        tint = if (isFromMe) BorshchevykTheme.colors.onPrimary else BorshchevykTheme.colors.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Slider(
                    value = progress,
                    onValueChange = { 
                        progress = it 
                        mediaPlayer?.let { player ->
                            player.seekTo((player.duration * it).toInt())
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = if (isFromMe) BorshchevykTheme.colors.onPrimaryContainer else BorshchevykTheme.colors.primary,
                        activeTrackColor = if (isFromMe) BorshchevykTheme.colors.primary else BorshchevykTheme.colors.primary,
                        inactiveTrackColor = if (isFromMe) BorshchevykTheme.colors.outline else BorshchevykTheme.colors.surfaceVariant
                    )
                )
                Text(
                    text = String.format("%d:%02d", (duration.toInt() / 60), (duration.toInt() % 60)),
                    style = BorshchevykTheme.typography.labelSmall,
                    color = if (isFromMe) BorshchevykTheme.colors.onPrimaryContainer else BorshchevykTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}
