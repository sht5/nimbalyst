package com.tvremote.samsung.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tvremote.samsung.data.RemoteKey
import com.tvremote.samsung.network.TvConnectionState
import com.tvremote.samsung.ui.components.DPad

private val PowerRed = Color(0xFFE0525C)
private val WakeAmber = Color(0xFFF0A94E)

@Composable
fun RemoteScreen(
    connectionState: TvConnectionState,
    canWake: Boolean,
    tvName: String,
    onKey: (RemoteKey) -> Unit,
    onPowerTap: () -> Unit,
    onSwitchTv: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = connectionState is TvConnectionState.Connected
    val isWaking = connectionState is TvConnectionState.WakingUp
    val showWakeState = !isConnected && connectionState != TvConnectionState.Connecting &&
        connectionState != TvConnectionState.AwaitingPairing && canWake
    val controlsEnabled = isConnected

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar(connectionState, tvName, onSwitchTv)
        Spacer(Modifier.height(18.dp))

        PowerSourceRow(
            showWakeState = showWakeState,
            isWaking = isWaking,
            onPowerTap = onPowerTap,
            onSource = { onKey(RemoteKey.SOURCE) },
            sourceEnabled = controlsEnabled,
        )

        if (showWakeState) {
            Spacer(Modifier.height(14.dp))
            WakeBanner(connectionState)
        }

        Spacer(Modifier.height(20.dp))

        DPad(
            onUp = { onKey(RemoteKey.UP) },
            onDown = { onKey(RemoteKey.DOWN) },
            onLeft = { onKey(RemoteKey.LEFT) },
            onRight = { onKey(RemoteKey.RIGHT) },
            onEnter = { onKey(RemoteKey.ENTER) },
            enabled = controlsEnabled,
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavItem(Icons.AutoMirrored.Filled.ArrowBack, "Back", controlsEnabled) { onKey(RemoteKey.BACK) }
            NavItem(Icons.Filled.Home, "Home", controlsEnabled) { onKey(RemoteKey.HOME) }
            NavItem(Icons.Filled.Menu, "Menu", controlsEnabled) { onKey(RemoteKey.MENU) }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            RockerColumn("Volume", controlsEnabled, { onKey(RemoteKey.VOLUME_UP) }, { onKey(RemoteKey.VOLUME_DOWN) })
            MuteColumn(controlsEnabled) { onKey(RemoteKey.MUTE) }
            RockerColumn("Channel", controlsEnabled, { onKey(RemoteKey.CHANNEL_UP) }, { onKey(RemoteKey.CHANNEL_DOWN) })
        }

        Spacer(Modifier.height(18.dp))

        TransportRow(controlsEnabled, onKey)

        Spacer(Modifier.height(18.dp))

        NumberPadChip(controlsEnabled, onKey)
    }
}

@Composable
private fun TopBar(connectionState: TvConnectionState, tvName: String, onSwitchTv: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = tvName,
                style = MaterialTheme.typography.labelLarge,
            )
            StatusPill(connectionState)
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onSwitchTv),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Switch TV",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun StatusPill(state: TvConnectionState) {
    val (label, color) = when (state) {
        TvConnectionState.Idle -> "Not connected" to MaterialTheme.colorScheme.onSurfaceVariant
        TvConnectionState.Connecting -> "Connecting…" to WakeAmber
        TvConnectionState.AwaitingPairing -> "Accept the prompt on your TV" to WakeAmber
        TvConnectionState.Connected -> "Connected" to Color(0xFF3FD98C)
        TvConnectionState.Disconnected -> "TV is off" to MaterialTheme.colorScheme.onSurfaceVariant
        TvConnectionState.WakingUp -> "Waking TV…" to WakeAmber
        is TvConnectionState.Error -> "Error" to PowerRed
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PowerSourceRow(
    showWakeState: Boolean,
    isWaking: Boolean,
    onPowerTap: () -> Unit,
    onSource: () -> Unit,
    sourceEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PowerButton(showWakeState = showWakeState, isWaking = isWaking, onClick = onPowerTap)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (showWakeState) (if (isWaking) "Waking…" else "Tap to turn on") else "Power",
                style = MaterialTheme.typography.labelSmall,
                color = if (showWakeState) WakeAmber else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (sourceEnabled) 1f else 0.35f))
                .clickable(enabled = sourceEnabled, onClick = onSource)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.SettingsInputHdmi, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Source", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PowerButton(showWakeState: Boolean, isWaking: Boolean, onClick: () -> Unit) {
    val tint = if (showWakeState) WakeAmber else PowerRed
    val infiniteTransition = rememberInfiniteTransition(label = "power-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse-scale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse-alpha",
    )

    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        if (isWaking) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .clip(CircleShape)
                    .border(1.5.dp, tint, CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f))
                .border(1.dp, tint.copy(alpha = 0.4f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Power, contentDescription = "Power", tint = tint)
        }
    }
}

@Composable
private fun WakeBanner(state: TvConnectionState) {
    // Error carries a specific reason (wake timeout, pairing rejection, etc.) — show that
    // verbatim rather than a generic message that could mislead about what actually happened.
    val text = when (state) {
        is TvConnectionState.Error -> state.message
        TvConnectionState.WakingUp -> "Sending a wake signal and waiting for the TV to come back on the network…"
        else -> "Connection dropped when the TV powered off. Tap Power to send a wake signal — " +
            "we'll reconnect automatically once it responds."
    }
    val tint = if (state is TvConnectionState.Error) PowerRed else WakeAmber

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Icon(Icons.Filled.Bolt, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.35f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
    }
}

@Composable
private fun RockerColumn(label: String, enabled: Boolean, onUp: () -> Unit, onDown: () -> Unit) {
    val alpha = if (enabled) 1f else 0.35f
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .width(46.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(enabled = enabled, onClick = onUp),
                contentAlignment = Alignment.Center,
            ) { Text("+", style = MaterialTheme.typography.titleMedium) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(enabled = enabled, onClick = onDown),
                contentAlignment = Alignment.Center,
            ) { Text("−", style = MaterialTheme.typography.titleMedium) }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
    }
}

@Composable
private fun MuteColumn(enabled: Boolean, onMute: () -> Unit) {
    val alpha = if (enabled) 1f else 0.35f
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 30.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                .clickable(enabled = enabled, onClick = onMute),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Mute", modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text("Mute", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
    }
}

@Composable
private fun TransportRow(enabled: Boolean, onKey: (RemoteKey) -> Unit) {
    var isPlaying by remember { mutableStateOf(true) }
    val alpha = if (enabled) 1f else 0.35f

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        TransportButton(Icons.Filled.FastRewind, "Rewind", enabled, alpha) { onKey(RemoteKey.REWIND) }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                .clickable(enabled = enabled) {
                    isPlaying = !isPlaying
                    onKey(if (isPlaying) RemoteKey.PLAY else RemoteKey.PAUSE)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        TransportButton(Icons.Filled.FastForward, "Fast forward", enabled, alpha) { onKey(RemoteKey.FAST_FORWARD) }
    }
}

@Composable
private fun TransportButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, alpha: Float, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
    }
}

private val numberKeys = listOf(
    RemoteKey.NUM_1, RemoteKey.NUM_2, RemoteKey.NUM_3,
    RemoteKey.NUM_4, RemoteKey.NUM_5, RemoteKey.NUM_6,
    RemoteKey.NUM_7, RemoteKey.NUM_8, RemoteKey.NUM_9,
    RemoteKey.NUM_0,
)

@Composable
private fun NumberPadChip(enabled: Boolean, onKey: (RemoteKey) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val alpha = if (enabled) 1f else 0.35f

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            .clickable(enabled = enabled) { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Dialpad, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text("Number pad", style = MaterialTheme.typography.labelMedium)
    }

    if (expanded && enabled) {
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(numberKeys) { key ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onKey(key) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(key.code.removePrefix("KEY_"), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
