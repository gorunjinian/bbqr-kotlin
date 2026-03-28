package com.gorunjinian.bbqr

enum class FileType(val code: Char) {
    Psbt('P'),
    Transaction('T'),
    Json('J'),
    Cbor('C'),
    UnicodeText('U');

    companion object {
        fun fromCode(code: Char): FileType? = entries.find { it.code == code }
    }
}
