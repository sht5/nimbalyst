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
import com.tvremote.samsung.data.DeviceKind
import com.tvremote.samsung.network.TvConnectionState
import com.tvremote.samsung.network.androidtv.AndroidTvConnectionState
import com.tvremote.samsung.ui.AndroidTvConnectScreen
import com.tvremote.samsung.ui.ConnectScreen
import com.tvremote.samsung.ui.RemoteScreen

private object Routes {
    const val CONNECT = "connect"
    const val REMOTE = "remote"
    const val ADD_ANDROID_TV = "add_android_tv"
}

@Composable
fun RemoteApp(viewModel: TvRemoteViewModel = viewModel()) {
    val navController = rememberNavController()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val androidTvState by viewModel.androidTvState.collectAsStateWithLifecycle()
    val activeDevice by viewModel.activeDevice.collectAsStateWithLifecycle()

    // Any connection attempt moves to the remote screen and keeps it there — including
    // Disconnected/Error, so losing the TV never strands the user on IP entry with no way
    // to power it back on. Only an explicit "Switch TV" (which resets to Idle) leaves it.
    LaunchedEffect(connectionState) {
        navigateForTvState(navController, connectionState)
    }

    // First-time Mecool pairing finishing successfully hands control back to the remote
    // screen automatically, with the streamer now the active device.
    LaunchedEffect(androidTvState) {
        if (androidTvState is AndroidTvConnectionState.Connected &&
            navController.currentDestination?.route == Routes.ADD_ANDROID_TV
        ) {
            viewModel.selectDevice(DeviceKind.ANDROID_TV)
            navController.popBackStack(Routes.REMOTE, inclusive = false)
        }
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
                androidTvState = androidTvState,
                activeDevice = activeDevice,
                canWake = viewModel.canWake(),
                tvName = viewModel.prefs.lastName ?: viewModel.connectedIp ?: viewModel.prefs.lastIp ?: "TV",
                androidTvName = viewModel.androidTvPrefs.lastName ?: "Mecool",
                onKey = viewModel::sendKey,
                onAndroidTvKey = viewModel::sendAndroidTvKey,
                onPowerTap = viewModel::onPowerTap,
                onSwitchTv = { viewModel.disconnect() },
                onSelectDevice = { kind ->
                    if (kind == DeviceKind.ANDROID_TV && !viewModel.hasPairedAndroidTv()) {
                        navController.navigate(Routes.ADD_ANDROID_TV)
                    } else {
                        viewModel.selectDevice(kind)
                    }
                },
            )
        }
        composable(Routes.ADD_ANDROID_TV) {
            AndroidTvConnectScreen(
                initialIp = viewModel.androidTvPrefs.lastIp.orEmpty(),
                isDiscovering = viewModel.isDiscoveringAndroidTv.collectAsStateWithLifecycle().value,
                discoveredTvs = viewModel.discoveredAndroidTvs.collectAsStateWithLifecycle().value,
                state = androidTvState,
                onDiscover = viewModel::discoverAndroidTv,
                onConnect = viewModel::connectAndroidTv,
                onSubmitCode = viewModel::submitAndroidTvPairingCode,
            )
        }
    }
}

private fun navigateForTvState(navController: NavHostController, state: TvConnectionState) {
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
