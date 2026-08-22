package com.tvremote.samsung

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvremote.samsung.data.RemoteKey
import com.tvremote.samsung.data.TvPrefs
import com.tvremote.samsung.network.DiscoveredTv
import com.tvremote.samsung.network.SamsungTvClient
import com.tvremote.samsung.network.TvConnectionState
import com.tvremote.samsung.network.TvDiscovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TvRemoteViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = TvPrefs(application)
    private val client = SamsungTvClient(prefs)

    val connectionState: StateFlow<TvConnectionState> = client.state

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    var connectedIp: String? = null
        private set

    fun connect(ip: String) {
        val trimmed = ip.trim()
        if (trimmed.isEmpty()) return
        connectedIp = trimmed
        prefs.lastIp = trimmed
        client.connect(trimmed)
    }

    fun sendKey(key: RemoteKey) = client.sendKey(key)

    fun disconnect() {
        client.disconnect()
        connectedIp = null
    }

    fun discover() {
        if (_isDiscovering.value) return
        viewModelScope.launch {
            _isDiscovering.value = true
            _discoveredTvs.value = emptyList()
            _discoveredTvs.value = TvDiscovery.discover(getApplication())
            _isDiscovering.value = false
        }
    }

    override fun onCleared() {
        client.disconnect()
        super.onCleared()
    }
}
