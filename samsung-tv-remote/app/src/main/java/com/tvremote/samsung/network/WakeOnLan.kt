package com.tvremote.samsung.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

private const val MAGIC_PACKET_PORT = 9
private const val BROADCAST_ADDRESS = "255.255.255.255"

/**
 * Sends a Wake-on-LAN "magic packet" to power a Samsung TV back on.
 *
 * This is the only channel available to us once the TV is fully off: the
 * remote-control websocket (SamsungTvClient) is served by Tizen itself,
 * which isn't running at all in that state. Samsung TVs instead keep their
 * network interface listening in standby when "Power on with mobile" is
 * enabled (Settings > General > Network > Expert Settings on most models) —
 * a magic packet addressed to that interface's MAC wakes it, the same
 * mechanism SmartThings uses.
 *
 * Blocking (opens a UDP socket) — call from a background dispatcher.
 */
object WakeOnLan {

    fun send(mac: String) {
        val macBytes = parseMac(mac)
        val packet = ByteArray(6 + 16 * macBytes.size).apply {
            for (i in 0 until 6) this[i] = 0xFF.toByte()
            for (block in 0 until 16) {
                System.arraycopy(macBytes, 0, this, 6 + block * macBytes.size, macBytes.size)
            }
        }
        DatagramSocket().use { socket ->
            socket.broadcast = true
            val address = InetAddress.getByName(BROADCAST_ADDRESS)
            socket.send(DatagramPacket(packet, packet.size, address, MAGIC_PACKET_PORT))
        }
    }

    private fun parseMac(mac: String): ByteArray {
        val parts = mac.split(":", "-")
        require(parts.size == 6) { "Malformed MAC address: $mac" }
        return ByteArray(6) { parts[it].toInt(16).toByte() }
    }
}
