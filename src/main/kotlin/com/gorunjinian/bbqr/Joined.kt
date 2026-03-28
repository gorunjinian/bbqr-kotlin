package com.gorunjinian.bbqr

data class Joined(
    val encoding: Encoding,
    val fileType: FileType,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Joined) return false
        return encoding == other.encoding && fileType == other.fileType && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = encoding.hashCode()
        result = 31 * result + fileType.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }

    companion object {
        fun fromParts(parts: List<String>): Joined {
            val (header, decoded) = joinQrs(parts)
            return Joined(
                encoding = header.encoding,
                fileType = header.fileType,
                data = decoded,
            )
        }
    }
}

private fun joinQrs(inputParts: List<String>): Pair<Header, ByteArray> {
    val header = getAndVerifyHeaders(inputParts)

    val orderedParts = Array(header.numParts) { "" }

    for (part in inputParts) {
        if (part.isEmpty()) continue

        val index = getIndexFromPart(part, header)
        val currentContent = orderedParts[index]
        val partData = part.substring(HEADER_LENGTH)

        if (currentContent.isNotEmpty() && currentContent != partData) {
            throw JoinError.DuplicatePartWrongContent(index)
        }

        if (partData.isEmpty()) {
            throw JoinError.PartWithNoData(index)
        }

        orderedParts[index] = partData
    }

    for ((index, part) in orderedParts.withIndex()) {
        if (part.isEmpty()) {
            throw JoinError.MissingPart(index)
        }
    }

    val data = try {
        Decode.decodeOrderedParts(orderedParts.toList(), header.encoding)
    } catch (e: DecodeError) {
        throw JoinError.Decode(e)
    }

    return header to data
}

private fun getAndVerifyHeaders(parts: List<String>): Header {
    if (parts.isEmpty()) throw JoinError.Empty()

    val firstNonEmpty = parts.firstOrNull { it.isNotEmpty() }
        ?: throw JoinError.Empty()

    val header = try {
        Header.parse(firstNonEmpty)
    } catch (e: HeaderParseError) {
        throw JoinError.HeaderParse(e)
    }

    for (part in parts.drop(1)) {
        if (part.trim().isEmpty()) continue
        if (part.length < HEADER_LENGTH) throw JoinError.ConflictingHeaders()
        if (part.substring(0, 6) != firstNonEmpty.substring(0, 6)) {
            throw JoinError.ConflictingHeaders()
        }
    }

    return header
}
