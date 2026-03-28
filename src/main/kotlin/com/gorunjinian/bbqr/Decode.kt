package com.gorunjinian.bbqr

import com.jcraft.jzlib.Inflater
import com.jcraft.jzlib.JZlib
import java.io.ByteArrayOutputStream

internal object Decode {

    fun decodeOrderedParts(parts: List<String>, encoding: Encoding): ByteArray = when (encoding) {
        Encoding.Hex -> decodeHexParts(parts)
        Encoding.Base32 -> decodeBase32Parts(parts)
        Encoding.Zlib -> {
            val bytes = decodeBase32Parts(parts)
            zlibDecompress(bytes)
        }
    }

    private fun decodeHexParts(parts: List<String>): ByteArray {
        val output = ByteArrayOutputStream()
        for ((index, part) in parts.withIndex()) {
            try {
                output.write(hexDecode(part))
            } catch (e: Exception) {
                throw DecodeError.UnableToDecodeHex(index, e.message ?: "Unknown error")
            }
        }
        return output.toByteArray()
    }

    private fun decodeBase32Parts(parts: List<String>): ByteArray {
        val output = ByteArrayOutputStream()
        for ((index, part) in parts.withIndex()) {
            try {
                output.write(Base32.decode(part))
            } catch (e: Exception) {
                throw DecodeError.UnableToDecodeBase32(index, e.message ?: "Unknown error")
            }
        }
        return output.toByteArray()
    }

    private fun hexDecode(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(hex.length / 2) { i ->
            val high = Character.digit(hex[i * 2], 16)
            val low = Character.digit(hex[i * 2 + 1], 16)
            require(high >= 0 && low >= 0) { "Invalid hex character at position ${i * 2}" }
            ((high shl 4) or low).toByte()
        }
    }

    private fun zlibDecompress(data: ByteArray): ByteArray {
        try {
            val inflater = Inflater(10, true)
            val output = ByteArray(data.size * 4)

            inflater.setInput(data, 0, data.size, false)
            inflater.setOutput(output, 0, output.size)

            val result = ByteArrayOutputStream()
            var status = inflater.inflate(JZlib.Z_NO_FLUSH)

            while (status != JZlib.Z_STREAM_END) {
                if (status != JZlib.Z_OK && status != JZlib.Z_BUF_ERROR) {
                    throw DecodeError.UnableToInflateZlib("Inflate failed with status: $status")
                }

                if (inflater.next_out_index > 0) {
                    result.write(output, 0, inflater.next_out_index)
                    inflater.setOutput(output, 0, output.size)
                }

                status = inflater.inflate(JZlib.Z_NO_FLUSH)
            }

            if (inflater.next_out_index > 0) {
                result.write(output, 0, inflater.next_out_index)
            }

            inflater.end()
            return result.toByteArray()
        } catch (e: DecodeError) {
            throw e
        } catch (e: Exception) {
            throw DecodeError.UnableToInflateZlib(e.message ?: "Unknown decompression error")
        }
    }
}
