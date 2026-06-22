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

/**
 * The main Compose entry point for an active video/audio call.
 * This screen requests camera/microphone permissions, handles LiveKit video rendering,
 * and delegates logic to the [CallViewModel].
 *
 * @param viewModel The [CallViewModel] handling the call state and logic.
 * @param onNavigateBack Callback triggered when the call is ended or an unrecoverable error occurs.
 */
@Composable
fun CallScreen(
    viewModel: CallViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var isNavigatingBack by remember { mutableStateOf(false) }
    val navigateBackSafely = remember(onNavigateBack) {
        {
            if (!isNavigatingBack) {
                isNavigatingBack = true
                onNavigateBack()
            }
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            viewModel.handleIntent(CallIntent.Connect)
        } else {
            navigateBackSafely()
        }
    }

    LaunchedEffect(Unit) {
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CallEffect.CallEnded -> navigateBackSafely()
                is CallEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is CallUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Connecting...", color = Color.White)
                        }
                    }
                }
                is CallUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Text(state.message, color = Color.Red, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = navigateBackSafely) {
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
    }
}

/**
 * Composable responsible for rendering the full-screen active call UI,
 * including the remote video track, local video Picture-in-Picture (PiP), and control buttons.
 *
 * @param state The active [CallUiState.Active] state.
 * @param onIntent Callback to dispatch [CallIntent]s to the ViewModel.
 */
@Composable
private fun ActiveCallContent(
    state: CallUiState.Active,
    onIntent: (CallIntent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Remote Video
        if (state.remoteVideoTrack != null && !state.isRemoteVideoMuted) {
            io.livekit.android.compose.ui.VideoTrackView(
                passedRoom = state.room,
                videoTrack = state.remoteVideoTrack,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.isRemoteMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = if (state.isRemoteMicMuted) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.remoteParticipantName ?: "Waiting for participant...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Remote Status Indicators
        if (state.isRemoteMicMuted && state.remoteVideoTrack != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.MicOff,
                    contentDescription = "Remote Mic Muted",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(4.dp)
                )
            }
        }

        // Local video PIP
        if (state.localVideoTrack != null && state.isCameraEnabled) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp, end = 16.dp)
                    .size(100.dp, 150.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                io.livekit.android.compose.ui.VideoTrackView(
                    passedRoom = state.room,
                    videoTrack = state.localVideoTrack,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Controls
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth(),
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
}

/**
 * Composable responsible for rendering a minimized card banner for an active call.
 * This is useful when the user wants to browse the app while remaining on the call.
 *
 * @param state The active [CallUiState.Active] state.
 * @param onIntent Callback to dispatch [CallIntent]s to the ViewModel.
 */
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
                .width(220.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.remoteParticipantName ?: "Active Call",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                    IconButton(
                        onClick = { onIntent(CallIntent.ToggleMinimize) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.OpenInFull, contentDescription = "Expand", modifier = Modifier.size(18.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { onIntent(CallIntent.ToggleMic) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (state.isMicEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                            contentDescription = "Mic",
                            tint = if (state.isMicEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(
                        onClick = { onIntent(CallIntent.EndCall) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = "End", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/**
 * A reusable circular icon button designed for the call control interface.
 *
 * @param icon The material icon to display.
 * @param onClick The click listener.
 * @param isActive Determines whether the toggle state is currently active.
 * @param isDestructive Determines whether this is a destructive action (like ending a call).
 */
@Composable
private fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isActive: Boolean,
    isDestructive: Boolean = false
) {
    val bgColor = when {
        isDestructive -> MaterialTheme.colorScheme.error
        isActive -> Color.White.copy(alpha = 0.2f)
        else -> Color.White
    }
    val tintColor = when {
        isDestructive -> MaterialTheme.colorScheme.onError
        isActive -> Color.White
        else -> Color.Black
    }

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(32.dp)
        )
    }
}