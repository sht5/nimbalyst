package com.tvremote.samsung.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

private const val TAG = "TvDiscovery"
private const val SSDP_ADDRESS = "239.255.255.250"
private const val SSDP_PORT = 1900

data class DiscoveredTv(val ip: String, val label: String)

/**
 * Finds Samsung TVs on the local Wi-Fi network via SSDP (UPnP discovery),
 * the same broadcast every DLNA/UPnP device answers. Devices reply directly
 * (unicast) to whoever sent the search, so no multicast group join is
 * required to receive answers — only to send the initial query.
 */
object TvDiscovery {

    suspend fun discover(context: Context, timeoutMs: Int = 3000): List<DiscoveredTv> =
        withContext(Dispatchers.IO) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val multicastLock = wifiManager?.createMulticastLock("tvremote-ssdp")?.apply { setReferenceCounted(true) }
            multicastLock?.acquire()

            val found = LinkedHashMap<String, DiscoveredTv>()
            try {
                DatagramSocket().use { socket ->
                    socket.soTimeout = timeoutMs
                    socket.broadcast = true

                    val query = buildString {
                        append("M-SEARCH * HTTP/1.1\r\n")
                        append("HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n")
                        append("MAN: \"ssdp:discover\"\r\n")
                        append("MX: 2\r\n")
                        append("ST: ssdp:all\r\n")
                        append("\r\n")
                    }.toByteArray()

                    val target = InetSocketAddress(InetAddress.getByName(SSDP_ADDRESS), SSDP_PORT)
                    socket.send(DatagramPacket(query, query.size, target))

                    val deadline = System.currentTimeMillis() + timeoutMs
                    val buffer = ByteArray(2048)
                    while (System.currentTimeMillis() < deadline) {
                        val remaining = (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(1)
                        socket.soTimeout = remaining
                        val packet = DatagramPacket(buffer, buffer.size)
                        try {
                            socket.receive(packet)
                        } catch (timeout: java.net.SocketTimeoutException) {
                            break
                        }
                        val response = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val headers = parseHeaders(response)
                        val server = headers["server"].orEmpty()
                        val location = headers["location"].orEmpty()
                        val looksLikeSamsung = server.contains("samsung", ignoreCase = true) ||
                            location.contains("samsung", ignoreCase = true)
                        if (looksLikeSamsung) {
                            val ip = packet.address.hostAddress ?: continue
                            found[ip] = DiscoveredTv(ip = ip, label = server.ifBlank { "Samsung TV" })
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "SSDP discovery failed: ${e.message}")
            } finally {
                multicastLock?.release()
            }
            found.values.toList()
        }

    private fun parseHeaders(raw: String): Map<String, String> =
        raw.lineSequence()
            .drop(1) // status line
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) null else line.substring(0, idx).trim().lowercase() to line.substring(idx + 1).trim()
            }
            .toMap()
}
