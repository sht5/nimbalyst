package com.tvremote.samsung.network.androidtv

/**
 * Minimal hand-rolled protobuf wire-format reader/writer for the Android TV Remote protocol's
 * two message families (polo.proto pairing messages, remotemessage.proto control messages).
 * We only ever touch a couple dozen fields across both, so this avoids pulling in the full
 * protobuf compiler/runtime for them — same philosophy as hand-rolling SSDP and Wake-on-LAN
 * elsewhere in this app rather than adding a library for one narrow use.
 *
 * Field numbers used against this are taken directly from the reference .proto files
 * (tronikos/androidtvremote2 on GitHub) — getting one wrong silently breaks the handshake with
 * no useful error from the TV side, so don't hand-tune them without checking that source.
 */
object Proto {
    const val WIRE_VARINT = 0
    const val WIRE_LENGTH_DELIMITED = 2

    fun encodeVarint(value: Long): ByteArray {
        var v = value
        val out = ArrayList<Byte>()
        do {
            var b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v != 0L) b = b or 0x80
            out.add(b.toByte())
        } while (v != 0L)
        return out.toByteArray()
    }

    private fun tag(fieldNumber: Int, wireType: Int): ByteArray =
        encodeVarint((fieldNumber.toLong() shl 3) or wireType.toLong())

    fun varintField(fieldNumber: Int, value: Long): ByteArray = tag(fieldNumber, WIRE_VARINT) + encodeVarint(value)
    fun varintField(fieldNumber: Int, value: Int): ByteArray = varintField(fieldNumber, value.toLong())

    fun bytesField(fieldNumber: Int, value: ByteArray): ByteArray =
        tag(fieldNumber, WIRE_LENGTH_DELIMITED) + encodeVarint(value.size.toLong()) + value

    fun stringField(fieldNumber: Int, value: String): ByteArray = bytesField(fieldNumber, value.toByteArray(Charsets.UTF_8))

    /** A nested message is itself just a length-delimited field wrapping the submessage's bytes. */
    fun messageField(fieldNumber: Int, value: ByteArray): ByteArray = bytesField(fieldNumber, value)

    /** Sequentially reads fields out of a raw protobuf message body. */
    class Reader(private val data: ByteArray) {
        private var pos = 0

        fun hasNext(): Boolean = pos < data.size

        /** @return (fieldNumber, wireType) */
        fun readTag(): Pair<Int, Int> {
            val t = readVarint()
            return Pair((t ushr 3).toInt(), (t and 0x7L).toInt())
        }

        fun readVarint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                val b = data[pos].toInt() and 0xFF
                pos++
                result = result or ((b.toLong() and 0x7F) shl shift)
                if (b and 0x80 == 0) break
                shift += 7
            }
            return result
        }

        fun readLengthDelimited(): ByteArray {
            val len = readVarint().toInt()
            val out = data.copyOfRange(pos, pos + len)
            pos += len
            return out
        }

        fun skip(wireType: Int) {
            when (wireType) {
                WIRE_VARINT -> readVarint()
                WIRE_LENGTH_DELIMITED -> readLengthDelimited()
                else -> error("Unsupported wire type $wireType")
            }
        }
    }
}
