package com.gorunjinian.bbqr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HeaderTest {

    @Test
    fun `parse valid header`() {
        val input = "B\$ZU0801somedata"
        val header = Header.parse(input)
        assertEquals(Encoding.Zlib, header.encoding)
        assertEquals(FileType.UnicodeText, header.fileType)
        assertEquals(8, header.numParts)
    }

    @Test
    fun `parse header hex psbt`() {
        val input = "B\$HP0200data"
        val header = Header.parse(input)
        assertEquals(Encoding.Hex, header.encoding)
        assertEquals(FileType.Psbt, header.fileType)
        assertEquals(2, header.numParts)
    }

    @Test
    fun `fails on bad fixed header`() {
        assertFailsWith<HeaderParseError.InvalidFixedHeader> {
            Header.parse("B#888888")
        }
    }

    @Test
    fun `fails on empty`() {
        assertFailsWith<HeaderParseError.Empty> {
            Header.parse("")
        }
    }

    @Test
    fun `fails on too short`() {
        assertFailsWith<HeaderParseError.InvalidHeaderSize> {
            Header.parse("B\$ZU")
        }
    }

    @Test
    fun `int to base36 padding`() {
        assertEquals("00", intToBase36(0))
        assertEquals("01", intToBase36(1))
        assertEquals("02", intToBase36(2))
        assertEquals("0Z", intToBase36(35))
        assertEquals("10", intToBase36(36))
        assertEquals("11", intToBase36(37))
        assertEquals("FG", intToBase36(556))
        assertEquals("ZZ", intToBase36(1295))
    }

    @Test
    fun `header toString roundtrip`() {
        val header = Header(Encoding.Zlib, FileType.UnicodeText, 8)
        assertEquals("B\$ZU08", header.toString())

        val parsed = Header.parse(header.toString() + "00")
        assertEquals(header, parsed)
    }
}
