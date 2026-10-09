package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.JarvisState
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorView(
    state: JarvisState,
    audioAmplitude: Float,
    isUnclearPronunciation: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorTransitions")

    // Slow continuous outer rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OuterRingRotation"
    )

    // Counter-clockwise inner rotation
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "InnerRingRotation"
    )

    // Core idle breathing pulse
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdlePulse"
    )

    // Dynamic scale driven by audio amplitude and state
    val stateScaleBoost = when (state) {
        JarvisState.LISTENING -> 0.15f * audioAmplitude.coerceIn(0f, 1f)
        JarvisState.SPEAKING -> 0.12f * (idlePulse - 0.94f) * 10f
        JarvisState.PROCESSING -> 0.08f * (idlePulse - 0.94f) * 5f
        else -> 0f
    }
    val currentScale = (idlePulse + stateScaleBoost).coerceIn(0.9f, 1.35f)

    // Glow color depending on state and phonetic confidence
    val primaryGlowColor = when {
        isUnclearPronunciation -> TerminalAmber
        state == JarvisState.LISTENING -> ArcCyan
        state == JarvisState.PROCESSING -> NeonBlue
        state == JarvisState.SPEAKING -> TerminalGreen
        state == JarvisState.ERROR -> TerminalRed
        else -> ArcCyan
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("arc_reactor_container"),
        contentAlignment = Alignment.Center
    ) {
        // Holographic Rings & Tech Glyphs Canvas
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .scale(currentScale)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f

            // 1. Ambient outer glow circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlowColor.copy(alpha = 0.35f),
                        primaryGlowColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // 2. Outer Rotating Ticks Ring
            rotate(outerRotation, pivot = center) {
                val tickRingRadius = maxRadius * 0.92f
                val tickCount = 48
                for (i in 0 until tickCount) {
                    val angle = (i * (360f / tickCount)) * (Math.PI / 180.0)
                    val isMajor = i % 4 == 0
                    val len = if (isMajor) 10.dp.toPx() else 4.dp.toPx()
                    val stroke = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                    val alpha = if (isMajor) 0.85f else 0.4f

                    val startX = center.x + (tickRingRadius - len) * cos(angle).toFloat()
                    val startY = center.y + (tickRingRadius - len) * sin(angle).toFloat()
                    val endX = center.x + tickRingRadius * cos(angle).toFloat()
                    val endY = center.y + tickRingRadius * sin(angle).toFloat()

                    drawLine(
                        color = primaryGlowColor.copy(alpha = alpha),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                // Outer segmented arcs
                val arcStroke = 2.dp.toPx()
                drawArc(
                    color = primaryGlowColor.copy(alpha = 0.7f),
                    startAngle = 10f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = arcStroke)
                )
                drawArc(
                    color = primaryGlowColor.copy(alpha = 0.7f),
                    startAngle = 100f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = arcStroke)
                )
                drawArc(
                    color = primaryGlowColor.copy(alpha = 0.7f),
                    startAngle = 190f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = arcStroke)
                )
                drawArc(
                    color = primaryGlowColor.copy(alpha = 0.7f),
                    startAngle = 280f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = arcStroke)
                )
            }

            // 3. Middle Counter-Rotating Technological Ring
            rotate(innerRotation, pivot = center) {
                val midRadius = maxRadius * 0.78f
                drawCircle(
                    color = ElectricBlue.copy(alpha = 0.45f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // 6 Technological sector blocks
                for (j in 0 until 6) {
                    val startAngle = j * 60f + 12f
                    drawArc(
                        color = primaryGlowColor.copy(alpha = 0.8f),
                        startAngle = startAngle,
                        sweepAngle = 36f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Butt)
                    )
                }
            }

            // 4. Reactive Audio Waveform Ring (expands with voice)
            val audioWaveRadius = maxRadius * (0.64f + audioAmplitude * 0.12f)
            drawCircle(
                color = primaryGlowColor.copy(alpha = (0.3f + audioAmplitude * 0.6f).coerceIn(0f, 1f)),
                radius = audioWaveRadius,
                center = center,
                style = Stroke(width = (2.dp + (audioAmplitude * 4f).dp).toPx())
            )
        }

        // Center Core Arc Reactor Asset with glowing frame
        Box(
            modifier = Modifier
                .size(136.dp)
                .scale(currentScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryGlowColor.copy(alpha = 0.4f),
                            Color.Black
                        )
                    )
                )
                .border(2.dp, primaryGlowColor.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_jarvis_logo),
                contentDescription = "Jarvis Arc Reactor Core",
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Inner glowing aperture halo
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f),
                                primaryGlowColor.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
