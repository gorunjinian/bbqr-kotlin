package com.gorunjinian.bbqr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ContinuousJoinerTest {

    @Test
    fun `empty part returns current state`() {
        val joiner = ContinuousJoiner()
        val result = joiner.addPart("")
        assertIs<ContinuousJoinResult.NotStarted>(result)
    }

    @Test
    fun `single part completes immediately`() {
        val data = "Hello, World!".toByteArray()
        val split = SplitResult.fromData(data, FileType.UnicodeText, SplitOptions(encoding = Encoding.Hex))
        assertEquals(1, split.parts.size)

        val joiner = ContinuousJoiner()
        val result = joiner.addPart(split.parts[0])
        assertIs<ContinuousJoinResult.Complete>(result)
        assertEquals("Hello, World!", String(result.joined.data))
    }

    @Test
    fun `multi-part join progresses correctly`() {
        val data = ByteArray(4000) { 'A'.code.toByte() }
        val split = SplitResult.fromData(
            data,
            FileType.Psbt,
            SplitOptions(encoding = Encoding.Hex),
        )
        assert(split.parts.size > 1)

        val joiner = ContinuousJoiner()

        for (i in 0 until split.parts.size - 1) {
            val result = joiner.addPart(split.parts[i])
            assertIs<ContinuousJoinResult.InProgress>(result)
            assertEquals(split.parts.size - i - 1, result.partsLeft)
        }

        val finalResult = joiner.addPart(split.parts.last())
        assertIs<ContinuousJoinResult.Complete>(finalResult)
        assertEquals(data.toList(), finalResult.joined.data.toList())
    }

    @Test
    fun `adding part after complete returns complete`() {
        val data = "Hi".toByteArray()
        val split = SplitResult.fromData(data, FileType.UnicodeText, SplitOptions(encoding = Encoding.Hex))

        val joiner = ContinuousJoiner()
        joiner.addPart(split.parts[0])

        // Adding again should still return complete
        val result = joiner.addPart(split.parts[0])
        assertIs<ContinuousJoinResult.Complete>(result)
    }

    @Test
    fun `out of order parts work`() {
        val data = ByteArray(4000) { 'A'.code.toByte() }
        val split = SplitResult.fromData(
            data,
            FileType.Psbt,
            SplitOptions(encoding = Encoding.Hex),
        )
        assert(split.parts.size > 1)

        val joiner = ContinuousJoiner()
        // Add parts in reverse order
        val reversed = split.parts.reversed()
        for (i in 0 until reversed.size - 1) {
            val result = joiner.addPart(reversed[i])
            assertIs<ContinuousJoinResult.InProgress>(result)
        }

        val finalResult = joiner.addPart(reversed.last())
        assertIs<ContinuousJoinResult.Complete>(finalResult)
        assertEquals(data.toList(), finalResult.joined.data.toList())
    }
}
