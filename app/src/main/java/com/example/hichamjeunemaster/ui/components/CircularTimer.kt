package com.example.hichamjeunemaster.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.hichamjeunemaster.R

@Composable
fun CircularTimer(
    progress: Float, // 0f to 1f
    timeText: String,
    phaseText: String,
    planName: String,
    elapsedTimeText: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    strokeWidth: Dp = 14.dp,
    isFasting: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val progressColor = if (isFasting) {
        Brush.sweepGradient(
            listOf(
                Color(0xFF00C853),
                Color(0xFF64FFDA),
                Color(0xFF00E5FF),
                Color(0xFF00C853)
            )
        )
    } else {
        Brush.sweepGradient(
            listOf(
                Color(0xFFFF9100),
                Color(0xFFFFEA00),
                Color(0xFFFF9100)
            )
        )
    }

    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val glowColor = if (isFasting) Color(0xFF2ECC71).copy(alpha = pulseAlpha * 0.3f)
    else Color(0xFFFF9100).copy(alpha = pulseAlpha * 0.3f)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (!isFasting) {
            Icon(
                imageVector = Icons.Rounded.Restaurant,
                contentDescription = null,
                modifier = Modifier.size(140.dp).alpha(0.08f),
                tint = Color(0xFFFF9100)
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val arcSize = Size(this.size.width, this.size.height)
            val stroke = strokeWidth.toPx()

            // Glow effect
            drawCircle(
                color = glowColor,
                radius = this.size.width / 2 - stroke,
            )

            // Cercle Alimentation (Assiette)
            if (!isFasting) {
                // Fond de l'assiette
                drawCircle(
                    color = Color(0xFFFF9100).copy(alpha = 0.15f),
                    radius = this.size.width / 2 - stroke * 2.5f,
                )
                // Bordure de l'assiette
                drawCircle(
                    color = Color(0xFFFF9100).copy(alpha = 0.4f),
                    radius = this.size.width / 2 - stroke * 2.5f,
                    style = Stroke(width = 4f)
                )
            }

            // Background track
            drawArc(
                color = bgColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = arcSize
            )

            val currentSweep = if (isFasting) animatedProgress * 360f else (1f - animatedProgress) * 360f

            // Progress arc
            drawArc(
                brush = progressColor,
                startAngle = -90f,
                sweepAngle = currentSweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = arcSize
            )

            // Dot at current position
            if (currentSweep > 0.01f) {
                val angle = Math.toRadians((-90.0 + currentSweep))
                val radius = this.size.width / 2
                val dotX = (this.size.width / 2 + radius * Math.cos(angle)).toFloat()
                val dotY = (this.size.height / 2 + radius * Math.sin(angle)).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = stroke / 2 + 4f,
                    center = Offset(dotX, dotY)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (elapsedTimeText != null) {
                Text(
                    text = stringResource(R.string.elapsed_time, elapsedTimeText),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = phaseText,
                style = MaterialTheme.typography.labelLarge,
                color = if (isFasting) Color(0xFF2ECC71) else Color(0xFFFF9100)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeText,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = planName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
