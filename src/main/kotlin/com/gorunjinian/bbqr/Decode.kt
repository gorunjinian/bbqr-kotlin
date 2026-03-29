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
        // Try JZlib first (raw deflate, then zlib-wrapped), fall back to java.util.zip
        // if JZlib can't handle the stream (some deflate streams trigger JZlib edge cases).
        try {
            return inflateBytes(data, nowrap = true)
        } catch (_: Exception) {}
        try {
            return inflateBytes(data, nowrap = false)
        } catch (_: Exception) {}
        // Fallback: java.util.zip.Inflater handles all standard deflate streams
        try {
            return javaInflateBytes(data, nowrap = true)
        } catch (_: Exception) {}
        try {
            return javaInflateBytes(data, nowrap = false)
        } catch (e: Exception) {
            throw DecodeError.UnableToInflateZlib(e.message ?: "Unknown decompression error")
        }
    }

    private fun javaInflateBytes(data: ByteArray, nowrap: Boolean): ByteArray {
        val inflater = java.util.zip.Inflater(nowrap)
        try {
            inflater.setInput(data)
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && inflater.needsInput()) break
                result.write(buffer, 0, count)
            }
            if (result.size() == 0) {
                throw DecodeError.UnableToInflateZlib("No data produced")
            }
            return result.toByteArray()
        } finally {
            inflater.end()
        }
    }

    private fun inflateBytes(data: ByteArray, nowrap: Boolean): ByteArray {
        val inflater = Inflater(15, nowrap)
        try {
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
                } else if (status == JZlib.Z_BUF_ERROR) {
                    throw DecodeError.UnableToInflateZlib("Inflate stalled: no progress possible")
                }

                status = inflater.inflate(JZlib.Z_NO_FLUSH)
            }

            if (inflater.next_out_index > 0) {
                result.write(output, 0, inflater.next_out_index)
            }

            return result.toByteArray()
        } finally {
            inflater.end()
        }
    }
}
