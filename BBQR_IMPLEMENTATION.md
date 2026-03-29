# BBQr Implementation Guide

## Overview

BBQr (Better Bitcoin QR) is a protocol for encoding binary data into one or more QR codes, optimized for Bitcoin-related payloads like PSBTs and transactions. It uses zlib compression and a simple header format to efficiently split large data across multiple QR codes that can be scanned sequentially.

**Dependency:**
```kotlin
implementation("com.gorunjinian:bbqr:1.0.0")
```

**Package:** `com.gorunjinian.bbqr`

**Key characteristics:**
- Pure Kotlin/JVM implementation — no native dependencies
- Cross-compatible with Rust and Python BBQr implementations
- Automatic QR version selection for optimal fit
- Built-in zlib compression (Zlib encoding mode)
- Streaming/incremental QR scanning support via `ContinuousJoiner`
- Native support for PSBT, Transaction, JSON, CBOR, and Unicode text file types
- Supports up to 1295 QR code parts per payload

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [API Reference](#api-reference)
   - [SplitResult Class](#splitresult-class)
   - [Joined Class](#joined-class)
   - [ContinuousJoiner Class](#continuousjoiner-class)
   - [SplitOptions Data Class](#splitoptions-data-class)
   - [Encoding Enum](#encoding-enum)
   - [FileType Enum](#filetype-enum)
   - [Version Enum](#version-enum)
   - [ContinuousJoinResult Sealed Class](#continuousjoinresult-sealed-class)
   - [Error Types](#error-types)
3. [PSBT Encoding Guide](#psbt-encoding-guide)
4. [PSBT Decoding Guide](#psbt-decoding-guide)
5. [Multi-Part QR Codes](#multi-part-qr-codes)
6. [BBQr Header Format](#bbqr-header-format)
7. [Error Handling](#error-handling)
8. [Complete Examples](#complete-examples)
9. [Comparison: BBQr vs BC-UR](#comparison-bbqr-vs-bc-ur)

---

## Quick Start

### Encode a PSBT into QR Code Parts

```kotlin
import com.gorunjinian.bbqr.*

val psbtBytes: ByteArray = // ... your PSBT data

val split = SplitResult.fromData(psbtBytes, FileType.Psbt)

val qrParts: List<String> = split.parts
// Each string in qrParts is the content for one QR code
```

### Decode Scanned QR Codes Back to PSBT

```kotlin
import com.gorunjinian.bbqr.*

val joiner = ContinuousJoiner()

// Call addPart() each time a QR code is scanned
fun onQrCodeScanned(qrContent: String) {
    when (val result = joiner.addPart(qrContent)) {
        is ContinuousJoinResult.NotStarted -> { /* Invalid/empty data */ }
        is ContinuousJoinResult.InProgress -> {
            updateProgress(result.partsLeft)
        }
        is ContinuousJoinResult.Complete -> {
            val psbtBytes = result.joined.data
            handleCompletePsbt(psbtBytes)
        }
    }
}
```

---

## API Reference

### SplitResult Class

Encodes binary data into one or more QR code parts. This is the primary class for encoding.

```kotlin
import com.gorunjinian.bbqr.SplitResult
```

#### Creating a SplitResult

```kotlin
SplitResult.fromData(
    bytes: ByteArray,                    // Raw data to encode
    fileType: FileType,                  // Type of data (Psbt, Transaction, etc.)
    options: SplitOptions = SplitOptions() // Encoding configuration (optional)
): SplitResult
```

**Throws:** `SplitError` on failure

#### Properties

| Property   | Type           | Description                               |
|-----------|----------------|-------------------------------------------|
| `parts`    | `List<String>` | All QR code content strings               |
| `encoding` | `Encoding`     | The encoding that was used                |
| `version`  | `Version`      | The QR code version that was selected     |

#### Usage

```kotlin
val split = SplitResult.fromData(data, FileType.Psbt)
println("Version: ${split.version}")
println("Encoding: ${split.encoding}")
println("Parts: ${split.parts.size}")
```

---

### Joined Class

Represents the result of joining multiple QR code parts back into the original data.

```kotlin
import com.gorunjinian.bbqr.Joined
```

#### Creating a Joined (All Parts at Once)

```kotlin
Joined.fromParts(
    parts: List<String>     // All QR code content strings
): Joined
```

**Throws:** `JoinError` on failure

#### Properties

| Property   | Type        | Description                               |
|-----------|-------------|-------------------------------------------|
| `data`     | `ByteArray` | The decoded raw data bytes                |
| `encoding` | `Encoding`  | The encoding that was used                |
| `fileType` | `FileType`  | The file type (Psbt, Transaction, etc.)   |

#### Usage

```kotlin
val joined = Joined.fromParts(allParts)
val data = joined.data
val fileType = joined.fileType
```

---

### ContinuousJoiner Class

Stateful joiner for incrementally adding QR code parts as they are scanned. This is the recommended approach for real-time QR scanning.

```kotlin
import com.gorunjinian.bbqr.ContinuousJoiner
```

#### Constructor

```kotlin
ContinuousJoiner()  // No parameters needed
```

#### Methods

| Method      | Signature                              | Returns                 | Description                 |
|------------|----------------------------------------|-------------------------|-----------------------------|
| `addPart()` | `(part: String): ContinuousJoinResult` | `ContinuousJoinResult`  | Add a scanned QR code part  |

**Throws:** `HeaderParseError` or `JoinError` on invalid part data

#### Behavior

- Parts can be added in **any order**
- **Duplicate parts** (same index, same content) are silently accepted
- **Duplicate parts with different content** throw `JoinError.DuplicatePartWrongContent`
- Each call returns the current state: not started, in progress, or complete
- Once complete, subsequent calls return `Complete` with the same result

---

### SplitOptions Data Class

Configuration for how data is split into QR code parts.

```kotlin
import com.gorunjinian.bbqr.SplitOptions

data class SplitOptions(
    val encoding: Encoding = Encoding.Zlib,
    val minSplitNumber: Int = 1,
    val maxSplitNumber: Int = MAX_PARTS,  // 1295
    val minVersion: Version = Version.V01,
    val maxVersion: Version = Version.V40,
)
```

#### Fields

| Field            | Type       | Default          | Description                                       |
|------------------|------------|------------------|---------------------------------------------------|
| `encoding`       | `Encoding` | `Encoding.Zlib`  | Encoding method for the data                      |
| `minSplitNumber` | `Int`      | `1`              | Minimum number of QR parts (1 = allow single QR)  |
| `maxSplitNumber` | `Int`      | `1295`           | Maximum number of QR parts                        |
| `minVersion`     | `Version`  | `Version.V01`    | Smallest QR version to consider                   |
| `maxVersion`     | `Version`  | `Version.V40`    | Largest QR version to consider                    |

#### Usage

```kotlin
// Use defaults
val split = SplitResult.fromData(data, FileType.Psbt)

// Custom options
val options = SplitOptions(
    encoding = Encoding.Zlib,
    minSplitNumber = 1,
    maxSplitNumber = 100,
    minVersion = Version.V05,
    maxVersion = Version.V20,
)
val split = SplitResult.fromData(data, FileType.Psbt, options)
```

---

### Encoding Enum

Specifies how the data is encoded before being placed into QR codes.

```kotlin
import com.gorunjinian.bbqr.Encoding
```

```kotlin
enum class Encoding(val code: Char) {
    Hex('H'),       // Raw hexadecimal encoding (2 chars per byte)
    Base32('2'),    // Base32 encoding (denser than hex)
    Zlib('Z'),      // Zlib compression + Base32 encoding (recommended)
}
```

#### Encoding Comparison

| Encoding | Method                   | Data Density | Overhead    | Best For                        |
|----------|--------------------------|-------------|-------------|---------------------------------|
| `Zlib`   | Compress then Base32     | Highest     | Compression | Most data types (recommended)   |
| `Base32` | Direct Base32            | Medium      | ~1.6x       | Data that doesn't compress well |
| `Hex`    | Direct hex               | Lowest      | 2x          | Debugging, simple use cases     |

**Note:** When `Zlib` is selected but compression doesn't reduce size, the library automatically falls back to `Base32` encoding.

#### Alignment Requirements

Each encoding has an internal alignment requirement (`splitMod`) that ensures clean data boundaries across parts:
- `Hex`: alignment of 2 bytes
- `Base32` / `Zlib`: alignment of 8 bytes

---

### FileType Enum

Identifies the type of data being encoded. This is embedded in the BBQr header so the decoder knows how to interpret the data.

```kotlin
import com.gorunjinian.bbqr.FileType
```

```kotlin
enum class FileType(val code: Char) {
    Psbt('P'),          // Partially Signed Bitcoin Transaction
    Transaction('T'),   // Signed Bitcoin transaction (raw bytes)
    Json('J'),          // JSON data
    Cbor('C'),          // CBOR-encoded data
    UnicodeText('U'),   // Plain UTF-8 text
}
```

#### Header Markers

| FileType      | Header Char | Description                        |
|---------------|-------------|-------------------------------------|
| `Psbt`        | `P`         | Bitcoin PSBT (BIP-174)             |
| `Transaction` | `T`         | Raw Bitcoin transaction             |
| `Json`        | `J`         | JSON document                       |
| `Cbor`        | `C`         | CBOR-encoded data                   |
| `UnicodeText` | `U`         | UTF-8 text                          |

---

### Version Enum

QR code version (size). Version 1 is the smallest (21x21 modules) and Version 40 is the largest (177x177 modules).

```kotlin
import com.gorunjinian.bbqr.Version
```

```kotlin
enum class Version {
    V01, V02, V03, ..., V40
}
```

#### Properties

| Property       | Type  | Description                                                   |
|----------------|-------|---------------------------------------------------------------|
| `dataCapacity` | `Int` | Alphanumeric character capacity at Low error correction level |

#### Factory Methods

| Method                 | Description                                     |
|------------------------|-------------------------------------------------|
| `Version.fromNumber(n)` | Convert 1-40 to Version enum (throws on invalid) |

#### Version Selection Guidance

| Version Range   | Modules (size)    | Best For                            |
|-----------------|-------------------|--------------------------------------|
| V01-V05         | 21x21 - 37x37    | Very small data, tiny QR codes      |
| V05-V15         | 37x37 - 77x77    | Medium data, mobile-friendly        |
| V10-V25         | 57x57 - 117x117  | Larger data, still scannable        |
| V25-V40         | 117x117 - 177x177| Maximum capacity, harder to scan    |

**Recommendation:** For mobile scanning, constrain to `V05`-`V20` for best reliability. The algorithm will automatically choose the smallest version that fits your data.

---

### ContinuousJoinResult Sealed Class

Return type of `ContinuousJoiner.addPart()`, representing the current state of the join operation.

```kotlin
import com.gorunjinian.bbqr.ContinuousJoinResult
```

```kotlin
sealed class ContinuousJoinResult {
    data object NotStarted : ContinuousJoinResult()

    data class InProgress(
        val partsLeft: Int
    ) : ContinuousJoinResult()

    data class Complete(
        val joined: Joined
    ) : ContinuousJoinResult()
}
```

#### Usage Pattern

```kotlin
when (val result = joiner.addPart(scannedContent)) {
    is ContinuousJoinResult.NotStarted -> {
        // No valid BBQr header found, or empty data
    }
    is ContinuousJoinResult.InProgress -> {
        val remaining = result.partsLeft
        updateUI("$remaining parts remaining")
    }
    is ContinuousJoinResult.Complete -> {
        val data = result.joined.data
        val type = result.joined.fileType
        processData(data, type)
    }
}
```

---

### Error Types

All errors are sealed classes extending `Exception`, enabling exhaustive `when` matching.

#### SplitError

```kotlin
sealed class SplitError(message: String) : Exception(message) {
    class Empty : SplitError(...)
    // No data provided to encode

    class CannotFit : SplitError(...)
    // Data cannot fit within the specified version/encoding/part constraints

    class MaxSplitSizeTooLarge(val got: Int) : SplitError(...)
    // maxSplitNumber exceeds 1295

    class MinSplitTooSmall : SplitError(...)
    // minSplitNumber < 1

    class InvalidSplitRange : SplitError(...)
    // minSplitNumber > maxSplitNumber

    class InvalidVersionRange : SplitError(...)
    // minVersion > maxVersion

    class Encode(val error: EncodeError) : SplitError(...)
    // Encoding failed (e.g., zlib compression error)
}
```

#### JoinError

```kotlin
sealed class JoinError(message: String) : Exception(message) {
    class Empty : JoinError(...)
    class ConflictingHeaders : JoinError(...)
    class TooManyParts(val expected: Int, val got: Int) : JoinError(...)
    class DuplicatePartWrongContent(val index: Int) : JoinError(...)
    class PartWithNoData(val index: Int) : JoinError(...)
    class MissingPart(val index: Int) : JoinError(...)
    class HeaderParse(val error: HeaderParseError) : JoinError(...)
    class Decode(val error: DecodeError) : JoinError(...)
}
```

#### EncodeError

```kotlin
sealed class EncodeError(message: String) : Exception(message) {
    class Empty : EncodeError(...)
    class CompressionError(val error: String) : EncodeError(...)
}
```

#### DecodeError

```kotlin
sealed class DecodeError(message: String) : Exception(message) {
    class UnableToDecodeHex(val partIndex: Int, val error: String) : DecodeError(...)
    class UnableToDecodeBase32(val partIndex: Int, val error: String) : DecodeError(...)
    class UnableToInflateZlib(val error: String) : DecodeError(...)
}
```

#### HeaderParseError

```kotlin
sealed class HeaderParseError(message: String) : Exception(message) {
    class Empty : HeaderParseError(...)
    class InvalidEncoding(val encoding: Char) : HeaderParseError(...)
    class InvalidFileType(val fileType: Char) : HeaderParseError(...)
    class InvalidFixedHeader : HeaderParseError(...)
    class InvalidHeaderSize(val size: Int) : HeaderParseError(...)
    class InvalidHeaderParts(val detail: String) : HeaderParseError(...)
}
```

---

## PSBT Encoding Guide

### Basic PSBT Encoding

BBQr works directly with raw bytes — no CBOR encoding is needed (unlike BC-UR).

```kotlin
import com.gorunjinian.bbqr.*

fun encodePsbt(psbtBytes: ByteArray): List<String> {
    return SplitResult.fromData(psbtBytes, FileType.Psbt).parts
}
```

### Encoding with Custom Options

```kotlin
fun encodePsbtWithOptions(psbtBytes: ByteArray): List<String> {
    val options = SplitOptions(
        encoding = Encoding.Zlib,
        minSplitNumber = 1,
        maxSplitNumber = 50,
        minVersion = Version.V05,
        maxVersion = Version.V20,
    )
    return SplitResult.fromData(psbtBytes, FileType.Psbt, options).parts
}
```

### Displaying QR Codes

**Single QR code (1 part):**

```kotlin
val parts = encodePsbt(psbtBytes)
if (parts.size == 1) {
    displayQrCode(parts[0])
}
```

**Multiple QR codes (animated):**

```kotlin
val parts = encodePsbt(psbtBytes)
if (parts.size > 1) {
    startQrAnimation(parts)
}
```

### Android Animation Implementation

```kotlin
class QrAnimationViewModel : ViewModel() {
    private var parts: List<String> = emptyList()
    private var currentIndex = 0
    private var animationJob: Job? = null

    val currentQrContent = MutableLiveData<String>()
    val progressText = MutableLiveData<String>()

    fun startAnimation(psbtBytes: ByteArray) {
        val split = SplitResult.fromData(
            psbtBytes,
            FileType.Psbt,
            SplitOptions(
                minSplitNumber = 1,
                maxSplitNumber = 100,
                minVersion = Version.V05,
                maxVersion = Version.V20,
            ),
        )

        parts = split.parts

        if (parts.size == 1) {
            currentQrContent.value = parts[0]
            progressText.value = "Single QR code"
            return
        }

        currentIndex = 0
        animationJob = viewModelScope.launch {
            while (isActive) {
                currentQrContent.postValue(parts[currentIndex])
                progressText.postValue("Part ${currentIndex + 1}/${parts.size}")
                currentIndex = (currentIndex + 1) % parts.size
                delay(250) // 4 fps
            }
        }
    }

    fun stopAnimation() {
        animationJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopAnimation()
    }
}
```

---

## PSBT Decoding Guide

### Approach 1: Streaming Decode with ContinuousJoiner (Recommended)

```kotlin
import com.gorunjinian.bbqr.*

class QrScannerViewModel : ViewModel() {
    private val joiner = ContinuousJoiner()

    val scanProgress = MutableLiveData<String>()
    val decodedPsbt = MutableLiveData<ByteArray>()
    val errorMessage = MutableLiveData<String>()

    fun onQrCodeScanned(content: String) {
        try {
            when (val result = joiner.addPart(content)) {
                is ContinuousJoinResult.NotStarted -> {
                    scanProgress.value = "Scan a BBQr QR code..."
                }
                is ContinuousJoinResult.InProgress -> {
                    scanProgress.value = "${result.partsLeft} parts remaining"
                }
                is ContinuousJoinResult.Complete -> {
                    val joined = result.joined
                    if (joined.fileType == FileType.Psbt) {
                        decodedPsbt.value = joined.data
                    } else {
                        errorMessage.value = "Expected PSBT, got: ${joined.fileType}"
                    }
                }
            }
        } catch (e: JoinError) {
            errorMessage.value = "Scan error: ${e.message}"
        }
    }
}
```

### Approach 2: Batch Decode with Joined (All Parts Available)

```kotlin
import com.gorunjinian.bbqr.*

fun decodePsbtFromParts(parts: List<String>): ByteArray {
    val joined = Joined.fromParts(parts)
    require(joined.fileType == FileType.Psbt) { "Expected PSBT, got: ${joined.fileType}" }
    return joined.data
}
```

---

## Multi-Part QR Codes

### How Splitting Works

1. The algorithm receives raw data bytes, file type, and split options
2. It encodes the data (hex, base32, or zlib+base32)
3. It iterates QR versions to find the optimal version + part count combination
4. Each part gets a header identifying the encoding, file type, total part count, and part index
5. The algorithm prefers fewer, larger QR codes over many small ones

### Part Numbering

- Parts are **0-indexed** in the header (part `00` is the first part)
- Part index is encoded in **base-36** using 2 characters (`00` through `ZZ`)
- Maximum parts: 1295 (36^2 - 1)

### Capacity Calculation

Each QR part's data capacity is:
```
usable_capacity = qr_version_capacity - 8_byte_header
adjusted_capacity = usable_capacity - (usable_capacity % encoding_alignment)
```

Where encoding alignment is:
- Hex: 2
- Base32/Zlib: 8

---

## BBQr Header Format

Every BBQr QR code part starts with an 8-byte ASCII header:

```
B $ <ENC> <TYPE> <TOTAL_PARTS> <PART_INDEX>
|  |   |     |        |              |
|  |   |     |        |              +-- 2 chars: base-36 part index (00-ZZ)
|  |   |     |        +---------------- 2 chars: base-36 total parts (01-ZZ)
|  |   |     +------------------------- 1 char: file type marker
|  |   +-------------------------------- 1 char: encoding marker
|  +------------------------------------ Fixed: $ (dollar sign)
+--------------------------------------- Fixed: B (BBQr identifier)
```

### Header Examples

| Header     | Meaning                                          |
|------------|--------------------------------------------------|
| `B$ZP0100` | Zlib encoding, PSBT, 1 total part, part 0        |
| `B$ZP0300` | Zlib encoding, PSBT, 3 total parts, part 0       |
| `B$ZP0302` | Zlib encoding, PSBT, 3 total parts, part 2       |
| `B$HT0100` | Hex encoding, Transaction, 1 part, part 0        |
| `B$2J0200` | Base32 encoding, JSON, 2 parts, part 0           |

### Encoding Markers

| Char | Encoding |
|------|----------|
| `H`  | Hex      |
| `2`  | Base32   |
| `Z`  | Zlib     |

### File Type Markers

| Char | FileType      |
|------|---------------|
| `P`  | Psbt          |
| `T`  | Transaction   |
| `J`  | Json          |
| `C`  | Cbor          |
| `U`  | UnicodeText   |

### Detecting BBQr QR Codes

```kotlin
fun isBbqrCode(qrContent: String): Boolean {
    return qrContent.length >= 8 && qrContent.startsWith("B$")
}
```

---

## Error Handling

### Encoding Errors

```kotlin
fun safeSplit(psbtBytes: ByteArray): List<String>? {
    return try {
        SplitResult.fromData(psbtBytes, FileType.Psbt, SplitOptions(
            maxSplitNumber = 50,
            minVersion = Version.V05,
            maxVersion = Version.V20,
        )).parts
    } catch (e: SplitError.Empty) {
        null
    } catch (e: SplitError.CannotFit) {
        // Retry with default (relaxed) constraints
        try {
            SplitResult.fromData(psbtBytes, FileType.Psbt).parts
        } catch (_: SplitError) { null }
    } catch (e: SplitError) {
        null
    }
}
```

### Decoding Errors

```kotlin
fun safeAddPart(joiner: ContinuousJoiner, content: String): ScanState {
    return try {
        when (val result = joiner.addPart(content)) {
            is ContinuousJoinResult.NotStarted -> ScanState.Waiting
            is ContinuousJoinResult.InProgress -> ScanState.Progress(result.partsLeft)
            is ContinuousJoinResult.Complete -> {
                ScanState.Done(result.joined.data, result.joined.fileType)
            }
        }
    } catch (e: JoinError.ConflictingHeaders) {
        ScanState.Error("Mixed QR codes from different payloads detected")
    } catch (e: JoinError.DuplicatePartWrongContent) {
        ScanState.Error("Corrupted part at index ${e.index}")
    } catch (e: JoinError) {
        ScanState.Error("Scan error: ${e.message}")
    }
}

sealed class ScanState {
    data object Waiting : ScanState()
    data class Progress(val partsLeft: Int) : ScanState()
    data class Done(val data: ByteArray, val fileType: FileType) : ScanState()
    data class Error(val message: String) : ScanState()
}
```

---

## Complete Examples

### Example 1: Full Round-Trip

```kotlin
import com.gorunjinian.bbqr.*

fun roundTripTest() {
    val originalData = ByteArray(2000) { it.toByte() }

    // Encode
    val split = SplitResult.fromData(originalData, FileType.Psbt)
    println("Version: ${split.version}")
    println("Encoding: ${split.encoding}")
    println("Parts: ${split.parts.size}")

    // Decode
    val joined = Joined.fromParts(split.parts)
    println("File type: ${joined.fileType}")

    // Verify
    assert(joined.data.contentEquals(originalData))
    println("Round-trip successful!")
}
```

### Example 2: Encoding Multiple Data Types

```kotlin
import com.gorunjinian.bbqr.*

fun encodePsbt(psbtBytes: ByteArray) =
    SplitResult.fromData(psbtBytes, FileType.Psbt).parts

fun encodeTransaction(txBytes: ByteArray) =
    SplitResult.fromData(txBytes, FileType.Transaction).parts

fun encodeJson(json: String) =
    SplitResult.fromData(json.toByteArray(), FileType.Json).parts

fun encodeText(text: String) =
    SplitResult.fromData(text.toByteArray(), FileType.UnicodeText).parts
```

### Example 3: Adaptive Split Options

```kotlin
import com.gorunjinian.bbqr.*

fun adaptiveSplitOptions(dataSize: Int, isCloseRange: Boolean): SplitOptions = when {
    dataSize < 500 -> SplitOptions(
        maxSplitNumber = 1,
        maxVersion = Version.V20,
    )
    dataSize < 5000 -> SplitOptions(
        maxSplitNumber = 10,
        minVersion = if (isCloseRange) Version.V10 else Version.V05,
        maxVersion = if (isCloseRange) Version.V25 else Version.V15,
    )
    else -> SplitOptions(
        maxSplitNumber = 50,
        minVersion = Version.V10,
        maxVersion = Version.V25,
    )
}
```

---

## Comparison: BBQr vs BC-UR

| Feature                  | BBQr                                | BC-UR                                 |
|--------------------------|-------------------------------------|---------------------------------------|
| **Encoding**             | Zlib + Base32/Hex                   | CBOR + Bytewords                      |
| **PSBT input**           | Raw PSBT bytes directly             | CBOR-wrapped PSBT bytes required      |
| **Multi-part strategy**  | Sequential parts (must collect all) | Fountain codes (any sufficient subset) |
| **Redundancy**           | None (all parts required)           | Built-in (generates unlimited repair parts) |
| **Part ordering**        | Parts can be scanned in any order   | Parts can be scanned in any order     |
| **Compression**          | Zlib (built-in)                     | None (CBOR only)                      |
| **File type awareness**  | Yes (Psbt, Transaction, Json, etc.) | Yes (via UR type string)              |
| **Scanning UX**          | Show progress (X of Y parts)        | Continue until decoded                |
| **Max parts**            | 1295                                | Unlimited (fountain codes)            |
| **Native Kotlin**        | Yes (pure Kotlin/JVM)               | Varies by implementation              |

### When to Use Which

- **BBQr**: Simple, deterministic multi-part encoding with built-in compression and clear progress indication. Good for PSBT exchange between devices where you can ensure all parts are scanned.
- **BC-UR**: When you need fountain code redundancy (unreliable scanning) or broad hardware wallet compatibility.

---

## References

- [BBQr Specification](https://github.com/coinkite/BBQr/blob/master/BBQr.md)
- [BBQr Rust Implementation](https://github.com/nicbus/bbqr-rust)
- [BIP-174: Partially Signed Bitcoin Transactions](https://github.com/bitcoin/bips/blob/master/bip-0174.mediawiki)
