package com.tvremote.samsung

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tvremote.samsung.network.TvConnectionState
import com.tvremote.samsung.ui.ConnectScreen
import com.tvremote.samsung.ui.RemoteScreen

private object Routes {
    const val CONNECT = "connect"
    const val REMOTE = "remote"
}

@Composable
fun RemoteApp(viewModel: TvRemoteViewModel = viewModel()) {
    val navController = rememberNavController()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    // Any connection attempt moves to the remote screen and keeps it there — including
    // Disconnected/Error, so losing the TV never strands the user on IP entry with no way
    // to power it back on. Only an explicit "Switch TV" (which resets to Idle) leaves it.
    LaunchedEffect(connectionState) {
        navigateForState(navController, connectionState)
    }

    NavHost(navController = navController, startDestination = Routes.CONNECT) {
        composable(Routes.CONNECT) {
            val isDiscovering by viewModel.isDiscovering.collectAsStateWithLifecycle()
            val discoveredTvs by viewModel.discoveredTvs.collectAsStateWithLifecycle()
            ConnectScreen(
                initialIp = viewModel.prefs.lastIp.orEmpty(),
                isDiscovering = isDiscovering,
                discoveredTvs = discoveredTvs,
                onDiscover = viewModel::discover,
                onConnect = viewModel::connect,
            )
        }
        composable(Routes.REMOTE) {
            RemoteScreen(
                connectionState = connectionState,
                canWake = viewModel.canWake(),
                tvName = viewModel.prefs.lastName ?: viewModel.connectedIp ?: viewModel.prefs.lastIp ?: "TV",
                onKey = viewModel::sendKey,
                onPowerTap = viewModel::onPowerTap,
                onSwitchTv = {
                    viewModel.disconnect()
                },
            )
        }
    }
}

private fun navigateForState(navController: NavHostController, state: TvConnectionState) {
    val onRemoteScreen = navController.currentDestination?.route == Routes.REMOTE
    when (state) {
        TvConnectionState.Idle -> {
            // Only a deliberate "Switch TV" resets to Idle — safe to leave the remote screen.
            if (onRemoteScreen) {
                navController.popBackStack(Routes.CONNECT, inclusive = false)
            }
        }
        else -> {
            // Connecting, AwaitingPairing, Connected, Disconnected, WakingUp, and Error all
            // stay on (or arrive at) the remote screen. Losing the TV mid-session — the whole
            // point of this screen — must never bounce the user back to IP entry: Power (and
            // the Wake-on-LAN retry it triggers) only lives here.
            if (!onRemoteScreen) {
                navController.navigate(Routes.REMOTE)
            }
        }
    }
}
