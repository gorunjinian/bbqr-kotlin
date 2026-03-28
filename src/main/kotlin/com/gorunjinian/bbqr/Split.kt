package com.gorunjinian.bbqr

data class SplitOptions(
    val encoding: Encoding = Encoding.Zlib,
    val minSplitNumber: Int = 1,
    val maxSplitNumber: Int = MAX_PARTS,
    val minVersion: Version = Version.V01,
    val maxVersion: Version = Version.V40,
)

data class SplitResult(
    val version: Version,
    val parts: List<String>,
    val encoding: Encoding,
) {
    companion object {
        fun fromData(
            bytes: ByteArray,
            fileType: FileType,
            options: SplitOptions = SplitOptions(),
        ): SplitResult {
            if (bytes.isEmpty()) throw SplitError.Empty()
            validateOptions(options)

            val encoded = try {
                Encoded.fromData(bytes, options.encoding)
            } catch (e: EncodeError) {
                throw SplitError.Encode(e)
            }

            val best = findBestVersion(encoded, options)
            val headerString = Header(encoded.encoding, fileType, best.count).toString()
            val encodedData = encoded.data

            val parts = (0 until best.count).map { i ->
                val startByte = i * best.dataPerQr
                val endByte = minOf(startByte + best.dataPerQr, encodedData.length)
                val partIndex = intToBase36(i)
                val dataPart = encodedData.substring(startByte, endByte)
                "$headerString$partIndex$dataPart"
            }

            return SplitResult(
                version = best.version,
                parts = parts,
                encoding = encoded.encoding,
            )
        }
    }
}

private fun validateOptions(options: SplitOptions) {
    if (options.maxSplitNumber > MAX_PARTS) {
        throw SplitError.MaxSplitSizeTooLarge(options.maxSplitNumber)
    }
    if (options.minSplitNumber < 1) {
        throw SplitError.MinSplitTooSmall()
    }
    if (options.minSplitNumber > options.maxSplitNumber) {
        throw SplitError.InvalidSplitRange()
    }
    if (options.minVersion > options.maxVersion) {
        throw SplitError.InvalidVersionRange()
    }
}

private fun findBestVersion(encoded: Encoded, options: SplitOptions): QrsNeeded {
    var bestOption: QrsNeeded? = null

    for (versionIndex in options.minVersion.ordinal..options.maxVersion.ordinal) {
        val version = Version.entries[versionIndex]
        val qrsNeeded = encoded.numberOfQrsNeeded(version)

        if (qrsNeeded.count > MAX_PARTS) continue
        if (qrsNeeded.count < options.minSplitNumber || qrsNeeded.count > options.maxSplitNumber) continue

        val current = bestOption
        if (current == null || qrsNeeded < current) {
            bestOption = qrsNeeded
        }
    }

    val best = bestOption ?: throw SplitError.CannotFit()

    if (best.dataPerQr * best.count < encoded.data.length) {
        throw SplitError.CannotFit()
    }

    return best
}
