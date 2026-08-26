package com.tvremote.samsung.network.androidtv

/** Which optional field the TV's reply to a polo.proto `OuterMessage` had set. */
sealed interface PoloIncoming {
    data object PairingRequestAck : PoloIncoming
    data object Options : PoloIncoming
    data object ConfigurationAck : PoloIncoming
    data object SecretAck : PoloIncoming
    data class StatusError(val status: Long) : PoloIncoming
    data object Unknown : PoloIncoming
}

/**
 * Builds and parses the pairing handshake messages (polo.proto `OuterMessage`, port 6467).
 * Field numbers are from polo.proto: protocol_version=1, status=2, pairing_request=10,
 * pairing_request_ack=11, options=20, configuration=30, configuration_ack=31, secret=40,
 * secret_ack=41. We don't need the *contents* of the TV's replies (server_name, its own
 * encoding list) — only which field is present, same as the reference client's state machine.
 */
object PoloMessages {
    private const val STATUS_OK = 200L
    private const val ENCODING_TYPE_HEXADECIMAL = 3
    private const val PIN_SYMBOL_LENGTH = 6
    private const val ROLE_TYPE_INPUT = 1

    private fun header(): ByteArray = Proto.varintField(1, 2L) + Proto.varintField(2, STATUS_OK)

    fun pairingRequest(clientName: String): ByteArray {
        val request = Proto.stringField(1, "atvremote") + Proto.stringField(2, clientName)
        return header() + Proto.messageField(10, request)
    }

    fun optionsResponse(): ByteArray {
        val encoding = Proto.varintField(1, ENCODING_TYPE_HEXADECIMAL) + Proto.varintField(2, PIN_SYMBOL_LENGTH)
        val options = Proto.messageField(1, encoding) + Proto.varintField(3, ROLE_TYPE_INPUT)
        return header() + Proto.messageField(20, options)
    }

    fun configurationResponse(): ByteArray {
        val encoding = Proto.varintField(1, ENCODING_TYPE_HEXADECIMAL) + Proto.varintField(2, PIN_SYMBOL_LENGTH)
        val configuration = Proto.messageField(1, encoding) + Proto.varintField(2, ROLE_TYPE_INPUT)
        return header() + Proto.messageField(30, configuration)
    }

    fun secret(secret: ByteArray): ByteArray {
        val secretMsg = Proto.bytesField(1, secret)
        return header() + Proto.messageField(40, secretMsg)
    }

    fun parse(bytes: ByteArray): PoloIncoming {
        val reader = Proto.Reader(bytes)
        var status = STATUS_OK
        var which: Int? = null
        while (reader.hasNext()) {
            val (fieldNumber, wireType) = reader.readTag()
            when (fieldNumber) {
                2 -> status = reader.readVarint()
                11, 20, 31, 41 -> {
                    reader.skip(wireType)
                    which = fieldNumber
                }
                else -> reader.skip(wireType)
            }
        }
        if (status != STATUS_OK) return PoloIncoming.StatusError(status)
        return when (which) {
            11 -> PoloIncoming.PairingRequestAck
            20 -> PoloIncoming.Options
            31 -> PoloIncoming.ConfigurationAck
            41 -> PoloIncoming.SecretAck
            else -> PoloIncoming.Unknown
        }
    }
}
