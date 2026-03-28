package com.gorunjinian.bbqr

import com.jcraft.jzlib.Deflater
import com.jcraft.jzlib.JZlib

internal class Encoded(
    val encoding: Encoding,
    val data: String,
) {
    fun numberOfQrsNeeded(version: Version): QrsNeeded {
        val dataSize = data.length

        val baseCapacity = version.dataCapacity - HEADER_LENGTH
        val adjustedCapacity = baseCapacity - (baseCapacity % encoding.splitMod)

        val estimatedCount = ceilDiv(dataSize, adjustedCapacity)

        if (estimatedCount == 1) {
            return QrsNeeded(version, count = 1, dataPerQr = dataSize)
        }

        val totalCapacity = (estimatedCount - 1) * adjustedCapacity + baseCapacity
        val count = if (totalCapacity >= dataSize) estimatedCount else estimatedCount + 1

        return QrsNeeded(version, count, dataPerQr = adjustedCapacity)
    }

    companion object {
        fun fromData(data: ByteArray, encoding: Encoding): Encoded {
            if (data.isEmpty()) throw EncodeError.Empty()

            return when (encoding) {
                Encoding.Hex -> Encoded(
                    encoding = Encoding.Hex,
                    data = data.toHexString(),
                )

                Encoding.Base32 -> Encoded(
                    encoding = Encoding.Base32,
                    data = Base32.encode(data),
                )

                Encoding.Zlib -> {
                    val compressed = zlibCompress(data)

                    if (compressed.size < data.size) {
                        Encoded(
                            encoding = Encoding.Zlib,
                            data = Base32.encode(compressed),
                        )
                    } else {
                        Encoded(
                            encoding = Encoding.Base32,
                            data = Base32.encode(data),
                        )
                    }
                }
            }
        }
    }
}

internal data class QrsNeeded(
    val version: Version,
    val count: Int,
    val dataPerQr: Int,
) : Comparable<QrsNeeded> {
    override fun compareTo(other: QrsNeeded): Int {
        val countCmp = count.compareTo(other.count)
        return if (countCmp != 0) countCmp else version.ordinal.compareTo(other.version.ordinal)
    }
}

private fun ByteArray.toHexString(): String {
    val sb = StringBuilder(size * 2)
    for (b in this) {
        sb.append(HEX_CHARS[(b.toInt() shr 4) and 0x0F])
        sb.append(HEX_CHARS[b.toInt() and 0x0F])
    }
    return sb.toString()
}

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b

private fun zlibCompress(data: ByteArray): ByteArray {
    try {
        val deflater = Deflater(JZlib.Z_BEST_COMPRESSION, 10, true)
        val output = ByteArray(data.size + 64)

        deflater.setInput(data, 0, data.size, false)
        deflater.setOutput(output, 0, output.size)
        deflater.params(JZlib.Z_BEST_COMPRESSION, JZlib.Z_DEFAULT_STRATEGY)

        var status = deflater.deflate(JZlib.Z_FINISH)
        while (status != JZlib.Z_STREAM_END) {
            if (status != JZlib.Z_OK) {
                throw EncodeError.CompressionError("Deflate failed with status: $status")
            }
            // Need a bigger buffer
            val newOutput = ByteArray(output.size * 2)
            output.copyInto(newOutput)
            deflater.setOutput(newOutput, deflater.next_out_index, newOutput.size - deflater.next_out_index)
            status = deflater.deflate(JZlib.Z_FINISH)
        }

        val compressedSize = deflater.next_out_index
        deflater.end()
        return output.copyOf(compressedSize)
    } catch (e: EncodeError) {
        throw e
    } catch (e: Exception) {
        throw EncodeError.CompressionError(e.message ?: "Unknown compression error")
    }
}
