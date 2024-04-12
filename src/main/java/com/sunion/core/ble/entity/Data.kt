package com.sunion.core.ble.entity


sealed class Data {
    data class NinetyNine(
        val target: Int,
        val sha256: String,
    ) : Data()

    data class NinetyB(
        val type: Int,
        val time: Int,
        val index: Int,
    ) : Data()

    data class NinetyC(
        val isSuccess: Boolean,
        val hasUnsyncedData: Int,
    ) : Data()
}