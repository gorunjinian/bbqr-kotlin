package com.gorunjinian.bbqr

import kotlin.test.Test
import kotlin.test.assertEquals

class RoundtripTest {

    @Test
    fun `hex roundtrip small data`() {
        val data = "Hello, World!, but much larger".toByteArray()
        val split = SplitResult.fromData(data, FileType.UnicodeText, SplitOptions(encoding = Encoding.Hex))
        val joined = Joined.fromParts(split.parts)
        assertEquals(FileType.UnicodeText, joined.fileType)
        assertEquals(Encoding.Hex, joined.encoding)
        assertEquals(String(data), String(joined.data))
    }

    @Test
    fun `base32 roundtrip`() {
        val data = "The quick brown fox jumps over the lazy dog.".toByteArray()
        val split = SplitResult.fromData(data, FileType.Json, SplitOptions(encoding = Encoding.Base32))
        val joined = Joined.fromParts(split.parts)
        assertEquals(String(data), String(joined.data))
    }

    @Test
    fun `zlib roundtrip`() {
        val data = "Hello, World!, but much larger and repeated! ".repeat(100).toByteArray()
        val split = SplitResult.fromData(data, FileType.UnicodeText)
        val joined = Joined.fromParts(split.parts)
        assertEquals(String(data), String(joined.data))
    }

    @Test
    fun `zlib roundtrip with custom options`() {
        val data = "Hello, World!, but much larger".toByteArray()
        val split = SplitResult.fromData(
            data,
            FileType.UnicodeText,
            SplitOptions(
                encoding = Encoding.Zlib,
                minSplitNumber = 1,
                maxSplitNumber = 100,
                minVersion = Version.V03,
                maxVersion = Version.V30,
            ),
        )

        val joined = Joined.fromParts(split.parts)
        assertEquals(FileType.UnicodeText, joined.fileType)
        assertEquals(String(data), String(joined.data))
    }

    @Test
    fun `large binary data roundtrip`() {
        val data = ByteArray(10000) { (it % 256).toByte() }
        val split = SplitResult.fromData(data, FileType.Psbt, SplitOptions(encoding = Encoding.Hex))
        val joined = Joined.fromParts(split.parts)
        assertEquals(FileType.Psbt, joined.fileType)
        assertEquals(data.toList(), joined.data.toList())
    }

    @Test
    fun `continuous joiner roundtrip`() {
        val data = "Continuous join test data repeated. ".repeat(50).toByteArray()
        val split = SplitResult.fromData(data, FileType.UnicodeText)

        val joiner = ContinuousJoiner()
        var result: ContinuousJoinResult = ContinuousJoinResult.NotStarted
        for (part in split.parts) {
            result = joiner.addPart(part)
        }

        val complete = result as ContinuousJoinResult.Complete
        assertEquals(String(data), String(complete.joined.data))
    }

    @Test
    fun `cross-compat decode rust PSBT part`() {
        // This part was produced by the Rust implementation
        val parts = listOf(
            "B\$ZP0100FMUE4KXZZ7EHBEUJQGAYDGMXLP2O34WEVGERCKNOQWTY3URDYXHGTXTTHOVHKW6YXZQYEVSN6UGQMEHYB4CP4RI3BJK43MVLSZ7ZRH5R5SB4527A5C6N3LL65GZRPNZZGU2NK332BFLVR7VVKSYTCILLOLCJZ5PGFQGCC2U37YW5J6CTXMW6NHMT533MMPJ3YXQQVGL2ZZ76AADRETFVNCV4MGOMY6GEPPPG5XSZFIOQ6PW2JUFQVDWX5FMIHLHCI3Y23V5WGPPK5OF5GSFOKZI576FTVA5FGM5NNACNMNSAQBLZQZY62PJQS7Q72PQGVTABGZRVEPB4HL5MGCB3SVONS5P7W7BGVVT557AFA33L63OW43GW5BAYVTCMNLH5XCUEDNRF4HXFFLMHCWV25FZM6VQNQFPW2PZWI5MXP3QKHGIKOPMXR6CZGOO6CFSQ42IKN3USTQ44OHX53YYXTSSX74XXZ7ZPFXMRMCDTBDJU3JIJ2Z6ZXJXOK3LD6YHA3NYV4PDUDFR2K2H6RIPZ4BI7V2YOXRXPQY5YJELSQ4EHNXBM6HQX74PC26NXEXI6B7UR6IQMH446CDUR36RHYRKV5DE5ZUUVS5FEIWNKJLGJXHGPK66WGP5YNX3PEW24ZRLTJYAOMEBNXIGEYT2OSSUTLGKFZX7M7VTZUGI5LH5GM6X6PC76XZOO3PM7WV67BCM3D5M3XMAEQBYPJAWRAJZQAMEQA"
        )

        val joined = Joined.fromParts(parts)
        assertEquals(FileType.Psbt, joined.fileType)
        assertEquals(Encoding.Zlib, joined.encoding)
        assert(joined.data.isNotEmpty())
    }

    @Test
    fun `version data capacity matches known values`() {
        assertEquals(25, Version.V01.dataCapacity)
        assertEquals(1990, Version.V26.dataCapacity)
        assertEquals(4296, Version.V40.dataCapacity)
    }
}
