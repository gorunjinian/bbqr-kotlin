package com.gorunjinian.bbqr

sealed class SplitError(message: String) : Exception(message) {
    class Empty : SplitError("No data found")
    class CannotFit : SplitError("Cannot make the data fit")
    class MaxSplitSizeTooLarge(val got: Int) : SplitError("Max split size is too large, max is $MAX_PARTS, got $got")
    class MinSplitTooSmall : SplitError("Min split size is too small, must at least be 1")
    class InvalidSplitRange : SplitError("Invalid split min and max range, min is larger than max")
    class InvalidVersionRange : SplitError("Invalid version min and max range, min is larger than max")
    class Encode(val error: EncodeError) : SplitError(error.message ?: "Encode error")
}

sealed class JoinError(message: String) : Exception(message) {
    class Empty : JoinError("No data found")
    class ConflictingHeaders : JoinError("Conflicting/variable file type/encodings/sizes")
    class TooManyParts(val expected: Int, val got: Int) : JoinError("Too many parts, expected $expected, got $got")
    class DuplicatePartWrongContent(val index: Int) : JoinError("Duplicated part index $index has wrong content")
    class PartWithNoData(val index: Int) : JoinError("Part with index $index has no data")
    class MissingPart(val index: Int) : JoinError("Missing part, with index $index")
    class HeaderParse(val error: HeaderParseError) : JoinError(error.message ?: "Header parse error")
    class Decode(val error: DecodeError) : JoinError(error.message ?: "Decode error")
}

sealed class EncodeError(message: String) : Exception(message) {
    class Empty : EncodeError("No data to encode")
    class CompressionError(val error: String) : EncodeError("Unable to compress data: $error")
}

sealed class DecodeError(message: String) : Exception(message) {
    class UnableToDecodeHex(val partIndex: Int, val error: String) : DecodeError("Unable to decode hex part: $partIndex, error: $error")
    class UnableToDecodeBase32(val partIndex: Int, val error: String) : DecodeError("Unable to decode base32 part: $partIndex, error: $error")
    class UnableToInflateZlib(val error: String) : DecodeError("Unable to decompress zlib data: $error")
}

sealed class HeaderParseError(message: String) : Exception(message) {
    class Empty : HeaderParseError("No data found")
    class InvalidEncoding(val encoding: Char) : HeaderParseError("Invalid encoding $encoding")
    class InvalidFileType(val fileType: Char) : HeaderParseError("Invalid FileType $fileType")
    class InvalidFixedHeader : HeaderParseError("Invalid fixed header")
    class InvalidHeaderSize(val size: Int) : HeaderParseError("Invalid header size, expected $HEADER_LENGTH bytes, got $size")
    class InvalidHeaderParts(val detail: String) : HeaderParseError("Invalid header parts $detail")
}
