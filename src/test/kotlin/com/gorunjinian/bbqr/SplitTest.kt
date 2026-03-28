package com.gorunjinian.bbqr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SplitTest {

    @Test
    fun `split 4000 bytes hex`() {
        val data = ByteArray(4000) { 'A'.code.toByte() }
        val split = SplitResult.fromData(
            data,
            FileType.Psbt,
            SplitOptions(
                encoding = Encoding.Hex,
                minVersion = Version.V01,
                maxVersion = Version.V40,
            ),
        )

        assertTrue(split.version <= Version.V40)
        assertEquals(Encoding.Hex, split.encoding)
        assertEquals(Version.V39, split.version)
        assertEquals(2, split.parts.size)

        val header = Header.parse(split.parts[0])
        assertEquals(2, header.numParts)
        assertEquals(Encoding.Hex, header.encoding)
        assertEquals(FileType.Psbt, header.fileType)
    }

    @Test
    fun `split empty throws`() {
        assertFailsWith<SplitError.Empty> {
            SplitResult.fromData(ByteArray(0), FileType.Psbt)
        }
    }

    @Test
    fun `split 2000 bytes hex with min version`() {
        val data = ByteArray(2000) { 'A'.code.toByte() }
        val split = SplitResult.fromData(
            data,
            FileType.Psbt,
            SplitOptions(
                encoding = Encoding.Hex,
                minVersion = Version.V11,
                maxVersion = Version.V40,
            ),
        )

        assertTrue(split.version <= Version.V40)
        assertEquals(Encoding.Hex, split.encoding)
        assertEquals(Version.V39, split.version)
        assertEquals(1, split.parts.size)

        val header = Header.parse(split.parts[0])
        assertEquals(1, header.numParts)
    }

    @Test
    fun `split with zlib encoding`() {
        val data = "Hello, World!, but much larger and repeated. ".repeat(50).toByteArray()
        val split = SplitResult.fromData(data, FileType.UnicodeText)

        assertTrue(split.parts.isNotEmpty())
        assertTrue(split.parts.all { it.startsWith("B\$") })

        // Verify header consistency
        val headers = split.parts.map { it.substring(0, 6) }
        assertTrue(headers.all { it == headers[0] })
    }

    @Test
    fun `split validates max split size`() {
        assertFailsWith<SplitError.MaxSplitSizeTooLarge> {
            SplitResult.fromData(
                ByteArray(10) { 1 },
                FileType.Psbt,
                SplitOptions(maxSplitNumber = MAX_PARTS + 1),
            )
        }
    }

    @Test
    fun `split validates min split too small`() {
        assertFailsWith<SplitError.MinSplitTooSmall> {
            SplitResult.fromData(
                ByteArray(10) { 1 },
                FileType.Psbt,
                SplitOptions(minSplitNumber = 0),
            )
        }
    }

    @Test
    fun `split validates range`() {
        assertFailsWith<SplitError.InvalidSplitRange> {
            SplitResult.fromData(
                ByteArray(10) { 1 },
                FileType.Psbt,
                SplitOptions(minSplitNumber = 10, maxSplitNumber = 5),
            )
        }
    }

    @Test
    fun `split validates version range`() {
        assertFailsWith<SplitError.InvalidVersionRange> {
            SplitResult.fromData(
                ByteArray(10) { 1 },
                FileType.Psbt,
                SplitOptions(minVersion = Version.V40, maxVersion = Version.V01),
            )
        }
    }
}
