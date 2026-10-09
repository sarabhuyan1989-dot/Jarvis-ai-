package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.JarvisState
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanGlow
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.BorderCyanBright
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.PureBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun JarvisHomeScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val audioAmplitude by viewModel.audioAmplitude.collectAsState()
    val currentTranscript by viewModel.currentTranscript.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    val conversationHistory by viewModel.conversationHistory.collectAsState()
    val isContinuousMode by viewModel.isContinuousMode.collectAsState()
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val autoExecuteAfterTts by viewModel.autoExecuteAfterTts.collectAsState()
    val ttsEngineName by viewModel.ttsEngineName.collectAsState()
    val activeVoiceName by viewModel.activeVoiceName.collectAsState()
    val isOdiaVoiceActive by viewModel.isOdiaVoiceActive.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val speechPitch by viewModel.speechPitch.collectAsState()
    val speechConfidence by viewModel.speechConfidence.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    val hasMicPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicButtonClicked()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice interaction", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack),
        containerColor = PureBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top HUD Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark.copy(alpha = 0.7f))
                    .border(0.5.dp, BorderCyan, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    JarvisState.IDLE -> TerminalGreen
                                    JarvisState.LISTENING -> ArcCyan
                                    JarvisState.PROCESSING -> ElectricBlue
                                    JarvisState.SPEAKING -> TerminalAmber
                                    JarvisState.EXECUTING_ACTION -> TerminalGreen
                                    JarvisState.ERROR -> TerminalRed
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "JARVIS MK-IV // VOICE ONLY",
                            color = ArcCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "MODE: ${preferredLanguage.displayName}  |  STATUS: ${state.label}",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.clearConversation() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("clear_conversation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Reset Conversation",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = ArcCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Animated Center Holographic Arc Reactor Core
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable {
                        // Tapping the Arc Reactor also triggers mic interaction
                        if (hasMicPermission) {
                            viewModel.onMicButtonClicked()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ArcReactorView(
                        state = state,
                        audioAmplitude = audioAmplitude,
                        isUnclearPronunciation = speechConfidence?.isUnclear == true,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (state) {
                            JarvisState.IDLE -> "SYSTEM READY // TAP MIC TO SPEAK"
                            JarvisState.LISTENING -> "LISTENING IN ${preferredLanguage.name}..."
                            JarvisState.PROCESSING -> "NEURAL SYNAPSE // THINKING..."
                            JarvisState.SPEAKING -> "SPEAKING RESPONSE // TAP TO INTERRUPT"
                            JarvisState.EXECUTING_ACTION -> "DISPATCHING DEVICE COMMAND..."
                            JarvisState.ERROR -> "CORE ALERT // TAP TO RETRY"
                        },
                        color = when (state) {
                            JarvisState.LISTENING -> ArcCyan
                            JarvisState.PROCESSING -> ElectricBlue
                            JarvisState.SPEAKING -> TerminalAmber
                            JarvisState.ERROR -> TerminalRed
                            else -> TextSecondary
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Transparent Sci-Fi Terminal Block (Voice Transcript & Odia Response)
            SciFiTerminal(
                state = state,
                audioAmplitude = audioAmplitude,
                currentTranscript = currentTranscript,
                lastResponse = lastResponse,
                pendingAction = pendingAction,
                logs = terminalLogs,
                onExecuteActionNow = { viewModel.executePendingAction() },
                onSpeakResponseAgain = { viewModel.speakLastResponse() },
                onStopSpeaking = { viewModel.stopSpeaking() },
                speechConfidence = speechConfidence,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Quick Odia Spoken Prompt Chips
            QuickCommandsBar(
                onCommandSelected = { odiaCommand ->
                    viewModel.processSpokenOdiaInput(odiaCommand)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Hands-Free Mode Toggle Chip + Large Animated Microphone Controller
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Continuous Voice Mode Toggle Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isContinuousMode) SurfaceDark else PureBlack,
                    border = BorderStroke(
                        1.dp,
                        if (isContinuousMode) ArcCyan else BorderCyan.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .clickable { viewModel.toggleContinuousMode() }
                        .testTag("continuous_voice_toggle_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isContinuousMode) TerminalGreen else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isContinuousMode) "HANDS-FREE MODE: ACTIVE (AUTO-LISTEN)" else "HANDS-FREE MODE: OFF (MANUAL)",
                            color = if (isContinuousMode) ArcCyan else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Controls Row: Interruption/Stop Button + Large Neon Mic Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // If Speaking: Show Stop Speaking / Interrupt button on the left
                    if (state == JarvisState.SPEAKING) {
                        OutlinedButton(
                            onClick = { viewModel.stopSpeaking() },
                            shape = CircleShape,
                            border = BorderStroke(1.5.dp, TerminalRed),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceDark,
                                contentColor = TerminalRed
                            ),
                            modifier = Modifier
                                .size(50.dp)
                                .padding(end = 4.dp)
                                .testTag("stop_speaking_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.StopCircle,
                                contentDescription = "Stop Speaking",
                                tint = TerminalRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // Main Large Glowing Animated Microphone FAB
                    PrimaryVoiceControllerButton(
                        state = state,
                        audioAmplitude = audioAmplitude,
                        onClick = {
                            if (hasMicPermission) {
                                viewModel.onMicButtonClicked()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Clear visual label below microphone button
                Text(
                    text = when (state) {
                        JarvisState.IDLE -> "READY // TAP TO TALK"
                        JarvisState.LISTENING -> "LISTENING... (TAP TO SEND)"
                        JarvisState.PROCESSING -> "PROCESSING..."
                        JarvisState.SPEAKING -> "SPEAKING (TAP TO INTERRUPT)"
                        JarvisState.EXECUTING_ACTION -> "EXECUTING..."
                        JarvisState.ERROR -> "TAP TO RETRY"
                    },
                    color = when (state) {
                        JarvisState.LISTENING -> ArcCyan
                        JarvisState.SPEAKING -> TerminalAmber
                        JarvisState.ERROR -> TerminalRed
                        else -> TextMuted
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SciFiSettingsDialog(
                currentApiKey = customApiKey,
                autoExecuteAfterTts = autoExecuteAfterTts,
                isContinuousMode = isContinuousMode,
                preferredLanguage = preferredLanguage,
                ttsEngineName = ttsEngineName,
                activeVoiceName = activeVoiceName,
                isOdiaVoiceActive = isOdiaVoiceActive,
                availableVoices = availableVoices,
                speechRate = speechRate,
                speechPitch = speechPitch,
                hasMicPermission = hasMicPermission,
                onSaveApiKey = { viewModel.setCustomApiKey(it) },
                onToggleAutoExecute = { viewModel.setAutoExecute(it) },
                onToggleContinuousMode = { viewModel.toggleContinuousMode() },
                onSelectLanguage = { viewModel.setPreferredLanguage(it) },
                onSelectVoice = { viewModel.selectVoice(it) },
                onUpdateSpeechRate = { viewModel.updateSpeechRate(it) },
                onUpdateSpeechPitch = { viewModel.updateSpeechPitch(it) },
                onTestVoice = { viewModel.testOdiaVoiceSample() },
                onStopSpeaking = { viewModel.stopSpeaking() },
                onClearConversation = { viewModel.clearConversation() },
                onOpenSystemTtsSettings = { viewModel.openSystemTtsSettings() },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
fun PrimaryVoiceControllerButton(
    state: JarvisState,
    audioAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PrimaryMicAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val isListening = state == JarvisState.LISTENING
    val isSpeaking = state == JarvisState.SPEAKING

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing Animated Wave Ring when active
        if (isListening || isSpeaking) {
            val haloColor = if (isListening) ArcCyanGlow else TerminalAmber.copy(alpha = 0.35f)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale + (audioAmplitude * 0.4f))
                    .clip(CircleShape)
                    .background(haloColor)
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (isListening) ArcCyan else TerminalAmber, CircleShape)
            )
        }

        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = when (state) {
                JarvisState.LISTENING -> TerminalRed
                JarvisState.SPEAKING -> TerminalAmber
                JarvisState.PROCESSING -> ElectricBlue
                JarvisState.ERROR -> TerminalRed
                else -> ArcCyan
            },
            contentColor = PureBlack,
            elevation = FloatingActionButtonDefaults.elevation(10.dp),
            modifier = Modifier
                .size(68.dp)
                .testTag("primary_mic_button")
        ) {
            Icon(
                imageVector = when (state) {
                    JarvisState.LISTENING -> Icons.Default.MicOff
                    JarvisState.SPEAKING -> Icons.Default.RecordVoiceOver
                    JarvisState.PROCESSING -> Icons.Default.Hearing
                    else -> Icons.Default.Mic
                },
                contentDescription = when (state) {
                    JarvisState.LISTENING -> "Stop Listening"
                    JarvisState.SPEAKING -> "Tap to Interrupt"
                    else -> "Start Speaking"
                },
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
