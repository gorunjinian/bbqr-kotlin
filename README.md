# BBQr Kotlin

Pure Kotlin implementation of the [BBQr protocol](https://github.com/coinkite/BBQr) for splitting and joining binary data across multiple QR codes, optimized for Bitcoin-related payloads like PSBTs and transactions.

## Features

- Pure Kotlin/JVM — no native dependencies, no JNI/JNA
- Cross-compatible with Rust and Python BBQr implementations
- Zlib compression with automatic fallback to Base32 when compression doesn't help
- Streaming/incremental QR scanning via `ContinuousJoiner`
- Automatic QR version selection for optimal fit
- Supports PSBT, Transaction, JSON, CBOR, and Unicode text file types
- Up to 1295 QR code parts per payload

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.gorunjinian:bbqr:1.0.3")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.gorunjinian:bbqr:1.0.3'
}
```

## Quick Start

### Split data into QR code parts

```kotlin
import com.gorunjinian.bbqr.*

val psbtBytes: ByteArray = // ... your PSBT data

val split = SplitResult.fromData(psbtBytes, FileType.Psbt)

val qrParts: List<String> = split.parts
// Each string in qrParts is the content for one QR code
```

### Join QR code parts back into data

```kotlin
import com.gorunjinian.bbqr.*

val joined = Joined.fromParts(qrParts)
val originalData: ByteArray = joined.data
val fileType: FileType = joined.fileType
```

### Streaming decode (scan QR codes one by one)

```kotlin
import com.gorunjinian.bbqr.*

val joiner = ContinuousJoiner()

fun onQrCodeScanned(qrContent: String) {
    when (val result = joiner.addPart(qrContent)) {
        is ContinuousJoinResult.NotStarted -> { /* no valid data yet */ }
        is ContinuousJoinResult.InProgress -> {
            println("${result.partsLeft} parts remaining")
        }
        is ContinuousJoinResult.Complete -> {
            val data = result.joined.data
            val fileType = result.joined.fileType
            // handle complete data
        }
    }
}
```

### Custom split options

```kotlin
val split = SplitResult.fromData(
    psbtBytes,
    FileType.Psbt,
    SplitOptions(
        encoding = Encoding.Zlib,
        minSplitNumber = 1,
        maxSplitNumber = 50,
        minVersion = Version.V05,
        maxVersion = Version.V20,
    ),
)
```

## Building

```bash
./gradlew build
```

## Testing

```bash
./gradlew test
```

## Documentation

See [BBQR_IMPLEMENTATION.md](BBQR_IMPLEMENTATION.md) for the full API reference, protocol details, and usage examples.

## Protocol Specification

See the [BBQr specification](https://github.com/coinkite/BBQr/blob/master/BBQr.md) for the protocol details.

## License

MIT
