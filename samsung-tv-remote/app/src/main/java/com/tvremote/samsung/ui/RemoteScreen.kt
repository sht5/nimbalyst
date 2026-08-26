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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tvremote.samsung.data.AndroidTvKey
import com.tvremote.samsung.data.DeviceKind
import com.tvremote.samsung.data.RemoteKey
import com.tvremote.samsung.network.TvConnectionState
import com.tvremote.samsung.network.androidtv.AndroidTvConnectionState
import com.tvremote.samsung.ui.components.DPad

private val PowerRed = Color(0xFFE0525C)
private val WakeAmber = Color(0xFFF0A94E)
private val ConnectedGreen = Color(0xFF3FD98C)

@Composable
fun RemoteScreen(
    connectionState: TvConnectionState,
    androidTvState: AndroidTvConnectionState,
    activeDevice: DeviceKind,
    canWake: Boolean,
    tvName: String,
    androidTvName: String,
    onKey: (RemoteKey) -> Unit,
    onAndroidTvKey: (AndroidTvKey) -> Unit,
    onPowerTap: () -> Unit,
    onSwitchTv: () -> Unit,
    onSelectDevice: (DeviceKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ActiveDeviceStatusBar(
            activeDevice = activeDevice,
            connectionState = connectionState,
            androidTvState = androidTvState,
            tvName = tvName,
            androidTvName = androidTvName,
            onSwitchTv = onSwitchTv,
        )
        Spacer(Modifier.height(18.dp))

        if (activeDevice == DeviceKind.TV) {
            TvControls(connectionState, canWake, onKey, onPowerTap)
        } else {
            AndroidTvControls(androidTvState, onAndroidTvKey)
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(18.dp))

        DeviceSwitchBar(
            tvConnected = connectionState is TvConnectionState.Connected,
            androidTvConnected = androidTvState is AndroidTvConnectionState.Connected,
            activeDevice = activeDevice,
            tvName = tvName,
            androidTvName = androidTvName,
            onSelectDevice = onSelectDevice,
        )
    }
}

/** A single line for whichever device is active — name, status pill, and the settings/forget action. */
@Composable
private fun ActiveDeviceStatusBar(
    activeDevice: DeviceKind,
    connectionState: TvConnectionState,
    androidTvState: AndroidTvConnectionState,
    tvName: String,
    androidTvName: String,
    onSwitchTv: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                text = if (activeDevice == DeviceKind.TV) tvName else androidTvName,
                style = MaterialTheme.typography.labelLarge,
            )
            if (activeDevice == DeviceKind.TV) StatusPill(connectionState) else AndroidTvStatusPill(androidTvState)
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
private fun AndroidTvStatusPill(state: AndroidTvConnectionState) {
    val (label, color) = when (state) {
        AndroidTvConnectionState.Idle -> "Not connected" to MaterialTheme.colorScheme.onSurfaceVariant
        AndroidTvConnectionState.Connecting -> "Connecting…" to WakeAmber
        AndroidTvConnectionState.AwaitingPairingCode -> "Enter the code on screen" to WakeAmber
        AndroidTvConnectionState.Connected -> "Connected" to ConnectedGreen
        AndroidTvConnectionState.Disconnected -> "Disconnected" to MaterialTheme.colorScheme.onSurfaceVariant
        is AndroidTvConnectionState.Error -> "Error" to PowerRed
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Full-width segmented toggle at the bottom of the screen, easy to reach one-handed. */
@Composable
private fun DeviceSwitchBar(
    tvConnected: Boolean,
    androidTvConnected: Boolean,
    activeDevice: DeviceKind,
    tvName: String,
    androidTvName: String,
    onSelectDevice: (DeviceKind) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SwitchSegment(tvName, tvConnected, activeDevice == DeviceKind.TV, Modifier.weight(1f)) { onSelectDevice(DeviceKind.TV) }
        SwitchSegment(androidTvName, androidTvConnected, activeDevice == DeviceKind.ANDROID_TV, Modifier.weight(1f)) { onSelectDevice(DeviceKind.ANDROID_TV) }
    }
}

@Composable
private fun SwitchSegment(label: String, connected: Boolean, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val dotColor = if (connected) ConnectedGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val textColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Samsung TV pane
// ---------------------------------------------------------------------------------------------

@Composable
private fun TvControls(
    connectionState: TvConnectionState,
    canWake: Boolean,
    onKey: (RemoteKey) -> Unit,
    onPowerTap: () -> Unit,
) {
    val isConnected = connectionState is TvConnectionState.Connected
    val isWaking = connectionState is TvConnectionState.WakingUp
    val showWakeState = !isConnected && connectionState != TvConnectionState.Connecting &&
        connectionState != TvConnectionState.AwaitingPairing && canWake
    val controlsEnabled = isConnected

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

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
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

@Composable
private fun StatusPill(state: TvConnectionState) {
    val (label, color) = when (state) {
        TvConnectionState.Idle -> "Not connected" to MaterialTheme.colorScheme.onSurfaceVariant
        TvConnectionState.Connecting -> "Connecting…" to WakeAmber
        TvConnectionState.AwaitingPairing -> "Accept the prompt on your TV" to WakeAmber
        TvConnectionState.Connected -> "Connected" to ConnectedGreen
        TvConnectionState.Disconnected -> "TV is off" to MaterialTheme.colorScheme.onSurfaceVariant
        TvConnectionState.WakingUp -> "Waking TV…" to WakeAmber
        is TvConnectionState.Error -> "Error" to PowerRed
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
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
            PowerButton(tint = if (showWakeState) WakeAmber else PowerRed, pulsing = isWaking, onClick = onPowerTap)
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
private fun PowerButton(tint: Color, pulsing: Boolean, onClick: () -> Unit) {
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
        if (pulsing) {
            Box(
                modifier = Modifier
                    .size(56.dp * pulseScale)
                    .clip(CircleShape)
                    .border(1.5.dp, tint.copy(alpha = pulseAlpha), CircleShape),
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
    ErrorBanner(text, danger = state is TvConnectionState.Error)
}

@Composable
private fun ErrorBanner(text: String, danger: Boolean = true) {
    val tint = if (danger) PowerRed else WakeAmber
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

// ---------------------------------------------------------------------------------------------
// Mecool (Android TV) pane
// ---------------------------------------------------------------------------------------------

@Composable
private fun AndroidTvControls(state: AndroidTvConnectionState, onKey: (AndroidTvKey) -> Unit) {
    val controlsEnabled = state is AndroidTvConnectionState.Connected

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PowerRed.copy(alpha = 0.14f))
                        .border(1.dp, PowerRed.copy(alpha = 0.4f), CircleShape)
                        .clickable(enabled = controlsEnabled) { onKey(AndroidTvKey.POWER) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Power, contentDescription = "Power", tint = PowerRed.copy(alpha = if (controlsEnabled) 1f else 0.5f))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Power", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (controlsEnabled) 1f else 0.35f))
                .clickable(enabled = controlsEnabled) { onKey(AndroidTvKey.SEARCH) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Voice search", style = MaterialTheme.typography.labelMedium)
        }
    }

    if (state is AndroidTvConnectionState.Error) {
        Spacer(Modifier.height(14.dp))
        ErrorBanner(state.message)
    } else if (state is AndroidTvConnectionState.Connecting || state is AndroidTvConnectionState.AwaitingPairingCode) {
        Spacer(Modifier.height(14.dp))
        Text(
            "Connecting to the streamer…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(20.dp))

    DPad(
        onUp = { onKey(AndroidTvKey.DPAD_UP) },
        onDown = { onKey(AndroidTvKey.DPAD_DOWN) },
        onLeft = { onKey(AndroidTvKey.DPAD_LEFT) },
        onRight = { onKey(AndroidTvKey.DPAD_RIGHT) },
        onEnter = { onKey(AndroidTvKey.DPAD_CENTER) },
        enabled = controlsEnabled,
    )

    Spacer(Modifier.height(20.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        NavItem(Icons.AutoMirrored.Filled.ArrowBack, "Back", controlsEnabled) { onKey(AndroidTvKey.BACK) }
        NavItem(Icons.Filled.Home, "Home", controlsEnabled) { onKey(AndroidTvKey.HOME) }
        NavItem(Icons.Filled.Apps, "Apps", controlsEnabled) { onKey(AndroidTvKey.APP_SWITCH) }
    }

    Spacer(Modifier.height(22.dp))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RockerColumn("Volume", controlsEnabled, { onKey(AndroidTvKey.VOLUME_UP) }, { onKey(AndroidTvKey.VOLUME_DOWN) })
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (controlsEnabled) 1f else 0.35f))
                .clickable(enabled = controlsEnabled) { onKey(AndroidTvKey.VOLUME_MUTE) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Mute", modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Still reaches the TV, via the Mecool's CEC passthrough",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(160.dp),
        )
    }

    Spacer(Modifier.height(18.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        val alpha = if (controlsEnabled) 1f else 0.35f
        TransportButton(Icons.Filled.FastRewind, "Rewind", controlsEnabled, alpha) { onKey(AndroidTvKey.MEDIA_REWIND) }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                .clickable(enabled = controlsEnabled) { onKey(AndroidTvKey.MEDIA_PLAY_PAUSE) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play/Pause", tint = MaterialTheme.colorScheme.onPrimary)
        }
        TransportButton(Icons.Filled.FastForward, "Fast forward", controlsEnabled, alpha) { onKey(AndroidTvKey.MEDIA_FAST_FORWARD) }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun NavItem(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
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
private fun TransportButton(icon: ImageVector, label: String, enabled: Boolean, alpha: Float, onClick: () -> Unit) {
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
