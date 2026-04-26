package ru.kubsu.borshchevyk.feature.call.ui.call

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.VideoTrack

@Composable
fun CallScreen(
    viewModel: CallViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            viewModel.handleIntent(CallIntent.Connect)
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        )
        viewModel.effect.collect { effect ->
            when (effect) {
                is CallEffect.CallEnded -> onNavigateBack()
                is CallEffect.ShowError -> {} // Handle error (e.g., Snackbar)
            }
        }
    }

    when (val state = uiState) {
        is CallUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Text("Connecting...", color = Color.White, modifier = Modifier.padding(top = 48.dp))
            }
        }
        is CallUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        }
        is CallUiState.Active -> {
            if (state.isMinimized) {
                MinimizedCallBanner(
                    state = state,
                    onIntent = viewModel::handleIntent
                )
            } else {
                ActiveCallContent(
                    state = state,
                    onIntent = viewModel::handleIntent
                )
            }
        }
    }
}

@Composable
private fun ActiveCallContent(
    state: CallUiState.Active,
    onIntent: (CallIntent) -> Unit
) {
    var remoteParticipant by remember { mutableStateOf<Participant?>(null) }
    var remoteVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var localVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }

    LaunchedEffect(state.room) {
        // Simple polling for participant updates, normally done via Room events
        while (true) {
            val participants = state.room.remoteParticipants
            val firstRemote = participants.values.firstOrNull()
            remoteParticipant = firstRemote
            remoteVideoTrack = firstRemote?.videoTrackPublications?.firstOrNull()?.first?.track as? VideoTrack
            localVideoTrack = state.room.localParticipant.videoTrackPublications.firstOrNull()?.first?.track as? VideoTrack
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
        if (remoteVideoTrack != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
                Text("Remote Video", color = Color.White)
            }
        } else {
            // Audio only or waiting
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (remoteParticipant != null) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                } else {
                    Text("Waiting for others to join...", color = Color.White)
                }
            }
        }

        // Local video PIP (bottom right)
        if (localVideoTrack != null && state.isCameraEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 16.dp)
                    .size(100.dp, 150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("Local", color = Color.Black)
            }
        }

        // Controls (bottom)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlIconButton(
                icon = if (state.isMicEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                onClick = { onIntent(CallIntent.ToggleMic) },
                isActive = state.isMicEnabled
            )
            ControlIconButton(
                icon = if (state.isCameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                onClick = { onIntent(CallIntent.ToggleCamera) },
                isActive = state.isCameraEnabled
            )
            ControlIconButton(
                icon = Icons.Filled.CloseFullscreen,
                onClick = { onIntent(CallIntent.ToggleMinimize) },
                isActive = true
            )
            ControlIconButton(
                icon = Icons.Filled.CallEnd,
                onClick = { onIntent(CallIntent.EndCall) },
                isActive = false,
                isDestructive = true
            )
        }
    }
}

@Composable
private fun MinimizedCallBanner(
    state: CallUiState.Active,
    onIntent: (CallIntent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(200.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Call",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { onIntent(CallIntent.ToggleMinimize) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.OpenInFull, contentDescription = "Expand", modifier = Modifier.size(16.dp))
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { onIntent(CallIntent.ToggleMic) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (state.isMicEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                            contentDescription = "Mic",
                            tint = if (state.isMicEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(
                        onClick = { onIntent(CallIntent.EndCall) },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = "End", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isActive: Boolean,
    isDestructive: Boolean = false
) {
    val bgColor = when {
        isDestructive -> Color.Red
        isActive -> Color.White.copy(alpha = 0.2f)
        else -> Color.White
    }
    val tintColor = when {
        isDestructive -> Color.White
        isActive -> Color.White
        else -> Color.Black
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(28.dp)
        )
    }
}