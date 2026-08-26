package com.tvremote.samsung.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Remembers the last-paired Android TV streamer (e.g. a Mecool box) — separate storage from
 * [TvPrefs] since it's a different device with a different pairing model. There's no bearer
 * token to save here: the client's identity is the self-signed certificate held in the Android
 * Keystore (see AndroidTvCertificate), which the streamer itself remembers after pairing. This
 * just tracks which IP we've completed that handshake with, so we know whether to pair again.
 */
class AndroidTvPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("androidtv_prefs", Context.MODE_PRIVATE)

    var lastIp: String?
        get() = prefs.getString(KEY_IP, null)
        set(value) = prefs.edit().putString(KEY_IP, value).apply()

    var lastName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    fun isPaired(ip: String): Boolean = prefs.getBoolean(pairedKey(ip), false)

    fun markPaired(ip: String) {
        prefs.edit().putBoolean(pairedKey(ip), true).apply()
    }

    private fun pairedKey(ip: String) = "$KEY_PAIRED_PREFIX$ip"

    private companion object {
        const val KEY_IP = "last_ip"
        const val KEY_NAME = "last_name"
        const val KEY_PAIRED_PREFIX = "paired_"
    }
}
