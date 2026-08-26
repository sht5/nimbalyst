package com.tvremote.samsung.data

/**
 * Subset of Android's RemoteKeyCode enum (remotemessage.proto, part of the Android TV Remote
 * protocol) exposed by the Mecool control screen. Values are Android's own standard key codes
 * (see android.view.KeyEvent) — richer and more standardized than Samsung's ad-hoc KEY_* strings.
 */
enum class AndroidTvKey(val code: Int) {
    DPAD_UP(19),
    DPAD_DOWN(20),
    DPAD_LEFT(21),
    DPAD_RIGHT(22),
    DPAD_CENTER(23),
    BACK(4),
    HOME(3),
    APP_SWITCH(187),
    VOLUME_UP(24),
    VOLUME_DOWN(25),
    VOLUME_MUTE(164),
    POWER(26),
    SEARCH(84),
    MEDIA_PLAY_PAUSE(85),
    MEDIA_REWIND(89),
    MEDIA_FAST_FORWARD(90),
}
