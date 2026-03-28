package com.gorunjinian.bbqr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JoinTest {

    @Test
    fun `join empty throws`() {
        assertFailsWith<JoinError.Empty> {
            Joined.fromParts(emptyList())
        }
    }

    @Test
    fun `join all empty strings throws`() {
        assertFailsWith<JoinError.Empty> {
            Joined.fromParts(listOf("", "", ""))
        }
    }

    @Test
    fun `join conflicting headers throws`() {
        assertFailsWith<JoinError.ConflictingHeaders> {
            Joined.fromParts(listOf("B\$ZU0801data", "B\$ZU0902data"))
        }
    }

    @Test
    fun `join detects duplicate with wrong content`() {
        // Two parts with same index but different data
        val header = "B\$HU02"
        val part0a = "${header}00AABB"
        val part0b = "${header}00CCDD"

        assertFailsWith<JoinError.DuplicatePartWrongContent> {
            Joined.fromParts(listOf(part0a, part0b))
        }
    }

    @Test
    fun `join allows duplicate with same content`() {
        // Create a simple single-part message
        val data = "Hello".toByteArray()
        val split = SplitResult.fromData(data, FileType.UnicodeText, SplitOptions(encoding = Encoding.Hex))

        // Duplicate the same part
        val parts = split.parts + split.parts
        val joined = Joined.fromParts(parts)
        assertEquals("Hello", String(joined.data))
    }
}
