package com.tvremote.samsung.network.androidtv

/** Which field a `RemoteMessage` from the TV had set, with just the payload our client acts on. */
sealed interface RemoteIncoming {
    data class Configure(val code1: Int) : RemoteIncoming
    data object SetActive : RemoteIncoming
    data class PingRequest(val val1: Int) : RemoteIncoming
    data class Start(val started: Boolean) : RemoteIncoming
    data object Unknown : RemoteIncoming
}

/**
 * Builds and parses control-channel messages (remotemessage.proto `RemoteMessage`, port 6466,
 * sent after pairing). Field numbers are from remotemessage.proto: remote_configure=1,
 * remote_set_active=2, remote_ping_request=8, remote_ping_response=9, remote_key_inject=10.
 * `RemoteDirection.SHORT=3` for a normal button press (vs a press-and-hold).
 */
object RemoteMessages {
    private const val DIRECTION_SHORT = 3

    /** Ping(2^0) | Key(2^1) | Power(2^5) | Volume(2^6) — the only features this app uses. */
    const val SUPPORTED_FEATURES = (1 shl 0) or (1 shl 1) or (1 shl 5) or (1 shl 6)

    fun configureResponse(code1: Int): ByteArray {
        val deviceInfo = Proto.varintField(3, 1) +
            Proto.stringField(4, "1") +
            Proto.stringField(5, "com.tvremote.samsung") +
            Proto.stringField(6, "1.0.0")
        val configure = Proto.varintField(1, code1) + Proto.messageField(2, deviceInfo)
        return Proto.messageField(1, configure)
    }

    fun setActiveResponse(active: Int): ByteArray = Proto.messageField(2, Proto.varintField(1, active))

    fun pingResponse(val1: Int): ByteArray = Proto.messageField(9, Proto.varintField(1, val1))

    fun keyInject(keyCode: Int, direction: Int = DIRECTION_SHORT): ByteArray {
        val inject = Proto.varintField(1, keyCode) + Proto.varintField(2, direction)
        return Proto.messageField(10, inject)
    }

    fun parse(bytes: ByteArray): RemoteIncoming {
        val reader = Proto.Reader(bytes)
        while (reader.hasNext()) {
            val (fieldNumber, wireType) = reader.readTag()
            if (wireType != Proto.WIRE_LENGTH_DELIMITED) {
                reader.skip(wireType)
                continue
            }
            when (fieldNumber) {
                1 -> return RemoteIncoming.Configure(readInt32Field(reader.readLengthDelimited(), 1) ?: 0)
                2 -> {
                    reader.readLengthDelimited()
                    return RemoteIncoming.SetActive
                }
                8 -> return RemoteIncoming.PingRequest(readInt32Field(reader.readLengthDelimited(), 1) ?: 0)
                40 -> return RemoteIncoming.Start((readInt32Field(reader.readLengthDelimited(), 1) ?: 0) != 0)
                else -> reader.readLengthDelimited()
            }
        }
        return RemoteIncoming.Unknown
    }

    private fun readInt32Field(bytes: ByteArray, targetField: Int): Int? {
        val reader = Proto.Reader(bytes)
        while (reader.hasNext()) {
            val (fieldNumber, wireType) = reader.readTag()
            if (fieldNumber == targetField && wireType == Proto.WIRE_VARINT) return reader.readVarint().toInt()
            reader.skip(wireType)
        }
        return null
    }
}
