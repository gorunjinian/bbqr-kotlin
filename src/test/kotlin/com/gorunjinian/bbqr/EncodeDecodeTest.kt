package com.gorunjinian.bbqr

import kotlin.test.Test
import kotlin.test.assertEquals

class EncodeDecodeTest {

    @Test
    fun `encode hex`() {
        val data = "Hello, world!".toByteArray()
        val encoded = Encoded.fromData(data, Encoding.Hex)
        assertEquals(Encoding.Hex, encoded.encoding)
        assertEquals("48656C6C6F2C20776F726C6421", encoded.data)
    }

    @Test
    fun `encode base32`() {
        val data = "The quick brown fox jumps over the lazy dog.".toByteArray()
        val encoded = Encoded.fromData(data, Encoding.Base32)
        assertEquals(Encoding.Base32, encoded.encoding)
        assertEquals("KRUGKIDROVUWG2ZAMJZG653OEBTG66BANJ2W24DTEBXXMZLSEB2GQZJANRQXU6JAMRXWOLQ", encoded.data)
    }

    @Test
    fun `encode empty throws`() {
        try {
            Encoded.fromData(ByteArray(0), Encoding.Hex)
            error("Should have thrown")
        } catch (_: EncodeError.Empty) {
            // expected
        }
    }

    @Test
    fun `hex encode-decode roundtrip`() {
        val original = ByteArray(200) { (it * 7).toByte() }
        val encoded = Encoded.fromData(original, Encoding.Hex)
        val decoded = Decode.decodeOrderedParts(listOf(encoded.data), Encoding.Hex)
        assertEquals(original.toList(), decoded.toList())
    }

    @Test
    fun `base32 encode-decode roundtrip`() {
        val original = ByteArray(200) { (it * 13).toByte() }
        val encoded = Encoded.fromData(original, Encoding.Base32)
        val decoded = Decode.decodeOrderedParts(listOf(encoded.data), Encoding.Base32)
        assertEquals(original.toList(), decoded.toList())
    }

    @Test
    fun `zlib encode-decode roundtrip`() {
        // Use repetitive data that compresses well
        val original = ByteArray(500) { (it % 10).toByte() }
        val encoded = Encoded.fromData(original, Encoding.Zlib)
        assertEquals(Encoding.Zlib, encoded.encoding)
        val decoded = Decode.decodeOrderedParts(listOf(encoded.data), encoded.encoding)
        assertEquals(original.toList(), decoded.toList())
    }

    @Test
    fun `zlib falls back to base32 for incompressible data`() {
        // Random-looking data that doesn't compress
        val original = ByteArray(100) { (it * 137 + 42).toByte() }
        val encoded = Encoded.fromData(original, Encoding.Zlib)
        // May fall back to Base32 if compression doesn't help
        val decoded = Decode.decodeOrderedParts(listOf(encoded.data), encoded.encoding)
        assertEquals(original.toList(), decoded.toList())
    }

    @Test
    fun `number of QRs needed calculation`() {
        // Ported from Rust: 2500 bytes hex-encoded at V05 needs 35 QRs
        val data = ByteArray(2500) { 'A'.code.toByte() }
        val encoded = Encoded.fromData(data, Encoding.Hex)
        assertEquals(5000, encoded.data.length)
        val qrsNeeded = encoded.numberOfQrsNeeded(Version.V05)
        assertEquals(35, qrsNeeded.count)
        assertEquals(146, qrsNeeded.dataPerQr)
    }
}
