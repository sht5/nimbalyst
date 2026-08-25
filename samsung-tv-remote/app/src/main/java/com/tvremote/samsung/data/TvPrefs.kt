package com.tvremote.samsung.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Remembers the last TV a user paired with: its IP and the pairing token the
 * TV hands back after the on-screen "Allow" prompt is accepted. Sending the
 * saved token on future connections lets us skip that prompt.
 */
class TvPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("tv_prefs", Context.MODE_PRIVATE)

    var lastIp: String?
        get() = prefs.getString(KEY_IP, null)
        set(value) = prefs.edit().putString(KEY_IP, value).apply()

    var lastName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    fun tokenFor(ip: String): String? = prefs.getString(tokenKey(ip), null)

    fun saveToken(ip: String, token: String) {
        prefs.edit().putString(tokenKey(ip), token).apply()
    }

    fun clearToken(ip: String) {
        prefs.edit().remove(tokenKey(ip)).apply()
    }

    /**
     * The TV's network MAC address, learned from its local device-info
     * endpoint the first time we connect. Wake-on-LAN needs this to turn the
     * TV back on once it's fully off — the remote-control websocket has
     * nothing to talk to at that point, only the TV's network card does.
     */
    fun macFor(ip: String): String? = prefs.getString(macKey(ip), null)

    fun saveMac(ip: String, mac: String) {
        prefs.edit().putString(macKey(ip), mac).apply()
    }

    private fun tokenKey(ip: String) = "$KEY_TOKEN_PREFIX$ip"
    private fun macKey(ip: String) = "$KEY_MAC_PREFIX$ip"

    private companion object {
        const val KEY_IP = "last_ip"
        const val KEY_NAME = "last_name"
        const val KEY_TOKEN_PREFIX = "token_"
        const val KEY_MAC_PREFIX = "mac_"
    }
}
