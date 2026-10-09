package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceAction
import com.example.data.JarvisResponse
import com.example.data.JarvisState
import com.example.data.TerminalLog
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.BorderCyanBright
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.PureBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceTerminal
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SciFiTerminal(
    state: JarvisState,
    audioAmplitude: Float,
    currentTranscript: String,
    lastResponse: JarvisResponse?,
    pendingAction: DeviceAction?,
    logs: List<TerminalLog>,
    onExecuteActionNow: () -> Unit,
    onSpeakResponseAgain: () -> Unit,
    onStopSpeaking: () -> Unit = {},
    speechConfidence: com.example.data.SpeechConfidence? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TerminalCursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorBlink"
    )

    var showRawJson by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceTerminal)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(BorderCyanBright, BorderCyan, Color.Transparent)
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("sci_fi_terminal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Bar: HUD Status, Audio Waveform, Tech Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusDotColor = when (state) {
                        JarvisState.IDLE -> TerminalGreen
                        JarvisState.LISTENING -> ArcCyan
                        JarvisState.PROCESSING -> NeonBlue
                        JarvisState.SPEAKING -> TerminalAmber
                        JarvisState.EXECUTING_ACTION -> TerminalGreen
                        JarvisState.ERROR -> TerminalRed
                    }

                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STATUS // ${state.label}",
                        color = ArcCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                }

                // Audio Wave Visualizer Bars
                AudioWaveVisualizer(
                    amplitude = audioAmplitude,
                    isActive = state == JarvisState.LISTENING || state == JarvisState.SPEAKING
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-status banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "LOCALE: or-IN [ODIA]  |  TTS: HD VOICE ENGINE",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (lastResponse?.rawJson != null) {
                    Text(
                        text = if (showRawJson) "[HIDE JSON]" else "[VIEW JSON]",
                        color = ArcCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .testTag("toggle_json_button")
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Terminal Content Body (Scrollable)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PureBlack.copy(alpha = 0.6f))
                    .border(0.5.dp, BorderCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
                    .verticalScroll(scrollState)
            ) {
                // 1. User Transcript Display with Confidence Radar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "> USER INPUT [ODIA]:",
                        color = ArcCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )

                    if (speechConfidence != null) {
                        val badgeColor = when {
                            speechConfidence.isUnclear -> TerminalRed
                            speechConfidence.level == com.example.data.ConfidenceLevel.FAIR -> TerminalAmber
                            else -> TerminalGreen
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .border(0.5.dp, badgeColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (speechConfidence.isUnclear) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CLARITY: ${speechConfidence.percentage}% [${speechConfidence.level.label}]",
                                color = badgeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayText = if (currentTranscript.isNotBlank()) {
                        currentTranscript
                    } else if (state == JarvisState.LISTENING) {
                        "କୁହନ୍ତୁ, ଜାର୍ଭିସ୍ ଶୁଣୁଛି..."
                    } else {
                        "// ନିର୍ଦ୍ଦେଶ ଦେବା ପାଇଁ ମାଇକ୍ରୋଫୋନ୍ ଦବାନ୍ତୁ..."
                    }

                    Text(
                        text = displayText,
                        color = if (currentTranscript.isNotBlank()) TextPrimary else TextMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (state == JarvisState.LISTENING) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(16.dp)
                                .background(ArcCyan.copy(alpha = cursorAlpha))
                        )
                    }
                }

                // Phonetic Unclear Pronunciation Visual Alert
                if (speechConfidence != null && speechConfidence.isUnclear) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(TerminalRed.copy(alpha = 0.15f))
                            .border(1.dp, TerminalRed.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = TerminalRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚠ PHONETIC ALERT: Pronunciation clarity low (${speechConfidence.percentage}%). JARVIS engaging context recovery. Speak closer to mic if needed.",
                            color = TerminalRed,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Jarvis AI Response Display
                Text(
                    text = "> JARVIS RESPONSE [ODIA]:",
                    color = TerminalGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))

                if (lastResponse != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lastResponse.reply,
                            color = TerminalGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (state == JarvisState.SPEAKING) {
                            IconButton(
                                onClick = onStopSpeaking,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("stop_speaking_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StopCircle,
                                    contentDescription = "Stop speaking",
                                    tint = TerminalRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onSpeakResponseAgain,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("speak_again_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak response again",
                                tint = ArcCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else if (state == JarvisState.PROCESSING) {
                    Text(
                        text = "ଜାର୍ଭିସ୍ ବିଶ୍ଳେଷଣ କରୁଛି... [THINKING]",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = "// ପ୍ରତୀକ୍ଷାରେ ରହିଛି... [READY]",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 3. Raw JSON View (if expanded)
                if (showRawJson && lastResponse?.rawJson != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "// GEMINI STRICT JSON PAYLOAD:",
                        color = TerminalAmber,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = lastResponse.rawJson,
                        color = TerminalAmber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                            .padding(6.dp)
                    )
                }

                // 4. Action Execution Chip
                if (pendingAction != null && pendingAction != DeviceAction.NONE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ActionStatusBadge(
                        action = pendingAction,
                        isSpeaking = state == JarvisState.SPEAKING,
                        onExecuteNow = onExecuteActionNow
                    )
                }
            }
        }
    }
}

@Composable
fun AudioWaveVisualizer(
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 7
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")
        for (i in 0 until barCount) {
            val animFactor by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.85f,
                animationSpec = infiniteRepeatable(
                    animation = tween(250 + i * 50),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "WaveBar$i"
            )

            val heightFactor = if (isActive) {
                ((amplitude * 0.7f) + (animFactor * 0.4f)).coerceIn(0.15f, 1f)
            } else {
                0.15f
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((6 + heightFactor * 16).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) ArcCyan else TextMuted.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
fun ActionStatusBadge(
    action: DeviceAction,
    isSpeaking: Boolean,
    onExecuteNow: () -> Unit
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyanBright),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("action_status_badge")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = TerminalGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "ACTION: ${action.key}",
                        color = ArcCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isSpeaking) "TTS ସମାପ୍ତ ହେଲେ ଖୋଲିବ... (Or tap Run)" else "ACTION READY",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            FilledTonalButton(
                onClick = onExecuteNow,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = ArcCyan,
                    contentColor = PureBlack
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 10.dp,
                    vertical = 4.dp
                ),
                modifier = Modifier.testTag("action_execute_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RUN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
