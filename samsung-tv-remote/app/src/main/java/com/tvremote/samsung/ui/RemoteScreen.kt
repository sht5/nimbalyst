package com.tvremote.samsung.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tvremote.samsung.data.RemoteKey
import com.tvremote.samsung.network.TvConnectionState
import com.tvremote.samsung.ui.components.DPad

@Composable
fun RemoteScreen(
    connectionState: TvConnectionState,
    onKey: (RemoteKey) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        StatusRow(connectionState, onDisconnect)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onKey(RemoteKey.POWER) }) {
                Icon(Icons.Filled.Power, contentDescription = "Power", tint = Color(0xFFE0525C))
            }
            IconButton(onClick = { onKey(RemoteKey.SOURCE) }) {
                Text("Source", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(24.dp))

        DPad(
            onUp = { onKey(RemoteKey.UP) },
            onDown = { onKey(RemoteKey.DOWN) },
            onLeft = { onKey(RemoteKey.LEFT) },
            onRight = { onKey(RemoteKey.RIGHT) },
            onEnter = { onKey(RemoteKey.ENTER) },
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = { onKey(RemoteKey.BACK) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            IconButton(onClick = { onKey(RemoteKey.HOME) }) {
                Icon(Icons.Filled.Home, contentDescription = "Home")
            }
            IconButton(onClick = { onKey(RemoteKey.MENU) }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            VolumeChannelColumn(
                label = "Vol",
                onUp = { onKey(RemoteKey.VOLUME_UP) },
                onDown = { onKey(RemoteKey.VOLUME_DOWN) },
            )
            IconButton(onClick = { onKey(RemoteKey.MUTE) }) {
                Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Mute")
            }
            VolumeChannelColumn(
                label = "Ch",
                onUp = { onKey(RemoteKey.CHANNEL_UP) },
                onDown = { onKey(RemoteKey.CHANNEL_DOWN) },
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        NumberPad(onKey)
    }
}

@Composable
private fun StatusRow(connectionState: TvConnectionState, onDisconnect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = statusLabel(connectionState),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onDisconnect) {
            Text("Disconnect")
        }
    }
}

private fun statusLabel(state: TvConnectionState): String = when (state) {
    TvConnectionState.Idle -> "Not connected"
    TvConnectionState.Connecting -> "Connecting…"
    TvConnectionState.AwaitingPairing -> "Accept the prompt on your TV"
    TvConnectionState.Connected -> "Connected"
    TvConnectionState.Disconnected -> "Disconnected"
    is TvConnectionState.Error -> "Error: ${state.message}"
}

@Composable
private fun VolumeChannelColumn(label: String, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        OutlinedButton(onClick = onUp) { Text("+") }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = onDown) { Text("−") }
    }
}

private val numberKeys = listOf(
    RemoteKey.NUM_1, RemoteKey.NUM_2, RemoteKey.NUM_3,
    RemoteKey.NUM_4, RemoteKey.NUM_5, RemoteKey.NUM_6,
    RemoteKey.NUM_7, RemoteKey.NUM_8, RemoteKey.NUM_9,
    RemoteKey.NUM_0,
)

@Composable
private fun NumberPad(onKey: (RemoteKey) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(numberKeys) { key ->
            FilledTonalButton(
                onClick = { onKey(key) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(key.code.removePrefix("KEY_"), textAlign = TextAlign.Center)
            }
        }
    }
}
