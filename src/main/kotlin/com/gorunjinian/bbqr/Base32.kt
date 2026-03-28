package com.gorunjinian.bbqr

/**
 * RFC 4648 Base32 encoding/decoding without padding.
 */
internal object Base32 {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val DECODE_TABLE = IntArray(128) { -1 }.also { table ->
        ALPHABET.forEachIndexed { i, c -> table[c.code] = i }
    }

    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val result = StringBuilder(data.size * 8 / 5 + 1)
        var buffer = 0
        var bitsLeft = 0
        for (byte in data) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                result.append(ALPHABET[(buffer shr bitsLeft) and 0x1F])
            }
        }
        if (bitsLeft > 0) {
            result.append(ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return result.toString()
    }

    fun decode(str: String): ByteArray {
        if (str.isEmpty()) return ByteArray(0)
        val output = ByteArray(str.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        var index = 0
        for (c in str) {
            val value = if (c.code < 128) DECODE_TABLE[c.code] else -1
            if (value < 0) throw IllegalArgumentException("Invalid Base32 character: $c")
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output[index++] = ((buffer shr bitsLeft) and 0xFF).toByte()
            }
        }
        return output.copyOf(index)
    }
}
