package com.gorunjinian.bbqr

enum class Version {
    V01, V02, V03, V04, V05, V06, V07, V08, V09, V10,
    V11, V12, V13, V14, V15, V16, V17, V18, V19, V20,
    V21, V22, V23, V24, V25, V26, V27, V28, V29, V30,
    V31, V32, V33, V34, V35, V36, V37, V38, V39, V40;

    /** Alphanumeric capacity at Low error correction level. */
    val dataCapacity: Int
        get() = QR_DATA_CAPACITY[ordinal][0][2]

    companion object {
        fun fromNumber(n: Int): Version {
            require(n in 1..40) { "Version number must be between 1 and 40, got $n" }
            return entries[n - 1]
        }
    }
}
