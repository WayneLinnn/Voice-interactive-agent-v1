package com.waynelinnn.voiceagent.presentation.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.waynelinnn.voiceagent.domain.model.ListeningState
import com.waynelinnn.voiceagent.presentation.theme.QuantisBlue
import com.waynelinnn.voiceagent.presentation.theme.QuantisBrandGradient
import com.waynelinnn.voiceagent.presentation.theme.QuantisCoral
import com.waynelinnn.voiceagent.presentation.theme.QuantisMagenta
import com.waynelinnn.voiceagent.presentation.theme.QuantisViolet
import kotlin.math.sin

@Composable
fun VoiceOrb(
    listeningState: ListeningState,
    sessionRunning: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 236.dp,
) {
    val active = sessionRunning && listeningState != ListeningState.Idle
    val infinite = rememberInfiniteTransition(label = "quantis-orb")
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = if (active) 1.07f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (listeningState) {
                    ListeningState.SpeechDetected -> 650
                    ListeningState.Speaking -> 1000
                    ListeningState.Thinking, ListeningState.Recognizing -> 1300
                    else -> 2400
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (listeningState == ListeningState.Thinking) 3600 else 10000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    val wave by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave",
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = (this.size.minDimension / 2f) * 0.40f * pulse
        val ringBrush = Brush.sweepGradient(
            colors = QuantisBrandGradient + QuantisBrandGradient.first(),
            center = center,
        )
        val fillBrush = Brush.linearGradient(
            colors = listOf(QuantisCoral, QuantisMagenta, QuantisViolet, QuantisBlue),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius, center.y + radius),
        )

        // Soft brand wash (restrained, not neon bloom stack)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    QuantisMagenta.copy(alpha = if (active) 0.28f else 0.14f),
                    QuantisViolet.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                center = center,
                radius = radius * 2.6f,
            ),
            radius = radius * 2.6f,
            center = center,
        )

        for (i in 1..3) {
            val ringScale = 1f + i * 0.20f + (if (active) 0.035f * sin(wave + i) else 0f)
            drawCircle(
                color = QuantisMagenta.copy(alpha = 0.28f - i * 0.06f),
                radius = radius * ringScale,
                center = center,
                style = Stroke(width = 2.2f),
            )
        }

        // Core — dark disc with gradient rim
        drawCircle(
            color = Color(0xFF0A0A0E),
            radius = radius,
            center = center,
        )
        drawCircle(
            brush = fillBrush,
            radius = radius,
            center = center,
            style = Stroke(width = 7f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                center = center - Offset(radius * 0.25f, radius * 0.3f),
                radius = radius,
            ),
            radius = radius * 0.92f,
            center = center,
        )

        if (listeningState == ListeningState.Thinking || listeningState == ListeningState.Recognizing) {
            drawArc(
                brush = ringBrush,
                startAngle = sweep,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.18f, center.y - radius * 1.18f),
                size = Size(radius * 2.36f, radius * 2.36f),
                style = Stroke(width = 5f, cap = StrokeCap.Round),
            )
        }
        if (listeningState == ListeningState.Error) {
            drawCircle(
                color = Color(0xFFFF6B6B).copy(alpha = 0.35f),
                radius = radius * 1.08f,
                center = center,
                style = Stroke(width = 4f),
            )
        }
        if (listeningState == ListeningState.WakeListening) {
            drawArc(
                brush = ringBrush,
                startAngle = sweep,
                sweepAngle = 44f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.22f, center.y - radius * 1.22f),
                size = Size(radius * 2.44f, radius * 2.44f),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round),
            )
        }
    }
}
