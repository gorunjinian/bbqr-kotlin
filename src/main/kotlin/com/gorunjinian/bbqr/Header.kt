package com.gorunjinian.bbqr

data class Header(
    val encoding: Encoding,
    val fileType: FileType,
    val numParts: Int,
) {
    override fun toString(): String {
        return "B$${encoding.code}${fileType.code}${intToBase36(numParts)}"
    }

    companion object {
        fun parse(str: String): Header {
            if (str.isEmpty()) throw HeaderParseError.Empty()
            if (str.length < HEADER_LENGTH) throw HeaderParseError.InvalidHeaderSize(str.length)
            if (str[0] != 'B' || str[1] != '$') throw HeaderParseError.InvalidFixedHeader()

            val encoding = Encoding.fromCode(str[2])
                ?: throw HeaderParseError.InvalidEncoding(str[2])

            val fileType = FileType.fromCode(str[3])
                ?: throw HeaderParseError.InvalidFileType(str[3])

            val numPartsStr = str.substring(4, 6)
            val numParts = try {
                numPartsStr.toInt(36)
            } catch (_: NumberFormatException) {
                throw HeaderParseError.InvalidHeaderParts("Invalid number of parts: $numPartsStr")
            }

            return Header(encoding, fileType, numParts)
        }
    }
}

internal fun intToBase36(num: Int): String =
    num.toString(36).uppercase().padStart(2, '0')

internal fun getIndexFromPart(part: String, header: Header): Int {
    val index = part.substring(6, 8).toInt(36)
    if (index >= header.numParts) {
        throw JoinError.TooManyParts(header.numParts, index + 1)
    }
    return index
}
