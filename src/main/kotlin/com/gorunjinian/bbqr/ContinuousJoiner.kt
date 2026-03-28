package com.gorunjinian.bbqr

sealed class ContinuousJoinResult {
    data object NotStarted : ContinuousJoinResult()
    data class InProgress(val partsLeft: Int) : ContinuousJoinResult()
    data class Complete(val joined: Joined) : ContinuousJoinResult()
}

class ContinuousJoiner {
    private sealed class State {
        data object Initial : State()
        data class InProgress(
            val header: Header,
            val data: Array<String>,
            var partsLeft: Int,
        ) : State() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as InProgress

                if (partsLeft != other.partsLeft) return false
                if (header != other.header) return false
                if (!data.contentEquals(other.data)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = partsLeft
                result = 31 * result + header.hashCode()
                result = 31 * result + data.contentHashCode()
                return result
            }
        }

        data class Complete(val joined: Joined) : State()
    }

    private var state: State = State.Initial

    fun addPart(part: String): ContinuousJoinResult {
        if (part.isEmpty()) {
            return currentResult()
        }

        return when (val s = state) {
            is State.Initial -> handleInitial(part)
            is State.InProgress -> handleInProgress(s, part)
            is State.Complete -> ContinuousJoinResult.Complete(s.joined)
        }
    }

    private fun currentResult(): ContinuousJoinResult = when (val s = state) {
        is State.Initial -> ContinuousJoinResult.NotStarted
        is State.InProgress -> ContinuousJoinResult.InProgress(s.partsLeft)
        is State.Complete -> ContinuousJoinResult.Complete(s.joined)
    }

    private fun handleInitial(part: String): ContinuousJoinResult {
        val header = try {
            Header.parse(part)
        } catch (e: HeaderParseError) {
            throw e
        }

        val parts = Array(header.numParts) { "" }
        val index = getIndexFromPart(part, header)
        val partData = part.substring(HEADER_LENGTH)
        parts[index] = partData

        val partsLeft = header.numParts - 1

        if (partsLeft == 0) {
            val data = try {
                Decode.decodeOrderedParts(parts.toList(), header.encoding)
            } catch (e: DecodeError) {
                throw e
            }
            val joined = Joined(header.encoding, header.fileType, data)
            state = State.Complete(joined)
            return ContinuousJoinResult.Complete(joined)
        }

        state = State.InProgress(header, parts, partsLeft)
        return ContinuousJoinResult.InProgress(partsLeft)
    }

    private fun handleInProgress(s: State.InProgress, part: String): ContinuousJoinResult {
        val partHeader = try {
            Header.parse(part)
        } catch (e: HeaderParseError) {
            throw e
        }

        if (partHeader != s.header) {
            throw HeaderParseError.InvalidHeaderParts("Header parts do not match")
        }

        val index = getIndexFromPart(part, partHeader)
        val currentData = s.data[index]
        val partData = part.substring(HEADER_LENGTH)

        if (currentData.isEmpty()) {
            s.partsLeft--
        }

        if (currentData.isNotEmpty() && currentData != partData) {
            throw JoinError.DuplicatePartWrongContent(index)
        }

        if (currentData.isEmpty()) {
            s.data[index] = partData
        }

        if (s.partsLeft == 0) {
            val data = try {
                Decode.decodeOrderedParts(s.data.toList(), partHeader.encoding)
            } catch (e: DecodeError) {
                throw e
            }
            val joined = Joined(partHeader.encoding, partHeader.fileType, data)
            state = State.Complete(joined)
            return ContinuousJoinResult.Complete(joined)
        }

        return ContinuousJoinResult.InProgress(s.partsLeft)
    }
}
