package com.sunion.core.ble.entity

sealed class Data {
    data class NinetyNine(
        val target: Int,
        val sha256: String,
    ) : Data()

}