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

    // Once a connection attempt resolves to Connected, move to the remote screen.
    // Falling back to Idle/Disconnected/Error sends the user back to reconnect.
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
                onKey = viewModel::sendKey,
                onDisconnect = {
                    viewModel.disconnect()
                },
            )
        }
    }
}

private fun navigateForState(navController: NavHostController, state: TvConnectionState) {
    val onRemoteScreen = navController.currentDestination?.route == Routes.REMOTE
    when (state) {
        TvConnectionState.Connecting, TvConnectionState.AwaitingPairing, TvConnectionState.Connected -> {
            if (!onRemoteScreen) {
                navController.navigate(Routes.REMOTE)
            }
        }
        TvConnectionState.Idle, TvConnectionState.Disconnected, is TvConnectionState.Error -> {
            if (onRemoteScreen && state !is TvConnectionState.Error) {
                navController.popBackStack(Routes.CONNECT, inclusive = false)
            }
            // On Error we stay on the remote screen so the message is visible;
            // the user can tap Disconnect to go back and retry.
        }
    }
}
