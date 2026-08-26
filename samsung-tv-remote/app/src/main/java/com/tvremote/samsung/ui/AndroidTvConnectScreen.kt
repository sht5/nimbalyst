package com.tvremote.samsung.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tvremote.samsung.network.androidtv.AndroidTvConnectionState
import com.tvremote.samsung.network.androidtv.DiscoveredAndroidTv

@Composable
fun AndroidTvConnectScreen(
    initialIp: String,
    isDiscovering: Boolean,
    discoveredTvs: List<DiscoveredAndroidTv>,
    state: AndroidTvConnectionState,
    onDiscover: () -> Unit,
    onConnect: (String) -> Unit,
    onSubmitCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var ip by remember { mutableStateOf(initialIp) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Pair your Mecool", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Enter the streamer's IP address, or discover it on your Wi-Fi network. " +
                "The TV will show a 6-digit code — type it in here once it appears.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("Streamer IP address") },
            placeholder = { Text("192.168.1.55") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onConnect(ip) }, modifier = Modifier.weight(1f)) {
                Text("Pair")
            }
            OutlinedButton(onClick = onDiscover, modifier = Modifier.weight(1f)) {
                Text(if (isDiscovering) "Searching…" else "Discover devices")
            }
        }

        if (state is AndroidTvConnectionState.Error) {
            Text(text = state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (isDiscovering) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                Text("Looking for Android TV devices on your network…")
            }
        } else if (discoveredTvs.isNotEmpty()) {
            Text(text = "Found on your network", style = MaterialTheme.typography.titleSmall)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(discoveredTvs) { device ->
                    Card {
                        ListItem(
                            headlineContent = { Text(device.label) },
                            supportingContent = { Text(device.ip) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            OutlinedButton(onClick = { ip = device.ip; onConnect(device.ip) }) {
                                Text("Pair")
                            }
                        }
                    }
                }
            }
        }
    }

    if (state is AndroidTvConnectionState.AwaitingPairingCode) {
        PairingCodeDialog(onSubmit = onSubmitCode)
    }
}

@Composable
private fun PairingCodeDialog(onSubmit: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Enter the code shown on your TV") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it.uppercase() },
                label = { Text("6-digit code") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(code) }, enabled = code.length == 6) {
                Text("Confirm")
            }
        },
    )
}
