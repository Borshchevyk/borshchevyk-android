package ru.kubsu.borshchevyk.feature.chat.ui.chat.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import ru.kubsu.borshchevyk.core.ui.theme.BorshchevykTheme
import kotlin.math.atan2

/**
 * A circular video player component used for playing circle video messages.
 *
 * @param url The URL of the video to play.
 * @param duration The duration of the video in seconds.
 * @param imageRequest The image request for the video thumbnail/preview.
 * @param loadError Whether an error occurred while loading the video preview.
 * @param onLoadError Callback invoked when the video preview fails to load.
 */
@Composable
internal fun CircleVideoPlayer(
    url: String?,
    duration: Double,
    imageRequest: ImageRequest?,
    loadError: Boolean,
    onLoadError: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isPreparing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isSeeking by remember { mutableStateOf(false) }
    var videoSurface by remember { mutableStateOf<Surface?>(null) }

    DisposableEffect(url) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(mediaPlayer, videoSurface) {
        if (mediaPlayer != null && videoSurface != null) {
            mediaPlayer?.setSurface(videoSurface)
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

    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            /** Called when the surface texture is ready for use. Initializes the video surface. */
                            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                videoSurface = Surface(surface)
                            }
                            /** Called when the surface texture's buffers size changes. Currently a no-op. */
                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                            /** Called when the surface texture is about to be destroyed. Releases the video surface. */
                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                videoSurface?.release()
                                videoSurface = null
                                mediaPlayer?.setSurface(null)
                                return true
                            }
                            /** Called when the surface texture is updated through a new frame. Currently a no-op. */
                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }

        // Circular Bezel Progress Indicator
        val primaryColor = BorshchevykTheme.colors.primary
        val trackColor = Color.White.copy(alpha = 0.3f)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width.toFloat() / 2f, size.height.toFloat() / 2f)
                        val angle = atan2(offset.y - center.y, offset.x - center.x)
                        var normalizedAngle = (angle * 180 / Math.PI).toFloat() + 90f
                        if (normalizedAngle < 0) normalizedAngle += 360f
                        val newProgress = (normalizedAngle / 360f).coerceIn(0f, 1f)
                        progress = newProgress
                        mediaPlayer?.let {
                            it.seekTo((it.duration * newProgress).toInt())
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isSeeking = true },
                        onDragEnd = { isSeeking = false },
                        onDragCancel = { isSeeking = false }
                    ) { change, _ ->
                        val center = Offset(size.width.toFloat() / 2f, size.height.toFloat() / 2f)
                        val angle = atan2(change.position.y - center.y, change.position.x - center.x)
                        var normalizedAngle = (angle * 180 / Math.PI).toFloat() + 90f
                        if (normalizedAngle < 0) normalizedAngle += 360f
                        val newProgress = (normalizedAngle / 360f).coerceIn(0f, 1f)
                        progress = newProgress
                        mediaPlayer?.let {
                            it.seekTo((it.duration * newProgress).toInt())
                        }
                    }
                }
        ) {
            val strokeWidth = 8.dp.toPx()
            val sizeOuter = size.width - strokeWidth
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(sizeOuter, sizeOuter),
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(sizeOuter, sizeOuter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        if (!isPlaying) {
            if (url != null && !loadError) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "Video Circle",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = { onLoadError() }
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .clickable {
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
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isPreparing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Circle",
                        tint = Color.White
                    )
                }
            }
            Text(
                text = String.format("0:%02d", (duration.toInt() % 60)),
                color = Color.White,
                style = BorshchevykTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        } else {
            // Invisible overlay for pause to prevent interfering with bezel seeking
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // padding so we don't catch ring touches
                    .clip(CircleShape)
                    .clickable {
                        mediaPlayer?.pause()
                        isPlaying = false
                    }
            )
        }
    }
}
