package com.sunion.core.ble.entity

sealed class SunionBleNotification {
    object UNKNOWN : SunionBleNotification()
}

sealed class Alert : SunionBleNotification() {
    object UNKNOWN : Alert()
    data class AF(
        val alertType: Int,
    ) : Alert()
}