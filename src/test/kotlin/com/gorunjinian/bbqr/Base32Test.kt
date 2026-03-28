package com.gorunjinian.bbqr

import kotlin.test.Test
import kotlin.test.assertEquals

class Base32Test {

    @Test
    fun `encode empty`() {
        assertEquals("", Base32.encode(ByteArray(0)))
    }

    @Test
    fun `decode empty`() {
        assertEquals(0, Base32.decode("").size)
    }

    @Test
    fun `encode hello world`() {
        val data = "Hello, world!".toByteArray()
        val encoded = Base32.encode(data)
        assertEquals("JBSWY3DPFQQHO33SNRSCC", encoded)
    }

    @Test
    fun `roundtrip`() {
        val data = "The quick brown fox jumps over the lazy dog.".toByteArray()
        val encoded = Base32.encode(data)
        assertEquals("KRUGKIDROVUWG2ZAMJZG653OEBTG66BANJ2W24DTEBXXMZLSEB2GQZJANRQXU6JAMRXWOLQ", encoded)
        val decoded = Base32.decode(encoded)
        assertEquals(String(data), String(decoded))
    }

    @Test
    fun `roundtrip binary data`() {
        val data = ByteArray(256) { it.toByte() }
        val encoded = Base32.encode(data)
        val decoded = Base32.decode(encoded)
        assertEquals(data.toList(), decoded.toList())
    }
}
