package com.tvremote.samsung.data

/**
 * Key codes accepted by the Samsung "ms.remote.control" websocket channel.
 * This is the same key set the SmartThings app and every third-party Samsung
 * remote implementation use; Samsung does not publish an official list.
 */
enum class RemoteKey(val code: String) {
    POWER("KEY_POWER"),
    VOLUME_UP("KEY_VOLUP"),
    VOLUME_DOWN("KEY_VOLDOWN"),
    MUTE("KEY_MUTE"),
    CHANNEL_UP("KEY_CHUP"),
    CHANNEL_DOWN("KEY_CHDOWN"),
    UP("KEY_UP"),
    DOWN("KEY_DOWN"),
    LEFT("KEY_LEFT"),
    RIGHT("KEY_RIGHT"),
    ENTER("KEY_ENTER"),
    BACK("KEY_RETURN"),
    HOME("KEY_HOME"),
    MENU("KEY_MENU"),
    SOURCE("KEY_SOURCE"),
    GUIDE("KEY_GUIDE"),
    INFO("KEY_INFO"),
    EXIT("KEY_EXIT"),
    PLAY("KEY_PLAY"),
    PAUSE("KEY_PAUSE"),
    STOP("KEY_STOP"),
    REWIND("KEY_REWIND"),
    FAST_FORWARD("KEY_FF"),
    NUM_0("KEY_0"),
    NUM_1("KEY_1"),
    NUM_2("KEY_2"),
    NUM_3("KEY_3"),
    NUM_4("KEY_4"),
    NUM_5("KEY_5"),
    NUM_6("KEY_6"),
    NUM_7("KEY_7"),
    NUM_8("KEY_8"),
    NUM_9("KEY_9"),
}
