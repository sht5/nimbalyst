package com.tvremote.samsung.network.androidtv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "AndroidTvDiscovery"
private const val SERVICE_TYPE = "_androidtvremote2._tcp."

data class DiscoveredAndroidTv(val ip: String, val label: String)

/**
 * Finds Android TV / Google TV devices on the local network via mDNS — the service
 * (`_androidtvremote2._tcp`) the official Android TV Remote Control and Google Home apps use to
 * find devices to pair with. Unlike the Samsung SSDP scan, this goes through Android's built-in
 * NsdManager rather than a hand-rolled UDP socket: mDNS response parsing (compressed name
 * pointers, multiple record types) is considerably more involved than SSDP's plain HTTP-like
 * text replies, and the platform already does it correctly.
 */
object AndroidTvDiscovery {

    suspend fun discover(context: Context, timeoutMs: Long = 4000): List<DiscoveredAndroidTv> {
        val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        val found = LinkedHashMap<String, DiscoveredAndroidTv>()

        withTimeoutOrNull(timeoutMs) {
            // Runs discovery for the full timeout window, collecting into `found` as results
            // arrive; this suspend point only ever completes via the timeout cancelling it
            // (or a start failure), same "scan for N seconds, return whatever turned up" shape
            // as the Samsung SSDP scan.
            suspendCancellableCoroutine<Unit> { cont ->
                val discoveryListener = object : NsdManager.DiscoveryListener {
                    override fun onDiscoveryStarted(serviceType: String) = Unit
                    override fun onServiceFound(service: NsdServiceInfo) {
                        resolve(nsdManager, service) { ip, label -> found[ip] = DiscoveredAndroidTv(ip, label) }
                    }
                    override fun onServiceLost(service: NsdServiceInfo) = Unit
                    override fun onDiscoveryStopped(serviceType: String) = Unit
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                        Log.w(TAG, "Discovery start failed: $errorCode")
                        if (cont.isActive) cont.resumeWith(Result.success(Unit))
                    }
                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
                }
                cont.invokeOnCancellation {
                    runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
                }
                nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            }
        }
        return found.values.toList()
    }

    private fun resolve(nsdManager: NsdManager, service: NsdServiceInfo, onResolved: (String, String) -> Unit) {
        nsdManager.resolveService(
            service,
            object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                }
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val ip = serviceInfo.host?.hostAddress ?: return
                    onResolved(ip, serviceInfo.serviceName ?: "Android TV")
                }
            },
        )
    }
}
