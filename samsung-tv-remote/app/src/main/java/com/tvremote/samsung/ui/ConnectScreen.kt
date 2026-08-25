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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tvremote.samsung.network.DiscoveredTv

@Composable
fun ConnectScreen(
    initialIp: String,
    isDiscovering: Boolean,
    discoveredTvs: List<DiscoveredTv>,
    onDiscover: () -> Unit,
    onConnect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var ip by remember { mutableStateOf(initialIp) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Connect to your TV",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Enter your TV's IP address, or discover it on your Wi-Fi network. " +
                "The first connection shows an Allow prompt on the TV itself — accept it there.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("TV IP address") },
            placeholder = { Text("192.168.1.42") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onConnect(ip) }, modifier = Modifier.weight(1f)) {
                Text("Connect")
            }
            OutlinedButton(onClick = onDiscover, modifier = Modifier.weight(1f)) {
                Text(if (isDiscovering) "Searching…" else "Discover TVs")
            }
        }

        if (isDiscovering) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                Text("Looking for Samsung TVs on your network…")
            }
        } else if (discoveredTvs.isNotEmpty()) {
            Text(
                text = "Found on your network",
                style = MaterialTheme.typography.titleSmall,
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(discoveredTvs) { tv ->
                    Card {
                        ListItem(
                            headlineContent = { Text(tv.label) },
                            supportingContent = { Text(tv.ip) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            OutlinedButton(onClick = { ip = tv.ip; onConnect(tv.ip) }) {
                                Text("Connect")
                            }
                        }
                    }
                }
            }
        }
    }
}
