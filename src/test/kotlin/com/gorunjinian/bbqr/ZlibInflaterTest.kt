package com.gorunjinian.bbqr

import com.jcraft.jzlib.Deflater
import com.jcraft.jzlib.JZlib
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Tests to isolate whether JZlib's Inflater can decompress data correctly,
 * comparing against java.util.zip.Inflater as a known-good reference.
 */
class ZlibInflaterTest {

    /** Compress with JZlib Deflater(Z_BEST_COMPRESSION, wbits=10, nowrap=true) — same as Encode.kt */
    private fun jzlibCompress(data: ByteArray): ByteArray {
        val deflater = Deflater(JZlib.Z_BEST_COMPRESSION, 10, true)
        val output = ByteArray(data.size + 64)
        deflater.setInput(data, 0, data.size, false)
        deflater.setOutput(output, 0, output.size)
        deflater.params(JZlib.Z_BEST_COMPRESSION, JZlib.Z_DEFAULT_STRATEGY)
        var status = deflater.deflate(JZlib.Z_FINISH)
        while (status != JZlib.Z_STREAM_END) {
            if (status != JZlib.Z_OK) error("Deflate failed: $status")
            val newOutput = ByteArray(output.size * 2)
            output.copyInto(newOutput)
            deflater.setOutput(newOutput, deflater.next_out_index, newOutput.size - deflater.next_out_index)
            status = deflater.deflate(JZlib.Z_FINISH)
        }
        val size = deflater.next_out_index
        deflater.end()
        return output.copyOf(size)
    }

    /** Decompress with JZlib Inflater — same as Decode.kt */
    private fun jzlibDecompress(data: ByteArray, wbits: Int, nowrap: Boolean): ByteArray {
        val inflater = com.jcraft.jzlib.Inflater(wbits, nowrap)
        val output = ByteArray(data.size * 4)
        inflater.setInput(data, 0, data.size, false)
        inflater.setOutput(output, 0, output.size)
        val result = ByteArrayOutputStream()
        var status = inflater.inflate(JZlib.Z_NO_FLUSH)
        while (status != JZlib.Z_STREAM_END) {
            if (status != JZlib.Z_OK && status != JZlib.Z_BUF_ERROR) {
                error("JZlib inflate failed with status: $status")
            }
            if (inflater.next_out_index > 0) {
                result.write(output, 0, inflater.next_out_index)
                inflater.setOutput(output, 0, output.size)
            } else if (status == JZlib.Z_BUF_ERROR) {
                error("JZlib inflate stalled: no progress")
            }
            status = inflater.inflate(JZlib.Z_NO_FLUSH)
        }
        if (inflater.next_out_index > 0) {
            result.write(output, 0, inflater.next_out_index)
        }
        inflater.end()
        return result.toByteArray()
    }

    /** Decompress with java.util.zip.Inflater — known working reference */
    private fun javaDecompress(data: ByteArray, nowrap: Boolean): ByteArray {
        val inflater = java.util.zip.Inflater(nowrap)
        inflater.setInput(data)
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && inflater.needsInput()) break
            result.write(buffer, 0, count)
        }
        inflater.end()
        return result.toByteArray()
    }

    /** Compress with java.util.zip.Deflater (wbits=15, nowrap=true) */
    private fun javaCompress(data: ByteArray): ByteArray {
        val deflater = java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION, true)
        deflater.setInput(data)
        deflater.finish()
        val output = ByteArray(data.size + 64)
        val size = deflater.deflate(output)
        deflater.end()
        return output.copyOf(size)
    }

    // ---- Test 1: Can JZlib decompress its OWN output? ----

    @Test
    fun `jzlib decompress own output - wbits 10`() {
        val original = "Hello, BBQr world! This is a PSBT test payload.".toByteArray()
        val compressed = jzlibCompress(original)
        val decompressed = jzlibDecompress(compressed, wbits = 10, nowrap = true)
        assertEquals(original.toList(), decompressed.toList())
    }

    @Test
    fun `jzlib decompress own output - wbits 15`() {
        val original = "Hello, BBQr world! This is a PSBT test payload.".toByteArray()
        val compressed = jzlibCompress(original)
        val decompressed = jzlibDecompress(compressed, wbits = 15, nowrap = true)
        assertEquals(original.toList(), decompressed.toList())
    }

    // ---- Test 2: Can JZlib decompress java.util.zip output? ----

    @Test
    fun `jzlib decompress java-compressed data - wbits 15`() {
        val original = "Hello, BBQr world! This is a PSBT test payload.".toByteArray()
        val compressed = javaCompress(original)
        val decompressed = jzlibDecompress(compressed, wbits = 15, nowrap = true)
        assertEquals(original.toList(), decompressed.toList())
    }

    // ---- Test 3: Can java.util.zip decompress JZlib output? ----

    @Test
    fun `java decompress jzlib-compressed data`() {
        val original = "Hello, BBQr world! This is a PSBT test payload.".toByteArray()
        val compressed = jzlibCompress(original)
        val decompressed = javaDecompress(compressed, nowrap = true)
        assertEquals(original.toList(), decompressed.toList())
    }

    // ---- Test 4: Larger data (realistic PSBT size) ----

    @Test
    fun `jzlib roundtrip with large repetitive data`() {
        val original = ByteArray(2000) { (it % 256).toByte() }
        val compressed = jzlibCompress(original)
        val decompressed = jzlibDecompress(compressed, wbits = 15, nowrap = true)
        assertEquals(original.toList(), decompressed.toList())
    }

    @Test
    fun `jzlib decompress java-compressed large data`() {
        val original = ByteArray(2000) { (it % 256).toByte() }
        val compressed = javaCompress(original)
        val decompressed = jzlibDecompress(compressed, wbits = 15, nowrap = true)
        assertEquals(original.toList(), decompressed.toList())
    }

    // ---- Test 5: Both produce same bytes from same input? ----

    @Test
    fun `java and jzlib decompress produce same result from jzlib-compressed data`() {
        val original = ByteArray(500) { (it * 7 + 3).toByte() }
        val compressed = jzlibCompress(original)
        val fromJzlib = jzlibDecompress(compressed, wbits = 15, nowrap = true)
        val fromJava = javaDecompress(compressed, nowrap = true)
        assertEquals(fromJava.toList(), fromJzlib.toList())
    }

    @Test
    fun `java and jzlib decompress produce same result from java-compressed data`() {
        val original = ByteArray(500) { (it * 7 + 3).toByte() }
        val compressed = javaCompress(original)
        val fromJzlib = jzlibDecompress(compressed, wbits = 15, nowrap = true)
        val fromJava = javaDecompress(compressed, nowrap = true)
        assertEquals(fromJava.toList(), fromJzlib.toList())
    }

    // ---- Test 6: What if data is zlib-wrapped (nowrap=false)? ----

    @Test
    fun `jzlib fails on zlib-wrapped data when nowrap is true`() {
        val original = "Hello, BBQr world! This is a PSBT test payload.".toByteArray()
        // Compress WITHOUT nowrap → produces zlib header + data + adler32
        val deflater = java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION, false)
        deflater.setInput(original)
        deflater.finish()
        val output = ByteArray(original.size + 64)
        val size = deflater.deflate(output)
        deflater.end()
        val zlibWrapped = output.copyOf(size)

        // java.util.zip with nowrap=true should FAIL on zlib-wrapped data
        val javaResult = try {
            javaDecompress(zlibWrapped, nowrap = true)
            "success"
        } catch (_: Exception) { "failed" }

        // JZlib with nowrap=true should also FAIL on zlib-wrapped data
        val jzlibResult = try {
            jzlibDecompress(zlibWrapped, wbits = 15, nowrap = true)
            "success"
        } catch (_: Exception) { "failed" }

        println("java.util.zip(nowrap=true) on zlib-wrapped data: $javaResult")
        println("JZlib(wbits=15,nowrap=true) on zlib-wrapped data: $jzlibResult")

        // Both with nowrap=false SHOULD succeed
        val javaOk = javaDecompress(zlibWrapped, nowrap = false)
        assertEquals(original.toList(), javaOk.toList())

        val jzlibOk = jzlibDecompress(zlibWrapped, wbits = 15, nowrap = false)
        assertEquals(original.toList(), jzlibOk.toList())
    }

    // ---- Test 7: Decode.decodeOrderedParts handles zlib-wrapped data via fallback ----

    @Test
    fun `decodeOrderedParts handles zlib-wrapped compressed data`() {
        val original = "Test PSBT payload for BBQr decoding".toByteArray()
        // Compress with zlib wrapper (no nowrap) — simulates a non-spec encoder
        val deflater = java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION, false)
        deflater.setInput(original)
        deflater.finish()
        val buf = ByteArray(original.size + 64)
        val size = deflater.deflate(buf)
        deflater.end()
        val zlibWrapped = buf.copyOf(size)

        // Base32-encode the zlib-wrapped bytes (as an external encoder would)
        val base32Data = Base32.encode(zlibWrapped)

        // Decode should succeed via the nowrap=false fallback
        val decoded = Decode.decodeOrderedParts(listOf(base32Data), Encoding.Zlib)
        assertEquals(original.toList(), decoded.toList())
    }

    // ---- Test 8: Full BBQr encode→decode roundtrip through ContinuousJoiner ----

    @Test
    fun `full bbqr split and rejoin with zlib`() {
        val original = ByteArray(500) { (it % 10).toByte() }
        val split = SplitResult.fromData(original, FileType.Psbt, SplitOptions(encoding = Encoding.Zlib))
        val joiner = ContinuousJoiner()
        for (part in split.parts) {
            val result = joiner.addPart(part)
            if (result is ContinuousJoinResult.Complete) {
                assertEquals(original.toList(), result.joined.data.toList())
                return
            }
        }
        fail("ContinuousJoiner did not complete")
    }
}
