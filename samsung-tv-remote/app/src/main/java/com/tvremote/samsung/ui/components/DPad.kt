package com.tvremote.samsung.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A single circular trackpad surface — arrows around the rim, OK in the center — rather than
 * five separate buttons scattered in a plus shape. Reads as one physical control, the way the
 * D-pad on a real remote (or an Apple TV remote's trackpad) does.
 */
@Composable
fun DPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onEnter: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val rimColor = MaterialTheme.colorScheme.onSurfaceVariant
    val alpha = if (enabled) 1f else 0.35f

    Box(
        modifier = modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        TrackpadZone(Icons.Filled.KeyboardArrowUp, "Up", enabled, rimColor, alpha, onUp, Modifier.align(Alignment.TopCenter))
        TrackpadZone(Icons.Filled.KeyboardArrowDown, "Down", enabled, rimColor, alpha, onDown, Modifier.align(Alignment.BottomCenter))
        TrackpadZone(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left", enabled, rimColor, alpha, onLeft, Modifier.align(Alignment.CenterStart))
        TrackpadZone(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right", enabled, rimColor, alpha, onRight, Modifier.align(Alignment.CenterEnd))

        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        ),
                    ),
                )
                .clickable(enabled = enabled, onClick = onEnter),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "OK",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun TrackpadZone(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    tint: androidx.compose.ui.graphics.Color,
    alpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint.copy(alpha = alpha))
    }
}
