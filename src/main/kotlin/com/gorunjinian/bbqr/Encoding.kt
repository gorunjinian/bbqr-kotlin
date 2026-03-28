package com.gorunjinian.bbqr

enum class Encoding(val code: Char) {
    Hex('H'),
    Base32('2'),
    Zlib('Z');

    val splitMod: Int
        get() = when (this) {
            Hex -> 2
            Base32, Zlib -> 8
        }

    companion object {
        fun fromCode(code: Char): Encoding? = entries.find { it.code == code }
    }
}
